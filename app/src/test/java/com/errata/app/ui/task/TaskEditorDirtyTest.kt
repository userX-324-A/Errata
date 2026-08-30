package com.errata.app.ui.task

import com.errata.app.domain.cadence.CadenceMode
import com.errata.app.domain.cadence.ScheduleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TaskEditorDirtyTest {

    private fun state(
        title: String = "Bins",
        reminderMinutesOfDay: Int? = null,
        dueMinuteOfDay: Int = 9 * 60,
    ) = TaskEditorUiState(
        title = title,
        estimateMinutes = "15",
        intervalDays = "14",
        scheduleKind = ScheduleKind.INTERVAL,
        cadenceMode = CadenceMode.FROM_COMPLETION_CATCH_UP,
        dueMinuteOfDay = dueMinuteOfDay,
        reminderMinutesOfDay = reminderMinutesOfDay,
        loaded = true,
    )

    @Test
    fun fingerprint_ignoresLoadedAndError() {
        val a = state().copy(loaded = false, errorMessage = "title")
        val b = state().copy(loaded = true, errorMessage = null, saved = true)
        assertEquals(a.editFingerprint(), b.editFingerprint())
    }

    @Test
    fun fingerprint_titleAndReminderCountAsEdits() {
        val baseline = state()
        assertNotEquals(baseline.editFingerprint(), state(title = "Bins out").editFingerprint())
        assertNotEquals(
            baseline.editFingerprint(),
            state(reminderMinutesOfDay = 8 * 60).editFingerprint(),
        )
        assertEquals(baseline.editFingerprint(), state().editFingerprint())
    }

    @Test
    fun blankNew_whenDueAndDueClockFromSettings() {
        val dueMinutes = 8 * 60
        val state = TaskEditorUiState().withBlankNew(
            cadenceMode = CadenceMode.FROM_COMPLETION,
            todayEpochDay = 20_000L,
            dueMinutes = dueMinutes,
        )
        assertEquals(null, state.reminderMinutesOfDay)
        assertEquals(dueMinutes, state.dueMinuteOfDay)
        assertEquals(dueMinutes, state.defaultReminderMinutesOfDay)
        assertEquals(20_000L, state.dueEpochDay)
    }
}
