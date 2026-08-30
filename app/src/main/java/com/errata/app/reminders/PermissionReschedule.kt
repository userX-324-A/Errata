package com.errata.app.reminders

/**
 * Settings resume should not rebuild alarms on the first observation this
 * process — [com.errata.app.ErrataApp] already called rescheduleAll.
 * Later visits reschedule only when notify or exact-alarm access changed.
 */
object PermissionReschedule {
    fun shouldRun(
        previous: Pair<Boolean, Boolean>,
        next: Pair<Boolean, Boolean>,
    ): Boolean = previous != next
}
