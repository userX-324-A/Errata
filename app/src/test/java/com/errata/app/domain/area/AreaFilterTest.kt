package com.errata.app.domain.area

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AreaFilterTest {

    @Test
    fun hidesUntilTwoAreasAndSixRows() {
        assertFalse(AreaFilter.shouldShow(listOf("Bathroom"), 10))
        assertFalse(AreaFilter.shouldShow(listOf("Bathroom", "Kitchen"), 5))
        assertTrue(AreaFilter.shouldShow(listOf("Bathroom", "Kitchen"), 6))
    }
}
