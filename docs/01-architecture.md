# Architecture — Errata (planned)

Living doc. Update when stack decisions land in code.  
Product intent and feature purpose: [`03-product-map.md`](./03-product-map.md).

## Target shape

```
┌──────────────────────────────────────────┐
│  UI (Compose / Material 3)               │
│  Pending (home) · All tasks · Settings   │
│  Free window on pending · Editor         │
│  Home widget (due count + minutes)       │
└─────────────────┬────────────────────────┘
                  │
┌─────────────────▼────────────────────────┐
│  Domain                                  │
│  Cadence modes · Due calculation         │
│  Completion · Snooze · Skip · Pause      │
│  Free-window ranking · optional area     │
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
| Jetpack Compose + M3 | **In tree** | Margin-note shell: bottom nav, light/dark, punch-hole-safe insets |
| Room | **In tree** | Schema v4: tasks, completions, settings (`appearanceMode`, `digestEnabled`) |
| `minSdk` | **26** | Android 8+ — older tablets without cutting off Compose |
| `compileSdk` / `targetSdk` | **35** | |
| Application id | `com.errata.app` | |
| DI framework | Avoid until needed | `ErrataApp` holds DB + repository |
| Internet permission | **Absent** by default | |
| Exact alarms | Opt-in per UX | API 31+ special access; inexact fallback; do not demand |

## Schema v4 (Room)

- **tasks** — title, notes, estimateMinutes, intervalDays, scheduleKind (`INTERVAL` / `WEEKLY` / `MONTHLY`), weekdaysMask (bit 0 = Monday … bit 6 = Sunday), monthDay (1–31 or 0), cadenceMode, anchorEpochDay, nextDueAtEpochMs, lastCompletedAtEpochMs, reminderMinutesOfDay, snoozedUntilEpochMs, area, isPaused, isArchived, timestamps  
- **completions** — taskId, completedAt, scheduledDueAt (for catch-up audit), estimate snapshot  
- **settings** (singleton row) — defaultCadenceMode, defaultReminderMinutesOfDay, defaultWorkStartMinutesOfDay, soonHorizonDays, appearanceMode (`SYSTEM` / `LIGHT` / `DARK`), digestEnabled (default off)  

Existing installs migrate 3→4 with `INTERVAL` / mask `0` / monthDay `0`. Weekly/monthly rows keep a dummy `intervalDays` of 7.  

**UI shell:** bottom bar Pending · All tasks · Settings. Backup lives under Settings. Scaffolds use `WindowInsets.safeDrawing` so punch-holes don’t cover copy. Visual identity: paper/ink/terracotta (bundled Fraunces + Atkinson Hyperlegible).  

**Time:** epoch millis UTC. **Due:** local datetime. **Pending buckets:** by local calendar day of effective due. **After Done:** next due keeps the previous due’s local time-of-day (`CadenceCalculator.atLocalDateKeepingTime`).

## Cadence model

- **Schedule kind** (orthogonal to after-Done mode):  
  - `INTERVAL` — every N days (default; existing tasks). After-Done modes apply.  
  - `WEEKLY` — one or more weekdays. Next due is the next selected local calendar day **strictly after** Done or Skip, same clock time as the due that was open. After-Done mode is ignored.  
  - `MONTHLY` — day of month 1–31; missing days (31 in February) clamp to the last day of that month. Same “next slot after Done/Skip, keep time.” After-Done mode is ignored.  
- **nth-weekday** (“first Saturday”) and seasonal anchors are still later.  
- **Modes** (stored per task; global default in Settings; apply to interval tasks only):  
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
- **Area:** optional label (Bathroom / Body / Car / House / Paper, or a short custom string). Filter chips on pending and All tasks when any listed row has an area; quiet label on the card. Urgency sections stay primary.  

## History glance

- Editor (existing tasks with at least one Done): last completed date + typical lateness  
- Typical = median calendar-day delta vs scheduled due, last **8** completions, shown only with **3+** samples. Skip is not a completion. No streaks or charts.  

## All tasks library

- Bottom nav tab: every non-archived task, including not-soon after Done  
- Optional area filter when any library row has an area; same quiet card label as pending  
- Row → editor; Pause / Resume; Archive (confirm) hides from library; no archived browser yet  
- Done / Snooze stay on pending only  
- **Empty state:** in-app starter pack (`StarterCatalog`) when there are zero active tasks. User checks what they actually do and pins; rows become normal tasks. First due is the next slot (interval = tomorrow at the default reminder time; weekly/monthly = next matching local day). Not shown once any task is pinned. Pending “nothing due” with existing later tasks stays the caught-up copy. No download, no new permission.  

## Home widget

- Optional launcher pin: overdue + due-today **count** and **total minutes** (`N due · ~M min`, or “Nothing due”). Soon is excluded.  
- Tap opens pending. No Done/Snooze on the widget.  
- Updates on task writes and boot/`rescheduleAll`. `updatePeriodMillis = 0`. One inexact local-midnight `RTC_WAKEUP` while at least one instance is pinned, cancelled when the last is removed.  

## Settings

- Bottom nav tab: appearance (system/light/dark), default reminder, opt-in morning digest, on-time reminders (API 31+), cadence, work-start  
- Backup is a Settings row (SAF export/import + optional folder), not a pending overflow item  
- Privacy is a Settings row (offline; same facts as [`05-privacy.md`](./05-privacy.md))  
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
- **Opt-in digest:** Settings toggle (off by default). One standing alarm at the default reminder time. Coalesces overdue + due-today tasks that use the default time. Custom reminder times and future snoozes stay per-task. At fire: N=0 silent; N=1 existing per-task card (Done/Snooze); N≥2 one notification (count + total minutes, tap pending). Cadence math unchanged if a digest is missed.  
- Notification: title + duration hint; actions Done / Snooze (notification snooze = 1 hour)  
- In-app snooze: 1h / later today / tomorrow / pick clock time (past → tomorrow)  
- Exact when `canScheduleExactAlarms()`; otherwise `setAndAllowWhileIdle`  
- **Exact-alarm UX (API 31+):** optional special access — one-shot explain on first task save if denied; Settings always has status + system settings path. Never required. Grant/revoke broadcasts (and return from the system screen) call `rescheduleAll` so alarms upgrade or fall back. Do not use `USE_EXACT_ALARM`.  
- Missing a reminder must not corrupt cadence (snooze vs skip vs pause — product map)  

## Distribution

- Debug/release APK for sideload — see [`04-sideload.md`](./04-sideload.md)  
- Privacy policy: [`05-privacy.md`](./05-privacy.md) (in-app Settings → Privacy; host the doc when submitting to Play)  
- Play listing / Data safety draft: [`06-play.md`](./06-play.md). Console upload is still later. Name **Errata** (not UpKeep).  
- **Android Auto Backup is off** (`allowBackup=false`; cloud and device-to-device extraction excluded). Move path is user Backup export/folder.  

## Backup (export / import)

- **In tree:** JSON `schemaVersion` 1 via kotlinx.serialization; SAF CreateDocument / OpenDocument  
- Contents: settings, all tasks (incl. paused/archived), completions  
- **Import:** replace-all in a Room transaction after user confirm; then `rescheduleAll`  
- **Optional folder:** user picks a tree (`OpenDocumentTree`); persistable URI on this device only (SharedPreferences, not in the JSON). Writes/reads `errata-backup.json` on demand. Last write wins; no merge, no folder watch, no `WorkManager`.  
- **Drive / other clouds:** Export or Import and pick the provider in the system sheet when the OEM offers it. Tree pickers often omit Drive. No `INTERNET`, no Google account, no Drive SDK.  
- No INTERNET; no account  

## Battery budget

**Reminders:** Each task gets at most one `AlarmManager` RTC wakeup at a time (due-day reminder time, or snooze instant, or next-day nudge while still open), except when **morning digest** is on: then there is one standing wakeup at the default reminder time covering default-time overdue/due-today tasks, plus per-task alarms only for custom times and future snoozes. **Widget:** while pinned, one additional inexact midnight wakeup so “due today” stays honest; no periodic `WorkManager`. No foreground service, no wake lock held across UI. Boot and cold start only reschedule alarms (and refresh the widget), then release. Justification: user-expected due reminders without continuous background work.
