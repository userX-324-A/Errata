package com.errata.app.domain.area

/** Area chips duplicate the quiet card label; hide until the list is long enough to need them. */
object AreaFilter {
    const val MIN_DISTINCT_AREAS = 2
    const val MIN_ROWS = 6

    fun shouldShow(usedAreas: List<String>, rowCount: Int): Boolean =
        usedAreas.size >= MIN_DISTINCT_AREAS && rowCount >= MIN_ROWS
}
