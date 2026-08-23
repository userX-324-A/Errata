package com.errata.app.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.nio.charset.StandardCharsets

/**
 * Persistable SAF tree + read/write of [BackupFolder.FILE_NAME].
 * User-initiated only; no folder watch.
 */
class BackupFolderStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(BackupFolder.PREFS, Context.MODE_PRIVATE)
    private val resolver = app.contentResolver

    fun uri(): Uri? =
        BackupFolder.persistableUriString(prefs.getString(BackupFolder.KEY_URI, null))
            ?.let(Uri::parse)

    fun displayName(): String? {
        val tree = uri() ?: return null
        return try {
            queryDisplayName(documentUriForTree(tree))
        } catch (_: Exception) {
            null
        }
    }

    fun setTreeUri(uri: Uri) {
        val flags = persistFlags()
        uri()?.let { old ->
            if (old != uri) {
                releaseQuietly(old, flags)
            }
        }
        try {
            resolver.takePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            throw BackupFolderException("folder_unavailable")
        }
        prefs.edit().putString(BackupFolder.KEY_URI, uri.toString()).apply()
    }

    fun clear() {
        uri()?.let { releaseQuietly(it, persistFlags()) }
        prefs.edit().remove(BackupFolder.KEY_URI).apply()
    }

    fun writeJson(json: String) {
        val tree = uri() ?: throw BackupFolderException("no_folder")
        val fileUri = try {
            findOrCreateBackupFile(tree)
        } catch (e: BackupFolderException) {
            throw e
        } catch (_: SecurityException) {
            throw BackupFolderException("folder_unavailable")
        } catch (_: Exception) {
            throw BackupFolderException("folder_unavailable")
        }
        try {
            val bytes = json.toByteArray(StandardCharsets.UTF_8)
            val out = resolver.openOutputStream(fileUri, "wt")
                ?: resolver.openOutputStream(fileUri)
                ?: throw BackupFolderException("folder_unavailable")
            out.use { it.write(bytes) }
        } catch (e: BackupFolderException) {
            throw e
        } catch (_: SecurityException) {
            throw BackupFolderException("folder_unavailable")
        }
    }

    fun readJson(): String {
        val tree = uri() ?: throw BackupFolderException("no_folder")
        val fileUri = try {
            findBackupFile(tree) ?: throw BackupFolderException("file_missing")
        } catch (e: BackupFolderException) {
            throw e
        } catch (_: SecurityException) {
            throw BackupFolderException("folder_unavailable")
        } catch (_: Exception) {
            throw BackupFolderException("folder_unavailable")
        }
        return try {
            resolver.openInputStream(fileUri)?.use { input ->
                input.reader(StandardCharsets.UTF_8).readText()
            } ?: throw BackupFolderException("folder_unavailable")
        } catch (e: BackupFolderException) {
            throw e
        } catch (_: SecurityException) {
            throw BackupFolderException("folder_unavailable")
        }
    }

    private fun persistFlags(): Int =
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

    private fun releaseQuietly(uri: Uri, flags: Int) {
        try {
            resolver.releasePersistableUriPermission(uri, flags)
        } catch (_: SecurityException) {
            // Already gone.
        }
    }

    private fun documentUriForTree(tree: Uri): Uri {
        val docId = DocumentsContract.getTreeDocumentId(tree)
        return DocumentsContract.buildDocumentUriUsingTree(tree, docId)
    }

    private fun findBackupFile(tree: Uri): Uri? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            tree,
            DocumentsContract.getTreeDocumentId(tree),
        )
        resolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIdx) == BackupFolder.FILE_NAME) {
                    return DocumentsContract.buildDocumentUriUsingTree(
                        tree,
                        cursor.getString(idIdx),
                    )
                }
            }
        }
        return null
    }

    private fun findOrCreateBackupFile(tree: Uri): Uri {
        findBackupFile(tree)?.let { return it }
        val parent = documentUriForTree(tree)
        return DocumentsContract.createDocument(
            resolver,
            parent,
            "application/json",
            BackupFolder.FILE_NAME,
        ) ?: throw BackupFolderException("folder_unavailable")
    }

    private fun queryDisplayName(documentUri: Uri): String? {
        resolver.query(
            documentUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                if (idx >= 0) return cursor.getString(idx)
            }
        }
        return null
    }
}
