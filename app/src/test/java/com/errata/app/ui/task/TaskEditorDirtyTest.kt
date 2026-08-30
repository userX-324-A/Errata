package com.errata.app.ui.task

import com.errata.app.domain.cadence.CadenceMode
import com.errata.app.domain.cadence.ScheduleKind
import com.errata.app.domain.reminders.ReminderPolicy
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

    @Test
    fun blankNew_inheritsNoneAndClock() {
        val dueMinutes = 18 * 60
        val none = TaskEditorUiState().withBlankNew(
            cadenceMode = CadenceMode.FROM_COMPLETION,
            todayEpochDay = 20_000L,
            dueMinutes = dueMinutes,
            storedReminderMinutes = ReminderPolicy.NONE,
        )
        assertEquals(ReminderPolicy.NONE, none.reminderMinutesOfDay)
        assertEquals(dueMinutes, none.dueMinuteOfDay)
        val clock = TaskEditorUiState().withBlankNew(
            cadenceMode = CadenceMode.FROM_COMPLETION,
            todayEpochDay = 20_000L,
            dueMinutes = dueMinutes,
            storedReminderMinutes = dueMinutes,
        )
        assertEquals(dueMinutes, clock.reminderMinutesOfDay)
        assertEquals("", clock.estimateMinutes)
    }

    @Test
    fun blankNew_startsWithEmptyEstimate() {
        val state = TaskEditorUiState().withBlankNew(
            cadenceMode = CadenceMode.FROM_COMPLETION,
            todayEpochDay = 20_000L,
            dueMinutes = 9 * 60,
        )
        assertEquals("", state.estimateMinutes)
        assertEquals("", TaskEditorUiState().estimateMinutes)
    }

    @Test
    fun adoptSavedRow_keepsFingerprint_soSaveMustMarkBaseline() {
        val edited = state(title = "Bins out")
        val saved = edited.adoptSavedRow(
            id = 7L,
            uuid = "11111111-1111-1111-1111-111111111111",
            createdAtEpochMs = 99L,
        )
        assertEquals(edited.editFingerprint(), saved.editFingerprint())
    }

    @Test
    fun fingerprint_noneIsDistinctFromWhenDue() {
        val whenDue = state(reminderMinutesOfDay = null)
        val none = state(reminderMinutesOfDay = ReminderPolicy.NONE)
        assertNotEquals(whenDue.editFingerprint(), none.editFingerprint())
    }

    @Test
    fun fingerprint_ignoresSavingAndSaved() {
        val baseline = state()
        assertEquals(
            baseline.editFingerprint(),
            state().copy(saved = true, saving = true).editFingerprint(),
        )
    }

    @Test
    fun shouldSkipSave_whenSavedOrSaving() {
        assertEquals(false, state().shouldSkipSave())
        assertEquals(true, state().copy(saved = true).shouldSkipSave())
        assertEquals(true, state().copy(saving = true).shouldSkipSave())
    }

    @Test
    fun adoptSavedRow_promotesNewTask() {
        val next = TaskEditorUiState(isNew = true, saving = true).adoptSavedRow(
            id = 7L,
            uuid = "11111111-1111-1111-1111-111111111111",
            createdAtEpochMs = 99L,
        )
        assertEquals(7L, next.existingId)
        assertEquals("11111111-1111-1111-1111-111111111111", next.existingUuid)
        assertEquals(false, next.isNew)
        assertEquals(true, next.saved)
        assertEquals(false, next.saving)
        assertEquals(99L, next.createdAtEpochMs)
    }

    @Test
    fun existingFixedAnchor_dueOrIntervalEdit_retargetsGrid() {
        val loaded = state().copy(
            isNew = false,
            dueEpochDay = 20_000L,
            anchorEpochDay = 19_900L,
            intervalDays = "14",
            cadenceMode = CadenceMode.FIXED_ANCHOR,
        )
        assertEquals(19_900L, loaded.anchorOnSave(loaded))
        assertEquals(20_010L, loaded.copy(dueEpochDay = 20_010L).anchorOnSave(loaded))
        assertEquals(20_000L, loaded.copy(intervalDays = "21").anchorOnSave(loaded))
        assertEquals(
            20_000L,
            loaded.copy(cadenceMode = CadenceMode.FROM_COMPLETION).anchorOnSave(loaded),
        )
        assertEquals(19_900L, loaded.copy(title = "Bins out").anchorOnSave(loaded))
    }

    @Test
    fun newTask_anchorIsDueDay() {
        val draft = state().copy(isNew = true, dueEpochDay = 20_050L, anchorEpochDay = 0L)
        assertEquals(20_050L, draft.anchorOnSave(null))
    }

    @Test
    fun dueEdit_clearsSnooze_titleDoesNot() {
        val loaded = state().copy(
            isNew = false,
            dueEpochDay = 20_000L,
            dueMinuteOfDay = 9 * 60,
            snoozedUntilEpochMs = 99L,
        )
        assertEquals(99L, loaded.copy(title = "Bins out").snoozeOnSave(loaded))
        assertEquals(null, loaded.copy(dueEpochDay = 20_001L).snoozeOnSave(loaded))
        assertEquals(null, loaded.copy(dueMinuteOfDay = 10 * 60).snoozeOnSave(loaded))
    }
}
