package com.errata.app.sync

/**
 * Drive v3 File JSON has no etag. Media downloads often send a *weak* content
 * ETag; If-Match uses strong comparison, so that header 412s even with one writer.
 * Only a strong metadata ETag is a valid precondition.
 */
object DriveEtags {
    fun ifMatchValue(raw: String?): String? {
        val t = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (t.startsWith("W/", ignoreCase = true)) return null
        return t
    }

    /** 412 with the same (or missing) metadata tag is not a second writer. */
    fun falsePrecondition(sent: String?, fresh: String?): Boolean {
        val match = ifMatchValue(sent) ?: return false
        val now = ifMatchValue(fresh)
        return now == null || now == match
    }
}
