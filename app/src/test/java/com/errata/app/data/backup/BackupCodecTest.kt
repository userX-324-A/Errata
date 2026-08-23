package com.errata.app.data.backup

import com.errata.app.domain.cadence.CadenceMode
import com.errata.app.domain.cadence.ScheduleKind
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
        assertEquals("SYSTEM", decoded.settings.appearanceMode)
        assertEquals(false, decoded.settings.digestEnabled)
        assertEquals(ScheduleKind.INTERVAL.name, decoded.tasks.single().scheduleKind)
        assertEquals(0, decoded.tasks.single().weekdaysMask)
        assertEquals(0, decoded.tasks.single().monthDay)
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
    fun decode_missingAppearance_defaultsToSystem() {
        val json = BackupCodec.encode(sample()).replace(
            Regex(""",\s*"appearanceMode"\s*:\s*"SYSTEM""""),
            "",
        )
        val decoded = BackupCodec.decode(json)
        assertEquals("SYSTEM", decoded.settings.appearanceMode)
    }

    @Test
    fun decode_missingDigest_defaultsToOff() {
        val json = BackupCodec.encode(sample()).replace(
            Regex(""",\s*"digestEnabled"\s*:\s*false"""),
            "",
        )
        val decoded = BackupCodec.decode(json)
        assertEquals(false, decoded.settings.digestEnabled)
    }

    @Test
    fun roundTrip_preservesDigestEnabled() {
        val original = sample().copy(
            settings = sample().settings.copy(digestEnabled = true),
        )
        val decoded = BackupCodec.decode(BackupCodec.encode(original))
        assertEquals(true, decoded.settings.digestEnabled)
    }

    @Test
    fun roundTrip_preservesWeeklySchedule() {
        val original = sample().copy(
            tasks = listOf(
                sample().tasks.single().copy(
                    scheduleKind = ScheduleKind.WEEKLY.name,
                    weekdaysMask = 5,
                    monthDay = 0,
                    intervalDays = 7,
                ),
            ),
        )
        val decoded = BackupCodec.decode(BackupCodec.encode(original))
        val task = decoded.tasks.single()
        assertEquals(ScheduleKind.WEEKLY.name, task.scheduleKind)
        assertEquals(5, task.weekdaysMask)
        assertEquals(0, task.monthDay)
    }

    @Test
    fun decode_missingScheduleFields_defaultsToInterval() {
        val json = BackupCodec.encode(sample())
            .replace(Regex(""",\s*"scheduleKind"\s*:\s*"INTERVAL""""), "")
            .replace(Regex(""",\s*"weekdaysMask"\s*:\s*0"""), "")
            .replace(Regex(""",\s*"monthDay"\s*:\s*0"""), "")
        val decoded = BackupCodec.decode(json)
        val task = decoded.tasks.single()
        assertEquals(ScheduleKind.INTERVAL.name, task.scheduleKind)
        assertEquals(0, task.weekdaysMask)
        assertEquals(0, task.monthDay)
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
