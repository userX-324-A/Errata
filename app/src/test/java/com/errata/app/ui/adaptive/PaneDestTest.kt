package com.errata.app.ui.adaptive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaneDestTest {

    @Test
    fun taskKey_roundTrip() {
        assertEquals(12L to "", PaneDest.parseTask(PaneDest.task(12L)))
        assertEquals(0L to "bins", PaneDest.parseTask(PaneDest.task(0L, "bins")))
    }

    @Test
    fun taskId_fromKey() {
        assertEquals(7L, PaneDest.taskId(PaneDest.task(7L)))
        assertNull(PaneDest.taskId(PaneDest.CATALOG))
        assertNull(PaneDest.taskId(null))
    }
}
