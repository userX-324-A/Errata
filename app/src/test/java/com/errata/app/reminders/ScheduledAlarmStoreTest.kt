package com.errata.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduledAlarmStoreTest {

    @Test
    fun orphans_arePreviousMinusRemaining() {
        assertEquals(
            setOf(2L, 9L),
            ScheduledAlarmStore.orphans(
                previous = setOf(1L, 2L, 9L),
                remaining = setOf(1L, 4L),
            ),
        )
        assertEquals(emptySet<Long>(), ScheduledAlarmStore.orphans(emptySet(), setOf(1L)))
    }
}
