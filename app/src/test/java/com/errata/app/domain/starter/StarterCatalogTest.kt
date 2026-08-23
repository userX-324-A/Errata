package com.errata.app.domain.starter

import com.errata.app.domain.area.TaskAreas
import com.errata.app.domain.cadence.CadenceCalculator
import com.errata.app.domain.cadence.CadenceMode
import com.errata.app.domain.cadence.ScheduleKind
import com.errata.app.domain.cadence.Weekdays
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StarterCatalogTest {

    private val zone = ZoneOffset.UTC
    private val reminder = 9 * 60

    private fun noon(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atTime(12, 0).toInstant(zone).toEpochMilli()

    private fun atReminder(year: Int, month: Int, day: Int): Long =
        CadenceCalculator.atLocalDateMinutes(
            LocalDate.of(year, month, day).toEpochDay(),
            reminder,
            zone,
        )

    @Test
    fun pack_coversAreasAndCadenceKinds() {
        assertEquals(8, StarterCatalog.ALL.size)
        assertEquals(StarterCatalog.ALL.size, StarterCatalog.ALL.map { it.id }.distinct().size)
        val areas = StarterCatalog.ALL.map { it.area }.toSet()
        TaskAreas.PRESETS.forEach { preset ->
            assertTrue("missing area $preset", preset in areas)
        }
        assertTrue(StarterCatalog.ALL.any { it.scheduleKind == ScheduleKind.WEEKLY })
        assertTrue(StarterCatalog.ALL.any { it.scheduleKind == ScheduleKind.MONTHLY })
        val bins = StarterCatalog.ALL.single { it.id == "bins" }
        assertEquals(Weekdays.bit(DayOfWeek.TUESDAY), bins.weekdaysMask)
    }

    @Test
    fun specsByIds_emptySelection_isEmpty() {
        assertTrue(StarterCatalog.specsByIds(emptySet()).isEmpty())
    }

    @Test
    fun specsByIds_preservesCatalogOrder() {
        val picked = StarterCatalog.specsByIds(listOf("bill", "nails"))
        assertEquals(listOf("nails", "bill"), picked.map { it.id })
    }

    @Test
    fun interval_firstDueIsTomorrowAtReminder() {
        val spec = StarterCatalog.ALL.single { it.id == "nails" }
        val now = noon(2026, 1, 10)
        val due = StarterCatalog.firstDueEpochMs(spec, reminder, now, zone)
        assertEquals(atReminder(2026, 1, 11), due)
    }

    @Test
    fun weekly_nextTuesdayAfterMonday() {
        val spec = StarterCatalog.ALL.single { it.id == "bins" }
        val now = noon(2026, 1, 12) // Monday
        val due = StarterCatalog.firstDueEpochMs(spec, reminder, now, zone)
        assertEquals(atReminder(2026, 1, 13), due)
    }

    @Test
    fun weekly_afterTuesdayReminder_wrapsToNextWeek() {
        val spec = StarterCatalog.ALL.single { it.id == "bins" }
        val now = LocalDate.of(2026, 1, 13).atTime(10, 0).toInstant(zone).toEpochMilli()
        val due = StarterCatalog.firstDueEpochMs(spec, reminder, now, zone)
        assertEquals(atReminder(2026, 1, 20), due)
    }

    @Test
    fun monthly_day1_nextMonth() {
        val spec = StarterCatalog.ALL.single { it.id == "bill" }
        val now = noon(2026, 1, 15)
        val due = StarterCatalog.firstDueEpochMs(spec, reminder, now, zone)
        assertEquals(atReminder(2026, 2, 1), due)
    }

    @Test
    fun materialize_usesSettingsAndLeavesReminderDefault() {
        val spec = StarterCatalog.ALL.single { it.id == "bathroom" }
        val now = noon(2026, 3, 1)
        val entity = StarterCatalog.materialize(
            spec = spec,
            cadenceMode = CadenceMode.FIXED_ANCHOR,
            reminderMinutesOfDay = reminder,
            nowEpochMs = now,
            zone = zone,
        )
        assertEquals(0L, entity.id)
        assertEquals("Clean bathroom", entity.title)
        assertEquals("Bathroom", entity.area)
        assertEquals(7, entity.intervalDays)
        assertEquals(ScheduleKind.INTERVAL, entity.scheduleKind)
        assertEquals(CadenceMode.FIXED_ANCHOR, entity.cadenceMode)
        assertNull(entity.reminderMinutesOfDay)
        assertEquals(atReminder(2026, 3, 2), entity.nextDueAtEpochMs)
        assertEquals(LocalDate.of(2026, 3, 2).toEpochDay(), entity.anchorEpochDay)
        assertEquals(false, entity.isPaused)
    }

    @Test
    fun cadenceSummary_weeklyAndMonthly() {
        val bins = StarterCatalog.ALL.single { it.id == "bins" }
        val bill = StarterCatalog.ALL.single { it.id == "bill" }
        assertTrue(StarterCatalog.cadenceSummary(bins).startsWith("Weekly"))
        assertEquals("Monthly · day 1", StarterCatalog.cadenceSummary(bill))
    }
}
