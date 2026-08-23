package com.errata.app.data.backup

import com.errata.app.domain.cadence.CadenceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private fun sample(): ErrataBackup = ErrataBackup(
        exportedAtEpochMs = 1_700_000_000_000L,
        settings = SettingsBackup(
            defaultCadenceMode = CadenceMode.FROM_COMPLETION_CATCH_UP.name,
            defaultReminderMinutesOfDay = 540,
            soonHorizonDays = 7,
        ),
        tasks = listOf(
            TaskBackup(
                id = 3,
                title = "Filters",
                notes = null,
                estimateMinutes = 20,
                intervalDays = 30,
                cadenceMode = CadenceMode.FROM_COMPLETION.name,
                anchorEpochDay = 20_000,
                nextDueAtEpochMs = 1_700_000_100_000L,
                createdAtEpochMs = 1_700_000_000_000L,
                updatedAtEpochMs = 1_700_000_000_000L,
            ),
        ),
        completions = listOf(
            CompletionBackup(
                id = 1,
                taskId = 3,
                completedAtEpochMs = 1_699_000_000_000L,
                scheduledDueAtEpochMs = 1_698_000_000_000L,
                estimateMinutesAtCompletion = 20,
            ),
        ),
    )

    @Test
    fun roundTrip_preservesFields() {
        val original = sample()
        val decoded = BackupCodec.decode(BackupCodec.encode(original))
        assertEquals(original.schemaVersion, decoded.schemaVersion)
        assertEquals(original.tasks.single().title, decoded.tasks.single().title)
        assertEquals(original.completions.single().taskId, decoded.completions.single().taskId)
        assertEquals(
            original.settings.defaultReminderMinutesOfDay,
            decoded.settings.defaultReminderMinutesOfDay,
        )
    }

    @Test
    fun reject_wrongSchemaVersion() {
        val bad = sample().copy(schemaVersion = 99)
        val json = BackupCodec.encode(bad).replace(
            "\"schemaVersion\": 99",
            "\"schemaVersion\": 99",
        )
        // encode already has 99; decode should reject
        try {
            BackupCodec.decode(json)
            throw AssertionError("expected BackupFormatException")
        } catch (e: BackupFormatException) {
            assertTrue(e.message!!.contains("99"))
        }
    }

    @Test
    fun reject_garbageJson() {
        try {
            BackupCodec.decode("{not json")
            throw AssertionError("expected BackupFormatException")
        } catch (e: BackupFormatException) {
            assertTrue(e.message!!.isNotBlank())
        }
    }
}
