package com.errata.app.domain.widget

import com.errata.app.domain.cadence.CadenceCalculator
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSnapshotTest {

    private val zone = ZoneOffset.UTC
    private val now = LocalDate.of(2026, 3, 10).atTime(12, 0).toInstant(zone).toEpochMilli()

    private fun startOf(day: Int): Long =
        CadenceCalculator.startOfDayEpochMs(LocalDate.of(2026, 3, day).toEpochDay(), zone)

    private fun item(
        estimate: Int,
        dueDay: Int,
        snoozeDay: Int? = null,
        paused: Boolean = false,
        archived: Boolean = false,
        title: String = "",
    ) = WidgetSnapshot.Item(
        estimateMinutes = estimate,
        nextDueAtEpochMs = startOf(dueDay),
        snoozedUntilEpochMs = snoozeDay?.let { startOf(it) },
        isPaused = paused,
        isArchived = archived,
        title = title,
    )

    @Test
    fun empty_whenNothingDue() {
        val snap = WidgetSnapshot.from(emptyList(), now, zone)
        assertTrue(snap.isEmpty)
        assertEquals(0, snap.totalMinutes)
    }

    @Test
    fun overdueOnly() {
        val snap = WidgetSnapshot.from(
            listOf(item(estimate = 10, dueDay = 8)),
            now,
            zone,
        )
        assertEquals(1, snap.count)
        assertEquals(10, snap.totalMinutes)
    }

    @Test
    fun dueTodayOnly() {
        val snap = WidgetSnapshot.from(
            listOf(item(estimate = 20, dueDay = 10)),
            now,
            zone,
        )
        assertEquals(1, snap.count)
        assertEquals(20, snap.totalMinutes)
    }

    @Test
    fun overdueAndDueToday_sumMinutes() {
        val snap = WidgetSnapshot.from(
            listOf(
                item(estimate = 10, dueDay = 8),
                item(estimate = 15, dueDay = 10),
            ),
            now,
            zone,
        )
        assertEquals(2, snap.count)
        assertEquals(25, snap.totalMinutes)
    }

    @Test
    fun soon_excluded() {
        val snap = WidgetSnapshot.from(
            listOf(
                item(estimate = 10, dueDay = 10),
                item(estimate = 40, dueDay = 12),
            ),
            now,
            zone,
        )
        assertEquals(1, snap.count)
        assertEquals(10, snap.totalMinutes)
    }

    @Test
    fun pausedAndArchived_excluded() {
        val snap = WidgetSnapshot.from(
            listOf(
                item(estimate = 10, dueDay = 10, paused = true),
                item(estimate = 10, dueDay = 10, archived = true),
            ),
            now,
            zone,
        )
        assertTrue(snap.isEmpty)
    }

    @Test
    fun snoozeUntilLaterDay_excluded() {
        val snap = WidgetSnapshot.from(
            listOf(item(estimate = 30, dueDay = 1, snoozeDay = 12)),
            now,
            zone,
        )
        assertTrue(snap.isEmpty)
    }

    @Test
    fun nextMidnight_isStartOfTomorrow() {
        val fire = WidgetSnapshot.nextLocalMidnightEpochMs(now, zone)
        assertEquals(
            LocalDate.of(2026, 3, 11).atStartOfDay(zone).toInstant().toEpochMilli(),
            fire,
        )
    }

    @Test
    fun titles_overdueBeforeDueToday_cappedWithOverflow() {
        val snap = WidgetSnapshot.from(
            listOf(
                item(estimate = 10, dueDay = 10, title = "Today sponge"),
                item(estimate = 5, dueDay = 8, title = "Overdue floss"),
                item(estimate = 8, dueDay = 9, title = "Overdue bins"),
                item(estimate = 12, dueDay = 10, title = "Today filter"),
                item(estimate = 20, dueDay = 10, title = "Today towels"),
            ),
            now,
            zone,
        )
        assertEquals(5, snap.count)
        assertEquals(1, snap.overflowCount)
        assertEquals(
            listOf("Overdue floss", "Overdue bins", "Today filter", "Today sponge"),
            snap.titles,
        )
    }

    @Test
    fun titles_soonExcluded() {
        val snap = WidgetSnapshot.from(
            listOf(
                item(estimate = 10, dueDay = 10, title = "Today"),
                item(estimate = 40, dueDay = 12, title = "Soon"),
            ),
            now,
            zone,
        )
        assertEquals(listOf("Today"), snap.titles)
        assertEquals(0, snap.overflowCount)
    }

    @Test
    fun titleSlots_compactVsTall() {
        assertEquals(0, WidgetSnapshot.titleSlots(40))
        assertEquals(WidgetSnapshot.TITLE_LIMIT, WidgetSnapshot.titleSlots(0))
        assertEquals(WidgetSnapshot.TITLE_LIMIT, WidgetSnapshot.titleSlots(110))
    }
}
