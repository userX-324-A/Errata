# Architecture — Errata (planned)

Living doc. Update when stack decisions land in code.  
Product intent and feature purpose: [`03-product-map.md`](./03-product-map.md).

## Target shape

```
┌──────────────────────────────────────────┐
│  UI (Compose / Material 3)               │
│  Pending (home) · All tasks (secondary)  │
│  Settings · Free window · Editor         │
└─────────────────┬────────────────────────┘
                  │
┌─────────────────▼────────────────────────┐
│  Domain                                  │
│  Cadence modes · Due calculation         │
│  Completion · Snooze · Skip · Pause      │
│  Free-window ranking                     │
└─────────────────┬────────────────────────┘
                  │
┌─────────────────▼────────────────────────┐
│  Data (Room)                             │
│  Tasks · Completions · Settings          │
└─────────────────┬────────────────────────┘
                  │
┌─────────────────▼────────────────────────┐
│  Reminders                               │
│  Boot reschedule · Notifications         │
│  Done / Snooze actions · optional digest │
└──────────────────────────────────────────┘
```

## Stack decisions

| Choice | Status | Notes |
|---|---|---|
| Kotlin | **In tree** | |
| Jetpack Compose + M3 | **In tree** | Pending + All tasks + Settings + editor + Done/Snooze |
| Room | **In tree** | Schema v1: tasks, completions, settings; `TaskRepository` |
| `minSdk` | **26** | Android 8+ — older tablets without cutting off Compose |
| `compileSdk` / `targetSdk` | **35** | |
| Application id | `com.errata.app` | |
| DI framework | Avoid until needed | `ErrataApp` holds DB + repository |
| Internet permission | **Absent** by default | |
| Exact alarms | Opt-in per UX | Document permission flows |

## Schema v1 (Room)

- **tasks** — title, notes, estimateMinutes, intervalDays, cadenceMode, anchorEpochDay, nextDueAtEpochMs, lastCompletedAtEpochMs, reminderMinutesOfDay, snoozedUntilEpochMs, area, isPaused, isArchived, timestamps  
- **completions** — taskId, completedAt, scheduledDueAt (for catch-up audit), estimate snapshot  
- **settings** (singleton row) — defaultCadenceMode, defaultReminderMinutesOfDay, defaultWorkStartMinutesOfDay, soonHorizonDays  

**Time:** epoch millis UTC. **Due:** local datetime. **Pending buckets:** by local calendar day of effective due. **After Done:** next due keeps the previous due’s local time-of-day (`CadenceCalculator.atLocalDateKeepingTime`).

## Cadence model

- **Interval:** every N days (richer weekly/monthly later — Tier 3)  
- **Modes** (stored per task; global default in Settings):  
  - `FROM_COMPLETION`  
  - `FIXED_ANCHOR`  
  - `FROM_COMPLETION_CATCH_UP` (**default**)  
- **Implementation:** `domain.cadence.CadenceCalculator` (unit-tested)  
- **Catch-up formula:** see product map (overdue threshold 50% of interval; compress up to 25% of interval; floor `max(1 day, 50% interval)`)  
- **Pending buckets:** `domain.due.PendingClassifier` — overdue / due today / soon / later / hidden (paused, archived); snooze uses `max(nextDue, snoozedUntil)`

## Pending queue

- Sections: **overdue** → **due today** → **soon**  
- **Soon** = `nextDueAt` within **7 days**, excluding due today  
- Row copy: plain language due + `~estimateMinutes`  
- After in-app Done: optional duration honesty (shorter / about right / longer) adjusts `estimateMinutes`; notification Done skips the prompt  
- **Skip:** confirm-gated; advances `nextDue` via `CadenceCalculator.nextDueAfterSkip` with no completion row and no `lastCompletedAt` change; clears snooze; reschedules reminder  

## All tasks library

- Secondary surface (overflow → All tasks): every non-archived task, including not-soon after Done  
- Row → editor; Pause / Resume; Archive (confirm) hides from library; no archived browser yet  
- Done / Snooze stay on pending only  

## Settings

- Overflow → Settings: edit globals already on the settings row  
- Default reminder time (rescheduleAll when changed), default cadence mode (new tasks only), optional work-start for future free-window  
- Autosave; local-only footer  

## Free-window (domain)

- **In tree:** chips on pending home (15 / 30 / 45 / until work / custom minutes or stop-by)  
- Inputs: available minutes (and/or stop-by → minutes until T today)  
- Candidates: overdue + due today + soon  
- Rank: urgency band, then largest estimate that fits; leftover after best; show-all clears filter  
- Optional Settings work-start powers “until work” when still ahead today  
- Not a calendar sync product  

## Reminder policy

- **In tree:** `AlarmManager` one-shot per active task; `BootReceiver` + launch `rescheduleAll`  
- Global default time-of-day + optional per-task override (editor chips)  
- **Default:** per-task fires — not digest  
- **Opt-in digest** (Tier 2): one morning notification when many share a window  
- Notification: title + duration hint; actions Done / Snooze (notification snooze = 1 hour)  
- In-app snooze: 1h / later today / tomorrow / pick clock time (past → tomorrow)  
- Exact when `canScheduleExactAlarms()`; otherwise `setAndAllowWhileIdle`  
- Missing a reminder must not corrupt cadence (snooze vs skip vs pause — product map)  

## Distribution

- Debug/release APK for sideload — see [`04-sideload.md`](./04-sideload.md)  
- Play Store optional later; name **Errata** appears clearer than UpKeep for store collision  

## Backup (export / import)

- **In tree:** JSON `schemaVersion` 1 via kotlinx.serialization; SAF CreateDocument / OpenDocument  
- Contents: settings, all tasks (incl. paused/archived), completions  
- **Import:** replace-all in a Room transaction after user confirm; then `rescheduleAll`  
- No INTERNET; no account  

## Battery budget

**Reminders (v1):** Each task gets at most one `AlarmManager` RTC wakeup at a time (due-day reminder time, or snooze instant, or next-day nudge while still open). No foreground service, no periodic `WorkManager`, no wake lock held across UI. Boot and cold start only reschedule alarms, then release. Justification: user-expected due reminders without continuous background work.
