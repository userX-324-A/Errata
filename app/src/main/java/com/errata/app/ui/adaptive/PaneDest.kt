package com.errata.app.ui.adaptive

/**
 * Saveable string keys for [androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator].
 * Compact: list or detail. Expanded: list + detail side by side.
 */
object PaneDest {
    const val CATALOG = "catalog"
    const val BACKUP = "backup"
    const val PRIVACY = "privacy"

    fun task(taskId: Long, starterId: String = ""): String = "task:$taskId:$starterId"

    fun isTask(key: String): Boolean = key.startsWith("task:")

    fun parseTask(key: String): Pair<Long, String>? {
        if (!isTask(key)) return null
        val rest = key.removePrefix("task:")
        val idPart = rest.substringBefore(':')
        val starter = rest.substringAfter(':', "")
        val id = idPart.toLongOrNull() ?: return null
        return id to starter
    }

    fun taskId(key: String?): Long? = key?.let { parseTask(it)?.first }
}
