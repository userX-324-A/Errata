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

    @Test
    fun orphans_includeSessionIdsNotYetPersisted() {
        assertEquals(
            setOf(5L, 9L),
            ScheduledAlarmStore.orphans(
                previous = setOf(1L, 9L),
                remaining = setOf(1L, 4L),
                session = setOf(5L, 1L),
            ),
        )
        assertEquals(
            emptySet<Long>(),
            ScheduledAlarmStore.orphans(
                previous = setOf(1L),
                remaining = setOf(1L, 5L),
                session = setOf(5L),
            ),
        )
    }
}
