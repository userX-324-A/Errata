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
        assertTrue(decoded.tasks.single().uuid.isNotBlank())
        assertTrue(decoded.completions.single().uuid.isNotBlank())
        assertEquals(0, decoded.settings.historyGeneration)
        assertEquals(0, decoded.settings.tasksGeneration)
        assertEquals(original.tasks.single().title, decoded.tasks.single().title)
        assertEquals(original.completions.single().taskId, decoded.completions.single().taskId)
        assertEquals(
            original.settings.defaultReminderMinutesOfDay,
            decoded.settings.defaultReminderMinutesOfDay,
        )
        assertEquals("SYSTEM", decoded.settings.appearanceMode)
        assertEquals(false, decoded.settings.digestEnabled)
        assertEquals(730, decoded.settings.historyRetentionDays)
        assertEquals(ScheduleKind.INTERVAL.name, decoded.tasks.single().scheduleKind)
        assertEquals(0, decoded.tasks.single().weekdaysMask)
        assertEquals(0, decoded.tasks.single().monthDay)
        assertEquals(0, decoded.tasks.single().weekdayOrdinal)
        assertEquals(0, decoded.tasks.single().yearMonthsMask)
        assertEquals(0, decoded.tasks.single().seasonMask)
    }

    @Test
    fun roundTrip_preservesUuid() {
        val uuid = "11111111-1111-1111-1111-111111111111"
        val original = sample().copy(
            tasks = listOf(sample().tasks.single().copy(uuid = uuid)),
            completions = listOf(sample().completions.single().copy(uuid = "c1")),
        )
        val decoded = BackupCodec.decode(BackupCodec.encode(original))
        assertEquals(uuid, decoded.tasks.single().uuid)
        assertEquals("c1", decoded.completions.single().uuid)
    }

    @Test
    fun decode_v1_assignsUuids() {
        val json = BackupCodec.encode(sample())
            .replace("\"schemaVersion\": 2", "\"schemaVersion\": 1")
            .replace(Regex(""",\s*"uuid"\s*:\s*"[^"]*""""), "")
        val decoded = BackupCodec.decode(json)
        assertEquals(2, decoded.schemaVersion)
        assertTrue(decoded.tasks.single().uuid.isNotBlank())
        assertTrue(decoded.completions.single().uuid.isNotBlank())
    }

    @Test
    fun reject_wrongSchemaVersion() {
        val json = BackupCodec.encode(sample()).replace(
            "\"schemaVersion\": 2",
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
    fun decode_missingHistoryRetention_defaultsToTwoYears() {
        val json = BackupCodec.encode(sample()).replace(
            Regex(""",\s*"historyRetentionDays"\s*:\s*730"""),
            "",
        )
        val decoded = BackupCodec.decode(json)
        assertEquals(730, decoded.settings.historyRetentionDays)
    }

    @Test
    fun roundTrip_preservesHistoryRetention() {
        val original = sample().copy(
            settings = sample().settings.copy(historyRetentionDays = 90),
        )
        val decoded = BackupCodec.decode(BackupCodec.encode(original))
        assertEquals(90, decoded.settings.historyRetentionDays)
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
        assertEquals(0, task.weekdayOrdinal)
    }

    @Test
    fun roundTrip_preservesNthWeekdaySchedule() {
        val original = sample().copy(
            tasks = listOf(
                sample().tasks.single().copy(
                    scheduleKind = ScheduleKind.NTH_WEEKDAY.name,
                    weekdaysMask = 32,
                    monthDay = 0,
                    weekdayOrdinal = 5,
                    intervalDays = 7,
                ),
            ),
        )
        val decoded = BackupCodec.decode(BackupCodec.encode(original))
        val task = decoded.tasks.single()
        assertEquals(ScheduleKind.NTH_WEEKDAY.name, task.scheduleKind)
        assertEquals(32, task.weekdaysMask)
        assertEquals(0, task.monthDay)
        assertEquals(5, task.weekdayOrdinal)
    }

    @Test
    fun roundTrip_preservesYearlySchedule() {
        val original = sample().copy(
            tasks = listOf(
                sample().tasks.single().copy(
                    scheduleKind = ScheduleKind.YEARLY.name,
                    monthDay = 1,
                    yearMonthsMask = 4,
                    seasonMask = 5,
                    intervalDays = 7,
                ),
            ),
        )
        val decoded = BackupCodec.decode(BackupCodec.encode(original))
        val task = decoded.tasks.single()
        assertEquals(ScheduleKind.YEARLY.name, task.scheduleKind)
        assertEquals(1, task.monthDay)
        assertEquals(4, task.yearMonthsMask)
        assertEquals(5, task.seasonMask)
    }

    @Test
    fun decode_missingScheduleFields_defaultsToInterval() {
        val json = BackupCodec.encode(sample())
            .replace(Regex(""",\s*"scheduleKind"\s*:\s*"INTERVAL""""), "")
            .replace(Regex(""",\s*"weekdaysMask"\s*:\s*0"""), "")
            .replace(Regex(""",\s*"monthDay"\s*:\s*0"""), "")
            .replace(Regex(""",\s*"weekdayOrdinal"\s*:\s*0"""), "")
            .replace(Regex(""",\s*"yearMonthsMask"\s*:\s*0"""), "")
            .replace(Regex(""",\s*"seasonMask"\s*:\s*0"""), "")
        val decoded = BackupCodec.decode(json)
        val task = decoded.tasks.single()
        assertEquals(ScheduleKind.INTERVAL.name, task.scheduleKind)
        assertEquals(0, task.weekdaysMask)
        assertEquals(0, task.monthDay)
        assertEquals(0, task.weekdayOrdinal)
        assertEquals(0, task.yearMonthsMask)
        assertEquals(0, task.seasonMask)
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

    @Test
    fun parseScheduleKind_unknownFailsClosed() {
        try {
            parseScheduleKind("WEEKLY_ISH")
            throw AssertionError("expected BackupFormatException")
        } catch (e: BackupFormatException) {
            assertTrue(e.message!!.contains("WEEKLY_ISH"))
        }
    }
}
