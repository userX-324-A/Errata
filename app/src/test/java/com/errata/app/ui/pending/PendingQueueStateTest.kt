package com.errata.app.ui.pending

import com.errata.app.data.local.SettingsEntity
import com.errata.app.data.local.TaskEntity
import com.errata.app.domain.cadence.CadenceCalculator
import com.errata.app.domain.cadence.CadenceMode
import com.errata.app.domain.freewindow.FreeWindowSelection
import java.time.LocalDate
import java.time.LocalTime
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
        estimateMinutes: Int = 10,
    ) = TaskEntity(
        id = id,
        title = title,
        estimateMinutes = estimateMinutes,
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
            window = null,
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
            window = FreeWindowSelection.Duration(30),
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
            window = null,
            honesty = null,
            requestedArea = "Kitchen",
            hint = false,
        )
        assertFalse(state.areaFilterEmpty)
        assertEquals("Kitchen", state.activeArea)
        assertEquals(listOf("Sponge"), state.dueToday.map { it.task.title })
    }

    @Test
    fun untilWork_keepsClockAndShrinksRemaining() {
        val due = LocalDate.of(2026, 4, 10)
        val longFit = task(1, "Sheets", "House", due, estimateMinutes = 45)
        val shortFit = task(2, "Bins", "House", due, estimateMinutes = 15)
        val settings = SettingsEntity(defaultWorkStartMinutesOfDay = 9 * 60)
        val clock = FreeWindowSelection.UntilClock(9 * 60)
        val atEight = due.atTime(LocalTime.of(8, 0)).toInstant(zone).toEpochMilli()
        val atEightForty = due.atTime(LocalTime.of(8, 40)).toInstant(zone).toEpochMilli()

        val early = PendingQueueState.build(
            tasks = listOf(longFit, shortFit),
            settings = settings,
            now = atEight,
            window = clock,
            honesty = null,
            requestedArea = null,
            hint = false,
            zone = zone,
        )
        assertEquals(60, early.activeWindowMinutes)
        assertTrue(early.untilWorkSelected)
        assertEquals(setOf("Sheets", "Bins"), early.fits.map { it.task.title }.toSet())

        val later = PendingQueueState.build(
            tasks = listOf(longFit, shortFit),
            settings = settings,
            now = atEightForty,
            window = clock,
            honesty = null,
            requestedArea = null,
            hint = false,
            zone = zone,
        )
        assertEquals(20, later.activeWindowMinutes)
        assertTrue(later.untilWorkSelected)
        assertEquals(listOf("Bins"), later.fits.map { it.task.title })
        assertFalse(later.customWindowSelected)
    }

    @Test
    fun untilWork_pastClock_zeroRemainingEmptyFits() {
        val due = LocalDate.of(2026, 4, 10)
        val chore = task(1, "Bins", "House", due, estimateMinutes = 15)
        val later = PendingQueueState.build(
            tasks = listOf(chore),
            settings = SettingsEntity(defaultWorkStartMinutesOfDay = 9 * 60),
            now = due.atTime(LocalTime.of(10, 0)).toInstant(zone).toEpochMilli(),
            window = FreeWindowSelection.UntilClock(9 * 60),
            honesty = null,
            requestedArea = null,
            hint = false,
            zone = zone,
        )
        assertEquals(0, later.activeWindowMinutes)
        assertTrue(later.untilWorkSelected)
        assertTrue(later.fits.isEmpty())
    }

    @Test
    fun durationWindow_staysFrozen() {
        val due = LocalDate.of(2026, 4, 10)
        val chore = task(1, "Bins", "House", due, estimateMinutes = 15)
        val settings = SettingsEntity(defaultWorkStartMinutesOfDay = 9 * 60)
        val duration = FreeWindowSelection.Duration(60)
        val early = PendingQueueState.build(
            tasks = listOf(chore),
            settings = settings,
            now = due.atTime(LocalTime.of(8, 0)).toInstant(zone).toEpochMilli(),
            window = duration,
            honesty = null,
            requestedArea = null,
            hint = false,
            zone = zone,
        )
        val later = PendingQueueState.build(
            tasks = listOf(chore),
            settings = settings,
            now = due.atTime(LocalTime.of(8, 40)).toInstant(zone).toEpochMilli(),
            window = duration,
            honesty = null,
            requestedArea = null,
            hint = false,
            zone = zone,
        )
        assertEquals(60, early.activeWindowMinutes)
        assertEquals(60, later.activeWindowMinutes)
        assertFalse(later.untilWorkSelected)
    }

    @Test
    fun durationPreset_doesNotAlsoSelectUntilWorkWhenRemainingMatches() {
        val due = LocalDate.of(2026, 4, 10)
        val chore = task(1, "Bins", "House", due, estimateMinutes = 15)
        val settings = SettingsEntity(defaultWorkStartMinutesOfDay = 9 * 60)
        val atEightThirty = due.atTime(LocalTime.of(8, 30)).toInstant(zone).toEpochMilli()
        val state = PendingQueueState.build(
            tasks = listOf(chore),
            settings = settings,
            now = atEightThirty,
            window = FreeWindowSelection.Duration(30),
            honesty = null,
            requestedArea = null,
            hint = false,
            zone = zone,
        )
        assertEquals(30, state.activeWindowMinutes)
        assertEquals(30, state.untilWorkMinutes)
        assertFalse(state.untilWorkSelected)
        assertFalse(state.customWindowSelected)
    }

    @Test
    fun customDuration_selectsCustomNotUntilWork() {
        val due = LocalDate.of(2026, 4, 10)
        val chore = task(1, "Bins", "House", due, estimateMinutes = 15)
        val state = PendingQueueState.build(
            tasks = listOf(chore),
            settings = SettingsEntity(defaultWorkStartMinutesOfDay = 9 * 60),
            now = due.atTime(LocalTime.of(8, 0)).toInstant(zone).toEpochMilli(),
            window = FreeWindowSelection.Duration(20),
            honesty = null,
            requestedArea = null,
            hint = false,
            zone = zone,
        )
        assertTrue(state.customWindowSelected)
        assertFalse(state.untilWorkSelected)
        assertEquals(20, state.activeWindowMinutes)
    }
}
