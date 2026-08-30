package com.errata.app.sync

/**
 * One hidden errata-sync.json in appDataFolder. If two devices create at once,
 * keep the newest and drop the rest.
 */
object DriveSyncFiles {
    const val FILE_NAME = "errata-sync.json"

    data class FileRef(
        val id: String,
        val modifiedTime: String = "",
    )

    fun pickCanonical(files: List<FileRef>): FileRef? {
        if (files.isEmpty()) return null
        return files.maxWithOrNull(
            compareBy<FileRef> { it.modifiedTime }.thenBy { it.id },
        )
    }

    fun orphanIds(files: List<FileRef>, canonicalId: String): List<String> =
        files.map { it.id }.filter { it != canonicalId }

    /** Blank or whitespace-only Drive media is corrupt, not an empty snapshot. */
    fun mediaUnreadable(body: String): Boolean = body.isBlank()

    /** Wipe succeeded only when every listed copy was deleted (or none existed). */
    fun wipeComplete(listed: List<FileRef>, deletedIds: Set<String>): Boolean =
        listed.all { it.id in deletedIds }
}
