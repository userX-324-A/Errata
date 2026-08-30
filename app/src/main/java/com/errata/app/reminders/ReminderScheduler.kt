package com.errata.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.errata.app.data.TaskRepository
import com.errata.app.data.local.SettingsEntity
import com.errata.app.data.local.TaskEntity
import com.errata.app.domain.digest.DigestPlanner
import com.errata.app.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ReminderScheduler(
    private val context: Context,
    private val repository: TaskRepository,
    private val widgetUpdater: WidgetUpdater,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val digestNotifyLock = Mutex()
    private val alarmIdsLock = Mutex()
    private val sessionAlarmIds = mutableSetOf<Long>()

    suspend fun rescheduleAll(sameDayAppearedAtEpochMs: Long = 0L) = withContext(Dispatchers.IO) {
        val settings = repository.getSettings()
        val tasks = repository.listSchedulableTasks()
        if (settings.digestEnabled && NotificationAccess.areEnabled(context)) {
            val now = System.currentTimeMillis()
            scheduleDigest(
                settings.defaultReminderMinutesOfDay,
                nowEpochMs = now,
                afterFire = DigestPlanner.alreadyPostedToday(
                    DigestNotifyStore.lastNotifiedEpochDay(context),
                    now,
                ),
            )
        } else {
            cancelDigest()
        }
        val scheduled = mutableSetOf<Long>()
        tasks.forEach { task ->
            if (scheduleTask(task, settings, notifyIfMissedDigest = false)) {
                scheduled += task.id
            }
        }
        alarmIdsLock.withLock {
            val orphans = ScheduledAlarmStore.orphans(
                previous = ScheduledAlarmStore.load(context),
                remaining = scheduled,
                session = sessionAlarmIds,
            )
            orphans.forEach { cancel(it) }
            sessionAlarmIds.clear()
            sessionAlarmIds.addAll(scheduled)
            ScheduledAlarmStore.save(context, scheduled)
        }
        // Owns the widget pass; callers that also afterWrite skip a second refresh.
        widgetUpdater.refresh()
        notifyMissedDigestIfNeeded(sameDayAppearedAtEpochMs)
    }

    suspend fun rescheduleTask(taskId: Long) = withContext(Dispatchers.IO) {
        val task = repository.getTask(taskId)
        if (task == null || task.isArchived || task.isPaused) {
            forgetScheduled(taskId)
            return@withContext
        }
        val settings = repository.getSettings()
        if (scheduleTask(task, settings, notifyIfMissedDigest = true)) {
            rememberScheduled(taskId)
        } else {
            forgetScheduled(taskId)
        }
    }

    /**
     * If today's digest window has passed and we have not posted yet this local day,
     * show current members (missed standing alarm). Otherwise only post-digest new pins.
     */
    suspend fun notifyMissedDigestIfNeeded(sameDayAppearedAtEpochMs: Long = 0L) = withContext(Dispatchers.IO) {
        digestNotifyLock.withLock {
            val settings = repository.getSettings()
            if (!settings.digestEnabled || !NotificationAccess.areEnabled(context)) return@withLock
            val now = System.currentTimeMillis()
            val minutes = settings.defaultReminderMinutesOfDay
            if (DigestPlanner.todaysDigestPending(minutes, now)) return@withLock

            val today = DigestPlanner.localEpochDay(now)
            val tasks = repository.listSchedulableTasks()
            if (DigestPlanner.shouldReplayMissedDigest(
                    lastNotifiedEpochDay = DigestNotifyStore.lastNotifiedEpochDay(context),
                    defaultReminderMinutesOfDay = minutes,
                    nowEpochMs = now,
                )
            ) {
                val members = DigestPlanner.members(
                    candidates = tasks.map { it.toDigestCandidate() },
                    defaultReminderMinutesOfDay = minutes,
                    nowEpochMs = now,
                )
                val memberTasks = members.mapNotNull { candidate ->
                    tasks.find { it.id == candidate.id }
                }
                postDigestCards(memberTasks)
                DigestNotifyStore.markNotified(context, today)
                return@withLock
            }

            val fallback = tasks.filter { task ->
                DigestPlanner.sameDayFallback(
                    candidate = task.toDigestCandidate(),
                    defaultReminderMinutesOfDay = minutes,
                    nowEpochMs = now,
                    appearedAtEpochMs = sameDayAppearedAtEpochMs,
                ) && !DigestNotifyStore.alreadyPostedFallback(context, today, task.id)
            }
            postDigestCards(fallback)
            DigestNotifyStore.markFallbackPosted(context, today, fallback.map { it.id })
        }
    }

    suspend fun onDigestFired() = withContext(Dispatchers.IO) {
        digestNotifyLock.withLock {
            val settings = repository.getSettings()
            if (!settings.digestEnabled || !NotificationAccess.areEnabled(context)) {
                cancelDigest()
                return@withLock
            }
            val now = System.currentTimeMillis()
            // Miss-replay in rescheduleAll can mark this local day first when a
            // retained digest RTC starts a dead process. Do not post a second card.
            if (DigestPlanner.alreadyPostedToday(
                    DigestNotifyStore.lastNotifiedEpochDay(context),
                    now,
                )
            ) {
                scheduleDigest(
                    settings.defaultReminderMinutesOfDay,
                    nowEpochMs = now,
                    afterFire = true,
                )
                return@withLock
            }
            val members = DigestPlanner.members(
                candidates = repository.listSchedulableTasks().map { it.toDigestCandidate() },
                defaultReminderMinutesOfDay = settings.defaultReminderMinutesOfDay,
                nowEpochMs = now,
            )
            when (members.size) {
                0 -> Unit
                1 -> {
                    val task = repository.getTask(members.single().id)
                    if (task != null) {
                        NotificationHelper.showDueReminder(context, task)
                    }
                }
                else -> NotificationHelper.showDigest(
                    context,
                    count = members.size,
                    totalMinutes = DigestPlanner.totalMinutes(members),
                )
            }
            DigestNotifyStore.markNotified(context, DigestPlanner.localEpochDay(now))
            scheduleDigest(settings.defaultReminderMinutesOfDay, nowEpochMs = now, afterFire = true)
        }
    }

    private fun postDigestCards(tasks: List<TaskEntity>) {
        when (tasks.size) {
            0 -> Unit
            1 -> NotificationHelper.showDueReminder(context, tasks.single())
            else -> NotificationHelper.showDigest(
                context,
                count = tasks.size,
                totalMinutes = DigestPlanner.totalMinutes(tasks.map { it.toDigestCandidate() }),
            )
        }
    }

    fun cancel(taskId: Long) {
        alarmManager.cancel(alarmPendingIntent(taskId))
    }

    private suspend fun rememberScheduled(taskId: Long) {
        alarmIdsLock.withLock {
            sessionAlarmIds.add(taskId)
            ScheduledAlarmStore.add(context, taskId)
        }
    }

    private suspend fun forgetScheduled(taskId: Long) {
        alarmIdsLock.withLock {
            sessionAlarmIds.remove(taskId)
            ScheduledAlarmStore.remove(context, taskId)
        }
        cancel(taskId)
    }

    /** Drop a posted per-task card, its Done/Snooze actions, and a stale digest snapshot. */
    fun dismissPostedReminder(taskId: Long) {
        NotificationHelper.dismiss(context, taskId)
        NotificationHelper.cancelActions(context, taskId)
        NotificationHelper.dismissDigest(context)
    }

    private fun scheduleTask(
        task: TaskEntity,
        settings: SettingsEntity,
        notifyIfMissedDigest: Boolean,
    ): Boolean {
        cancel(task.id)
        if (NotificationAccess.shouldSkipWakeup(NotificationAccess.areEnabled(context))) return false
        val now = System.currentTimeMillis()
        if (settings.digestEnabled) {
            val candidate = task.toDigestCandidate()
            if (DigestPlanner.coveredByDigest(
                    candidate = candidate,
                    defaultReminderMinutesOfDay = settings.defaultReminderMinutesOfDay,
                    nowEpochMs = now,
                )
            ) {
                if (notifyIfMissedDigest &&
                    DigestPlanner.sameDayFallback(
                        candidate = candidate,
                        defaultReminderMinutesOfDay = settings.defaultReminderMinutesOfDay,
                        nowEpochMs = now,
                    )
                ) {
                    val today = DigestPlanner.localEpochDay(now)
                    if (!DigestNotifyStore.alreadyPostedFallback(context, today, task.id)) {
                        NotificationHelper.showDueReminder(context, task)
                        DigestNotifyStore.markFallbackPosted(context, today, listOf(task.id))
                    }
                }
                return false
            }
        }
        val fireAt = ReminderTimeCalculator.nextFireEpochMs(
            task,
            settings.defaultReminderMinutesOfDay,
            nowEpochMs = now,
            digestEnabled = settings.digestEnabled,
        ) ?: return false
        setWakeup(fireAt, alarmPendingIntent(task.id))
        return true
    }

    private fun scheduleDigest(
        defaultReminderMinutes: Int,
        nowEpochMs: Long = System.currentTimeMillis(),
        afterFire: Boolean = false,
    ) {
        val fireAt = if (afterFire) {
            DigestPlanner.nextDigestAfterFire(
                defaultReminderMinutesOfDay = defaultReminderMinutes,
                nowEpochMs = nowEpochMs,
            )
        } else {
            DigestPlanner.nextDigestEpochMs(
                defaultReminderMinutesOfDay = defaultReminderMinutes,
                nowEpochMs = nowEpochMs,
            )
        }
        setWakeup(fireAt, digestPendingIntent())
    }

    private fun cancelDigest() {
        alarmManager.cancel(digestPendingIntent())
    }

    private fun setWakeup(fireAt: Long, pi: PendingIntent) {
        if (NotificationAccess.shouldSkipWakeup(NotificationAccess.areEnabled(context))) return
        if (canExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
        }
    }

    private fun canExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

    private fun alarmPendingIntent(taskId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun digestPendingIntent(): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DIGEST
        }
        return PendingIntent.getBroadcast(
            context,
            DIGEST_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_ALARM = "com.errata.app.action.REMINDER_ALARM"
        const val ACTION_DIGEST = "com.errata.app.action.REMINDER_DIGEST"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_SCHEDULED_DUE = "scheduled_due"
        const val DIGEST_REQUEST_CODE = 0x7E11A701
        const val DIGEST_NOTIFICATION_ID = 0x7E11A702

        fun requestCode(taskId: Long): Int = taskId.toInt()
    }
}

private fun TaskEntity.toDigestCandidate() = DigestPlanner.Candidate(
    id = id,
    estimateMinutes = estimateMinutes,
    reminderMinutesOfDay = reminderMinutesOfDay,
    nextDueAtEpochMs = nextDueAtEpochMs,
    snoozedUntilEpochMs = snoozedUntilEpochMs,
    isPaused = isPaused,
    isArchived = isArchived,
    createdAtEpochMs = createdAtEpochMs,
)
