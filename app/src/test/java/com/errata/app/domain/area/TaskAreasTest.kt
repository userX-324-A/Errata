package com.errata.app.domain.area

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskAreasTest {

    @Test
    fun normalize_blankBecomesNull() {
        assertNull(TaskAreas.normalize(null))
        assertNull(TaskAreas.normalize(""))
        assertNull(TaskAreas.normalize("   "))
    }

    @Test
    fun normalize_trimsAndCapsLength() {
        assertEquals("Garden", TaskAreas.normalize("  Garden  "))
        assertEquals("a".repeat(TaskAreas.MAX_LENGTH), TaskAreas.normalize("a".repeat(30)))
    }

    @Test
    fun usedAreas_distinctSortedIgnoreCase() {
        assertEquals(
            listOf("Bathroom", "Garden"),
            TaskAreas.usedAreas(listOf("Garden", "Bathroom", " Garden ", "", null, "Bathroom")),
        )
    }
}
