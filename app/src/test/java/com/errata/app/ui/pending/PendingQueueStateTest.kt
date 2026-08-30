package com.errata.app.ui.pending

import com.errata.app.data.local.SettingsEntity
import com.errata.app.data.local.TaskEntity
import com.errata.app.domain.cadence.CadenceCalculator
import com.errata.app.domain.cadence.CadenceMode
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingQueueStateTest {

    private val zone = ZoneOffset.UTC
    private val now = LocalDate.of(2026, 4, 10).atTime(12, 0).toInstant(zone).toEpochMilli()

    private fun task(
        id: Long,
        title: String,
        area: String?,
        dueDay: LocalDate,
    ) = TaskEntity(
        id = id,
        title = title,
        estimateMinutes = 10,
        intervalDays = 7,
        cadenceMode = CadenceMode.FROM_COMPLETION,
        anchorEpochDay = dueDay.toEpochDay(),
        nextDueAtEpochMs = CadenceCalculator.atLocalDateMinutes(dueDay.toEpochDay(), 12 * 60, zone),
        area = area,
        createdAtEpochMs = 0,
        updatedAtEpochMs = 0,
    )

    @Test
    fun areaFilter_kitchenEmpty_keepsChipsAndEmptyCopy() {
        val bathroomDue = task(1, "Bins", "Bathroom", LocalDate.of(2026, 4, 10))
        val kitchenLater = task(2, "Sponge", "Kitchen", LocalDate.of(2026, 5, 1))
        val state = PendingQueueState.build(
            tasks = listOf(bathroomDue, kitchenLater),
            settings = SettingsEntity(),
            now = now,
            windowMinutes = null,
            honesty = null,
            requestedArea = "Kitchen",
            hint = false,
        )
        assertFalse(state.isEmpty)
        assertTrue(state.areaFilterEmpty)
        assertEquals("Kitchen", state.activeArea)
        assertTrue(state.overdue.isEmpty() && state.dueToday.isEmpty() && state.soon.isEmpty())
        assertEquals(listOf("Bathroom", "Kitchen"), state.availableAreas)
    }

    @Test
    fun areaFilter_kitchenEmpty_withWindow_stillAreaEmpty() {
        val bathroomDue = task(1, "Bins", "Bathroom", LocalDate.of(2026, 4, 10))
        val kitchenLater = task(2, "Sponge", "Kitchen", LocalDate.of(2026, 5, 1))
        val state = PendingQueueState.build(
            tasks = listOf(bathroomDue, kitchenLater),
            settings = SettingsEntity(),
            now = now,
            windowMinutes = 30,
            honesty = null,
            requestedArea = "Kitchen",
            hint = false,
        )
        assertTrue(state.areaFilterEmpty)
        assertEquals("Kitchen", state.activeArea)
        assertEquals(30, state.activeWindowMinutes)
        assertTrue(state.fits.isEmpty())
    }

    @Test
    fun areaFilter_paddedAreaStillMatches() {
        val kitchenDue = task(1, "Sponge", " Kitchen ", LocalDate.of(2026, 4, 10))
        val bathroomDue = task(2, "Bins", "Bathroom", LocalDate.of(2026, 4, 10))
        val state = PendingQueueState.build(
            tasks = listOf(kitchenDue, bathroomDue),
            settings = SettingsEntity(),
            now = now,
            windowMinutes = null,
            honesty = null,
            requestedArea = "Kitchen",
            hint = false,
        )
        assertFalse(state.areaFilterEmpty)
        assertEquals("Kitchen", state.activeArea)
        assertEquals(listOf("Sponge"), state.dueToday.map { it.task.title })
    }
}
