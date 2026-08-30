package com.errata.app.sync

import android.util.Log
import com.errata.app.domain.sync.CloudDocument
import com.errata.app.domain.sync.CloudSaveResult
import com.errata.app.domain.sync.CloudStore
import com.errata.app.domain.sync.SyncCodec
import com.errata.app.domain.sync.SyncSnapshot
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class DriveAppDataClient(
    private val tokenProvider: suspend () -> String?,
    private val fileIdStore: (String?) -> Unit,
    private val currentFileId: () -> String?,
    private val onUnauthorized: () -> Unit = {},
) : CloudStore {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun load(): CloudDocument = withContext(Dispatchers.IO) {
        val existingId = currentFileId() ?: findFileId()
        if (existingId == null) {
            return@withContext CloudDocument(snapshot = null, fileId = null, etag = null)
        }
        fileIdStore(existingId)
        val url = DRIVE_API_ROOT.toHttpUrl().newBuilder()
            .addPathSegment("files")
            .addPathSegment(existingId)
            .addQueryParameter("alt", "media")
            .build()
        execute { token ->
            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .get()
                .build()
        }.use { response ->
            when (response.code) {
                401, 403 -> {
                    logDriveFailure("download", response)
                    throw AuthRequiredException()
                }
                404 -> {
                    fileIdStore(null)
                    val recovered = findFileId()
                    if (recovered == null) {
                        CloudDocument(null, null, null)
                    } else {
                        fileIdStore(recovered)
                        downloadMedia(recovered)
                    }
                }
                in 200..299 -> documentFromMedia(existingId, response)
                else -> {
                    logDriveFailure("download", response)
                    throw NetworkException()
                }
            }
        }
    }

    override suspend fun save(
        snapshot: SyncSnapshot,
        fileId: String?,
        etag: String?,
    ): CloudSaveResult = withContext(Dispatchers.IO) {
        val bodyJson = SyncCodec.encode(snapshot)
        if (fileId.isNullOrBlank()) {
            recoverMissing(bodyJson)
        } else {
            val match = etag?.takeIf { it.isNotBlank() } ?: fetchEtag(fileId)
            if (match.isNullOrBlank()) {
                recoverMissing(bodyJson)
            } else {
                updateFile(fileId, match, bodyJson, recovering = false)
            }
        }
    }

    override suspend fun delete(): Boolean = withContext(Dispatchers.IO) {
        val refs = try {
            listSyncFiles()
        } catch (_: AuthRequiredException) {
            return@withContext false
        } catch (_: NetworkException) {
            return@withContext false
        }
        val deleted = mutableSetOf<String>()
        for (ref in refs) {
            if (deleteFileById(ref.id)) deleted += ref.id
        }
        if (!DriveSyncFiles.wipeComplete(refs, deleted)) {
            return@withContext false
        }
        fileIdStore(null)
        true
    }

    private suspend fun downloadMedia(fileId: String): CloudDocument {
        val url = DRIVE_API_ROOT.toHttpUrl().newBuilder()
            .addPathSegment("files")
            .addPathSegment(fileId)
            .addQueryParameter("alt", "media")
            .build()
        return execute { token ->
            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .get()
                .build()
        }.use { response ->
            when (response.code) {
                401, 403 -> {
                    logDriveFailure("download", response)
                    throw AuthRequiredException()
                }
                404 -> CloudDocument(null, null, null)
                in 200..299 -> documentFromMedia(fileId, response)
                else -> {
                    logDriveFailure("download", response)
                    throw NetworkException()
                }
            }
        }
    }

    private fun documentFromMedia(fileId: String, response: Response): CloudDocument {
        val body = response.body?.string().orEmpty()
        val etag = response.header("ETag") ?: response.header("etag")
        if (DriveSyncFiles.mediaUnreadable(body)) {
            return CloudDocument(
                snapshot = null,
                fileId = fileId,
                etag = etag,
                unreadable = true,
            )
        }
        return try {
            CloudDocument(SyncCodec.decode(body), fileId, etag)
        } catch (_: Exception) {
            CloudDocument(
                snapshot = null,
                fileId = fileId,
                etag = etag,
                unreadable = true,
            )
        }
    }

    private suspend fun findFileId(): String? {
        val refs = listSyncFiles()
        val canonical = DriveSyncFiles.pickCanonical(refs) ?: return null
        DriveSyncFiles.orphanIds(refs, canonical.id).forEach { orphan ->
            deleteFileById(orphan)
        }
        return canonical.id
    }

    private suspend fun listSyncFiles(): List<DriveSyncFiles.FileRef> {
        val acc = mutableListOf<DriveSyncFiles.FileRef>()
        var pageToken: String? = null
        var pages = 0
        do {
            val page = listSyncFilesPage(pageToken)
            acc += page.files.mapNotNull { meta ->
                val id = meta.id ?: return@mapNotNull null
                DriveSyncFiles.FileRef(id, meta.modifiedTime.orEmpty())
            }
            pageToken = page.nextPageToken?.takeIf { it.isNotBlank() }
            pages += 1
        } while (pageToken != null && pages < MAX_LIST_PAGES)
        return acc
    }

    private suspend fun listSyncFilesPage(pageToken: String?): DriveListResponse {
        val url = DRIVE_API_ROOT.toHttpUrl().newBuilder()
            .addPathSegment("files")
            .addQueryParameter("spaces", "appDataFolder")
            .addQueryParameter("q", QUERY)
            .addQueryParameter("fields", "nextPageToken,files(id,modifiedTime)")
            .addQueryParameter("pageSize", "100")
            .apply {
                if (!pageToken.isNullOrBlank()) {
                    addQueryParameter("pageToken", pageToken)
                }
            }
            .build()
        execute { token ->
            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .get()
                .build()
        }.use { response ->
            if (response.code == 401 || response.code == 403) {
                logDriveFailure("list", response)
                throw AuthRequiredException()
            }
            if (!response.isSuccessful) {
                logDriveFailure("list", response)
                throw NetworkException()
            }
            return json.decodeFromString<DriveListResponse>(
                response.body?.string().orEmpty().ifBlank { "{}" },
            )
        }
    }

    private suspend fun createFile(bodyJson: String): CloudSaveResult {
        val boundary = "errata_sync"
        val metadata = """{"name":"${DriveSyncFiles.FILE_NAME}","parents":["appDataFolder"]}"""
        val payload = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata)
            append("\r\n--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(bodyJson)
            append("\r\n--$boundary--\r\n")
        }
        val url = DRIVE_UPLOAD_ROOT.toHttpUrl().newBuilder()
            .addPathSegment("files")
            .addQueryParameter("uploadType", "multipart")
            .addQueryParameter("fields", "id")
            .build()
        execute { token ->
            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .post(
                    payload.toRequestBody("multipart/related; boundary=$boundary".toMediaType()),
                )
                .build()
        }.use { response ->
            return when (response.code) {
                401, 403 -> {
                    logDriveFailure("create", response)
                    CloudSaveResult.Failed("auth")
                }
                in 200..299 -> {
                    val meta = json.decodeFromString<DriveFileMeta>(
                        response.body?.string().orEmpty().ifBlank { "{}" },
                    )
                    val id = meta.id ?: return CloudSaveResult.Failed("network")
                    fileIdStore(id)
                    CloudSaveResult.Written(
                        fileId = id,
                        etag = response.header("ETag").orEmpty(),
                    )
                }
                else -> {
                    logDriveFailure("create", response)
                    CloudSaveResult.Failed("network")
                }
            }
        }
    }

    private suspend fun updateFile(
        fileId: String,
        etag: String,
        bodyJson: String,
        recovering: Boolean,
    ): CloudSaveResult {
        val url = DRIVE_UPLOAD_ROOT.toHttpUrl().newBuilder()
            .addPathSegment("files")
            .addPathSegment(fileId)
            .addQueryParameter("uploadType", "media")
            .addQueryParameter("fields", "id")
            .build()
        execute { token ->
            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("If-Match", etag)
                .patch(bodyJson.toRequestBody(JSON))
                .build()
        }.use { response ->
            return when (response.code) {
                412 -> CloudSaveResult.Stale
                401, 403 -> {
                    logDriveFailure("update", response)
                    CloudSaveResult.Failed("auth")
                }
                404 -> {
                    fileIdStore(null)
                    if (recovering) {
                        CloudSaveResult.Failed("network")
                    } else {
                        recoverMissing(bodyJson)
                    }
                }
                in 200..299 -> {
                    fileIdStore(fileId)
                    CloudSaveResult.Written(
                        fileId = fileId,
                        etag = response.header("ETag").orEmpty(),
                    )
                }
                else -> {
                    logDriveFailure("update", response)
                    CloudSaveResult.Failed("network")
                }
            }
        }
    }

    private suspend fun recoverMissing(bodyJson: String): CloudSaveResult {
        val existing = findFileId()
        if (existing == null) return createFile(bodyJson)
        val freshEtag = fetchEtag(existing)
        if (freshEtag.isNullOrBlank()) return CloudSaveResult.Failed("conflict")
        return updateFile(existing, freshEtag, bodyJson, recovering = true)
    }

    private suspend fun fetchEtag(fileId: String): String? {
        val url = DRIVE_API_ROOT.toHttpUrl().newBuilder()
            .addPathSegment("files")
            .addPathSegment(fileId)
            .addQueryParameter("fields", "id")
            .build()
        execute { token ->
            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .get()
                .build()
        }.use { response ->
            return when (response.code) {
                401, 403 -> {
                    logDriveFailure("etag", response)
                    throw AuthRequiredException()
                }
                404 -> null
                in 200..299 -> response.header("ETag") ?: response.header("etag")
                else -> {
                    logDriveFailure("etag", response)
                    null
                }
            }
        }
    }

    private suspend fun deleteFileById(id: String): Boolean {
        val url = DRIVE_API_ROOT.toHttpUrl().newBuilder()
            .addPathSegment("files")
            .addPathSegment(id)
            .build()
        return try {
            execute { token ->
                Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer $token")
                    .delete()
                    .build()
            }.use { response ->
                when (response.code) {
                    401, 403 -> {
                        logDriveFailure("delete", response)
                        false
                    }
                    404 -> true
                    else -> {
                        if (response.isSuccessful) {
                            true
                        } else {
                            logDriveFailure("delete", response)
                            false
                        }
                    }
                }
            }
        } catch (_: AuthRequiredException) {
            false
        }
    }

    private suspend fun execute(build: (String) -> Request): Response {
        var token = tokenProvider() ?: throw AuthRequiredException()
        var response = http.newCall(build(token)).execute()
        if (response.code == 401 || response.code == 403) {
            response.close()
            onUnauthorized()
            token = tokenProvider() ?: throw AuthRequiredException()
            response = http.newCall(build(token)).execute()
        }
        return response
    }

    private fun logDriveFailure(op: String, response: Response) {
        val snippet = response.body?.string()?.take(400).orEmpty()
        Log.w(TAG, "Drive $op HTTP ${response.code} $snippet")
    }

    class AuthRequiredException : Exception()
    class NetworkException : Exception()

    @Serializable
    private data class DriveListResponse(
        val files: List<DriveFileMeta> = emptyList(),
        val nextPageToken: String? = null,
    )

    @Serializable
    private data class DriveFileMeta(
        val id: String? = null,
        val modifiedTime: String? = null,
    )

    private companion object {
        const val TAG = "ErrataSync"
        const val QUERY = "name = 'errata-sync.json'"
        const val DRIVE_API_ROOT = "https://www.googleapis.com/drive/v3"
        const val DRIVE_UPLOAD_ROOT = "https://www.googleapis.com/upload/drive/v3"
        val JSON = "application/json; charset=UTF-8".toMediaType()
        const val MAX_LIST_PAGES = 20
    }
}
