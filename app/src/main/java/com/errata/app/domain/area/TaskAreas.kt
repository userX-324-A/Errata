package com.errata.app.domain.area

object TaskAreas {
    const val MAX_LENGTH = 24

    val PRESETS = listOf("Bathroom", "Body", "Car", "House", "Paper", "Kitchen", "Clothes")

    fun normalize(raw: String?): String? {
        val trimmed = raw?.trim()?.take(MAX_LENGTH).orEmpty()
        return trimmed.ifEmpty { null }
    }

    fun usedAreas(values: Iterable<String?>): List<String> =
        values.mapNotNull { normalize(it) }.distinct().sortedBy { it.lowercase() }
}
