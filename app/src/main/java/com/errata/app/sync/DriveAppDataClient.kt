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
) : CloudStore {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun load(): CloudDocument = withContext(Dispatchers.IO) {
        val token = tokenProvider() ?: throw AuthRequiredException()
        val existingId = currentFileId() ?: findFileId(token)
        if (existingId == null) {
            return@withContext CloudDocument(snapshot = null, fileId = null, etag = null)
        }
        fileIdStore(existingId)
        val url = DRIVE_API_ROOT.toHttpUrl().newBuilder()
            .addPathSegment("files")
            .addPathSegment(existingId)
            .addQueryParameter("alt", "media")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        http.newCall(request).execute().use { response ->
            when (response.code) {
                401, 403 -> {
                    logDriveFailure("download", response)
                    throw AuthRequiredException()
                }
                404 -> {
                    fileIdStore(null)
                    CloudDocument(null, null, null)
                }
                in 200..299 -> {
                    val body = response.body?.string().orEmpty()
                    val etag = response.header("ETag") ?: response.header("etag")
                    val snapshot = if (body.isBlank()) {
                        null
                    } else {
                        runCatching { SyncCodec.decode(body) }.getOrNull()
                    }
                    CloudDocument(snapshot, existingId, etag)
                }
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
        val token = tokenProvider() ?: return@withContext CloudSaveResult.Failed("auth")
        val bodyJson = SyncCodec.encode(snapshot)
        if (fileId.isNullOrBlank()) {
            createFile(token, bodyJson)
        } else {
            updateFile(token, fileId, etag, bodyJson)
        }
    }

    override suspend fun delete(): Boolean = withContext(Dispatchers.IO) {
        val token = tokenProvider() ?: return@withContext false
        val id = currentFileId() ?: findFileId(token) ?: return@withContext true
        val url = DRIVE_API_ROOT.toHttpUrl().newBuilder()
            .addPathSegment("files")
            .addPathSegment(id)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .delete()
            .build()
        http.newCall(request).execute().use { response ->
            if (response.code == 401 || response.code == 403) {
                logDriveFailure("delete", response)
                return@use false
            }
            if (response.code == 404 || response.isSuccessful) {
                fileIdStore(null)
                true
            } else {
                logDriveFailure("delete", response)
                false
            }
        }
    }

    private fun findFileId(token: String): String? {
        val url = DRIVE_API_ROOT.toHttpUrl().newBuilder()
            .addPathSegment("files")
            .addQueryParameter("spaces", "appDataFolder")
            .addQueryParameter("q", QUERY)
            .addQueryParameter("fields", "files(id)")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        http.newCall(request).execute().use { response ->
            if (response.code == 401 || response.code == 403) {
                logDriveFailure("list", response)
                throw AuthRequiredException()
            }
            if (!response.isSuccessful) {
                logDriveFailure("list", response)
                throw NetworkException()
            }
            val parsed = json.decodeFromString<DriveListResponse>(
                response.body?.string().orEmpty().ifBlank { "{}" },
            )
            return parsed.files.firstOrNull()?.id
        }
    }

    private fun createFile(token: String, bodyJson: String): CloudSaveResult {
        val boundary = "errata_sync"
        val metadata = """{"name":"$FILE_NAME","parents":["appDataFolder"]}"""
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
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .post(
                payload.toRequestBody("multipart/related; boundary=$boundary".toMediaType()),
            )
            .build()
        http.newCall(request).execute().use { response ->
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

    private fun updateFile(
        token: String,
        fileId: String,
        etag: String?,
        bodyJson: String,
    ): CloudSaveResult {
        val url = DRIVE_UPLOAD_ROOT.toHttpUrl().newBuilder()
            .addPathSegment("files")
            .addPathSegment(fileId)
            .addQueryParameter("uploadType", "media")
            .addQueryParameter("fields", "id")
            .build()
        val builder = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .patch(bodyJson.toRequestBody(JSON))
        if (!etag.isNullOrBlank()) {
            builder.header("If-Match", etag)
        }
        http.newCall(builder.build()).execute().use { response ->
            return when (response.code) {
                412 -> CloudSaveResult.Stale
                401, 403 -> {
                    logDriveFailure("update", response)
                    CloudSaveResult.Failed("auth")
                }
                404 -> createFile(token, bodyJson)
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

    private fun logDriveFailure(op: String, response: Response) {
        val snippet = response.body?.string()?.take(400).orEmpty()
        Log.w(TAG, "Drive $op HTTP ${response.code} $snippet")
    }

    class AuthRequiredException : Exception()
    class NetworkException : Exception()

    @Serializable
    private data class DriveListResponse(val files: List<DriveFileMeta> = emptyList())

    @Serializable
    private data class DriveFileMeta(val id: String? = null)

    private companion object {
        const val TAG = "ErrataSync"
        const val FILE_NAME = "errata-sync.json"
        const val QUERY = "name = 'errata-sync.json'"
        const val DRIVE_API_ROOT = "https://www.googleapis.com/drive/v3"
        const val DRIVE_UPLOAD_ROOT = "https://www.googleapis.com/upload/drive/v3"
        val JSON = "application/json; charset=UTF-8".toMediaType()
    }
}
