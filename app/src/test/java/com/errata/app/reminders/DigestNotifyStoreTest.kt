package com.errata.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DigestNotifyStoreTest {

    @Test
    fun idsForDay_emptyWhenStoredDayDiffers() {
        assertEquals(
            emptySet<Long>(),
            DigestNotifyStore.idsForDay(
                storedDay = 20_000L,
                storedIds = setOf(1L, 2L),
                todayEpochDay = 20_001L,
            ),
        )
    }

    @Test
    fun idsForDay_keepsIdsOnSameDay() {
        assertEquals(
            setOf(3L, 9L),
            DigestNotifyStore.idsForDay(
                storedDay = 20_000L,
                storedIds = setOf(3L, 9L),
                todayEpochDay = 20_000L,
            ),
        )
    }

    @Test
    fun idsForDay_emptyWhenNeverStored() {
        assertTrue(
            DigestNotifyStore.idsForDay(
                storedDay = null,
                storedIds = setOf(1L),
                todayEpochDay = 20_000L,
            ).isEmpty(),
        )
    }
}
