# Architecture — Errata (planned)

Living doc. Update when stack decisions land in code.  
Product intent and feature purpose: [`03-product-map.md`](./03-product-map.md).

## Target shape

```
┌──────────────────────────────────────────┐
│  UI (Compose / Material 3)               │
│  Pending (home) · All tasks · Settings   │
│  Free window on pending · Editor         │
│  Home widget (due count, minutes, titles)│
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
| Jetpack Compose + M3 | **In tree** | Adaptive shell: `NavigationSuiteScaffold` (bottom bar on compact, nav rail on medium/expanded). Pending, library, and catalog+editor use `NavigableListDetailPaneScaffold` (compact push, **medium and expanded** two-pane so the list stays beside the editor when the rail is showing). Settings opens Backup/Privacy in the detail pane. Punch-hole-safe insets. Single-pane forms still cap ~720dp; the editor uses two columns when the pane is ≥480dp. |
| Room | **In tree** | Schema v9: `defaultReminderKind` on settings; tasks `reminderMinutesOfDay` `-1` = none (null still When due) |
| `minSdk` | **26** | Android 8+ — older tablets without cutting off Compose |
| `compileSdk` / `targetSdk` | **35** | |
| Application id | `com.errata.app` | |
| DI framework | Avoid until needed | `ErrataApp` holds DB + repository |
| Internet permission | **Present** | Used only for opt-in Google Drive app data |
| Exact alarms | Opt-in per UX | API 31+ special access; inexact fallback; do not demand |

## Schema v9 (Room)

- **tasks** — uuid (sync identity; local Long id stays the alarm/nav key), title, notes, estimateMinutes, intervalDays, scheduleKind (`INTERVAL` / `WEEKLY` / `MONTHLY` / `NTH_WEEKDAY` / `YEARLY`), weekdaysMask (bit 0 = Monday … bit 6 = Sunday), monthDay (1–31 or 0), weekdayOrdinal (1–4 or 5 = last when nth-weekday; else 0), yearMonthsMask (bit 0 = January … bit 11 = December), seasonMask (bit 0 = Spring … bit 3 = Winter), cadenceMode, anchorEpochDay, nextDueAtEpochMs, lastCompletedAtEpochMs, reminderMinutesOfDay (`null` = When due, `-1` = none, `0`–`1439` = clock), snoozedUntilEpochMs, area, isPaused, isArchived, timestamps  
- **completions** — uuid, taskId, completedAt, scheduledDueAt (for catch-up audit), estimate snapshot  
- **settings** (singleton row) — defaultCadenceMode, defaultReminderKind (`NONE` / `WHEN_DUE` / `CLOCK`; new tasks only), defaultReminderMinutesOfDay, defaultWorkStartMinutesOfDay, soonHorizonDays, appearanceMode (`SYSTEM` / `LIGHT` / `DARK`), digestEnabled (default off), historyRetentionDays (0 = keep all; default **730**), updatedAtEpochMs (shared settings LWW), historyGeneration / historyPurgedAtEpochMs, tasksGeneration / tasksResetAtEpochMs  

Existing installs migrate 8→9 with `defaultReminderKind = WHEN_DUE`. Calendar-grid rows keep a dummy `intervalDays` of 7.  

**UI shell:** `NavigationSuiteScaffold` — Pending · All tasks · Settings. Compact uses a bottom bar; medium/expanded use a navigation rail. List+editor (and catalog, Backup, Privacy) share a list-detail pane: compact still pushes a full screen; **medium and expanded** keep the list visible. Backup is a Settings row. Scaffolds use `WindowInsets.safeDrawing` so punch-holes don’t cover copy. Visual identity: paper/ink/terracotta (bundled Fraunces + Atkinson Hyperlegible).  

**Time:** epoch millis UTC. **Due:** local datetime. **Pending buckets:** by local calendar day of effective due. **After Done:** next due keeps the previous due’s local time-of-day (`CadenceCalculator.atLocalDateKeepingTime`).

## Cadence model

- **Schedule kind** (orthogonal to after-Done mode):  
  - `INTERVAL` — every N days (default; existing tasks). After-Done modes apply.  
  - `WEEKLY` — one or more weekdays. Next due is the next selected local calendar day **strictly after both** Done/Skip and the open due day (early Done or Skip consumes that slot), same clock time as the due that was open. After-Done mode is ignored.  
  - `MONTHLY` — day of month 1–31; missing days (31 in February) clamp to the last day of that month. Same consume + keep-time rule. After-Done mode is ignored.  
  - `NTH_WEEKDAY` — 1st / 2nd / 3rd / 4th / last of **one** weekday each month (`weekdaysMask` exactly one bit; `weekdayOrdinal` 1–4 or 5 = last). Same consume + keep-time grid. After-Done mode is ignored.  
  - `YEARLY` — union of selected calendar months (shared `monthDay` 1–31, clamp missing days) and northern **civil** season starts (Spring 20 Mar, Summer 21 Jun, Autumn 22 Sep, Winter 21 Dec). `yearMonthsMask` / `seasonMask`. Same consume + keep-time grid. After-Done mode is ignored.  
- **Astronomy / southern-hemisphere setting** are still out; southern users pick months.  
- **Modes** (stored per task; global default in Settings; apply to interval tasks only):  
  - `FROM_COMPLETION`  
  - `FIXED_ANCHOR`  
  - `FROM_COMPLETION_CATCH_UP` (**default**)  
- **Implementation:** `domain.cadence.CadenceCalculator` (unit-tested)  
- **Catch-up formula:** see product map (overdue threshold 50% of interval; compress up to 25% of interval; floor `max(1 day, 50% interval)`). After keep-time snap, next due is not before `completedAt + floor` as a duration (bump a local day if the snapped clock is still early). Do not ceil half-days into an extra calendar day.  
- **Pending buckets:** `domain.due.PendingClassifier` — overdue / due today / soon / later / hidden (paused, archived); snooze uses `max(nextDue, snoozedUntil)`. Home `nowTick` refreshes on resume and once a minute while Pending is STARTED (no wakeup).

## Pending queue

- Sections: **overdue** → **due today** → **soon**  
- **Soon** = `nextDueAt` within **7 days**, excluding due today  
- Row copy: plain language due + `~estimateMinutes`. Clock labels use `DateFormat.getTimeFormat` so they match TimePicker 12/24.  
- After in-app Done: optional duration honesty (shorter / about right / longer) adjusts `estimateMinutes` when the completion applied and the estimate is at least 10 minutes; shorter chores and notification Done skip the prompt  
- **Skip:** confirm-gated and the same expected-due one-shot as Done; advances `nextDue` via `CadenceCalculator.nextDueAfterSkip` with no completion row and no `lastCompletedAt` change; clears snooze; reschedules reminder  
- **Pause / resume:** clear snooze so an old delay cannot hide the task after Resume. Editing due day, due clock, interval, mode, or schedule kind also clears snooze and (for those grid fields) retargets `anchorEpochDay` to the saved due day so FIXED_ANCHOR does not snap back to the old grid.  
- **Area:** optional label (Bathroom / Body / Car / House / Paper / Kitchen / Clothes, or a short custom string). Filter chips on pending and All tasks when any listed row has an area; quiet label on the card. Urgency sections stay primary.  

## History glance

- Editor (existing tasks with at least one Done): last completed date + typical lateness  
- Typical = median calendar-day delta vs scheduled due, last **8** completions, shown only with **3+** samples. Skip is not a completion. No streaks or charts.  
- **Retention:** Settings 90 days / 1 year / 2 years (default) / keep all. Always keep the last 8 Dones per task. SQL prune on cold start, when the retention setting changes, and at most once per local day after Done (`KEEP_ALL` skips). Import does not prune restored history. Purge history deletes completion rows only. Reset tasks deletes tasks + completions, keeps settings. Linked reset with **Also clear the Google copy** also bumps `historyGeneration` so merge does not restore Dones.  

## All tasks library

- Bottom nav tab: every non-archived task, including not-soon after Done  
- Optional area filter when any library row has an area; same quiet card label as pending  
- Row → editor; Pause (confirm) / Resume; Archive (confirm) hides from library; no archived browser yet  
- Done / Snooze stay on pending only  
- **Empty state:** in-app starter pack (`StarterCatalog`) when there are zero active tasks. User checks what they actually do and pins; rows become normal tasks. First due is the next slot (interval = tomorrow at the default reminder time; weekly/monthly/nth-weekday/yearly = **today** if that local clock is still ahead, otherwise the next matching day). **Blank task** uses the same clock rule (today if the default due clock is still ahead, otherwise tomorrow) so an afternoon create is not Overdue. **Add task** (pending/library FAB, and empty-state Add task) opens the same catalog grouped by area; **Blank task** is a scratch editor (minutes required, no placeholder 15; 10/15/20/30/45 chips). Tapping a starter (empty-state row or catalog) pre-fills the editor so minutes can change before pin; checkboxes still multi-pin as-is. Save returns to the origin list (catalog under the editor is dropped), including Back / Not now during the first-pin notify or exact prompt; Back before Save still shows the catalog. Same starter may be pinned more than once. Weekly catalog rows say **change the day**. Get a haircut / Oil change use a shop-block estimate (not a 10-minute reminder); oil default interval is 180 days. Pack holiday lights is 1 January, not winter solstice. Multi-check Pin selected stays on the true empty state only. Pending “nothing due” with existing later tasks stays the caught-up copy; the Add FAB stays whenever there are pins. No download, no new permission.  

## Home widget

- Optional launcher pin: overdue + due-today **count** and **total minutes** (`N due · ~M min`, or “Nothing due”). Soon is excluded. Default size is **4×2** on API 31+ (`targetCell*`); older launchers use **3×2** (`minWidth`/`minHeight`). Android 12+ can shrink to a 2×1 strip (`minResize*`).  
- When the tile is two rows or taller (~100dp+), show up to four titles (effective due, then name) and “+N more” if needed. Compact 2×1 stays count + minutes. Tap opens pending. No Done/Snooze on the widget. Already-pinned instances keep their old size until resized or re-pinned.  
- Updates on task writes, resize, and boot/`rescheduleAll`. `rescheduleAll` owns that widget pass — pin-many / import / reset / sync / digest-settings do not refresh again after it. `updatePeriodMillis = 0`. One inexact local-midnight `RTC_WAKEUP` while at least one instance is pinned, cancelled when the last is removed. Midnight uses an **explicit** `PendingIntent` (custom action is not in the exported intent-filter).  

## Settings

- Bottom nav tab: appearance (system/light/dark), default reminder, opt-in morning digest, notifications, on-time reminders (API 31+), cadence, work-start, optional Google link, history retention (90 / 1y / 2y / keep all), confirm-gated purge history and reset tasks  
- Backup is a Settings row (SAF export/import + optional folder), not a pending overflow item  
- Privacy is a Settings row (offline; same topics as [`05-privacy.md`](./05-privacy.md), shorter copy)  
- Autosave; local-only footer  

## Free-window (domain)

- **In tree:** chips on pending home (15 / 30 / 45 / until work / custom minutes or stop-by)
- Inputs: available minutes, or a **clock target** (until work / stop-by). Clock windows recompute remaining minutes on each Pending tick (resume + once a minute). Fixed 15/30/45/custom minutes stay that pocket.
- Candidates: overdue + due today + soon
- Rank: urgency band, then largest estimate that fits (each ≤ window); leftover is minutes left after that best pick (not a packed set); show-all clears filter. Until-work / stop-by after that clock: “that clock has passed,” not “0 min.”
- Optional Settings work-start powers “until work” when still ahead today (chip stays selected if you keep it after that clock)
- Not a calendar sync product

## Reminder policy

- **In tree:** `AlarmManager` one-shot per active task; `BootReceiver` + process-start `rescheduleAll`. Also reschedules on `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`, and common OEM quick-boot. Per-task alarm ids are tracked in device prefs and updated on `rescheduleTask` (create / done / snooze / skip / pause / archive). `rescheduleAll` cancels previous ∪ this-process ids that are no longer schedulable (import / reset / sync prune). Not direct-boot aware. Activity rotation does not rewrite alarms.  
- Global default **kind** (`NONE` / `WHEN_DUE` / `CLOCK`) seeds **new** pins and starters only — it does not retarget existing tasks. Global default **clock** still seeds new due times, CLOCK reminders, and the optional morning digest. Per-task `reminderMinutesOfDay`: `null` = fire at the **due clock**; `-1` = **none** (no `AlarmManager`, including after in-app snooze — quiet stays quiet; snooze still delays the pending bucket); `0`–`1439` = a clock. Editor Back confirms if the form is dirty.  
- **Default:** per-task fires — not digest. None never wakes the device.  
- **Overdue cap:** per-task wakeup on the due day, plus one extra local day if digest is off. After that, no RTC (the task stays on pending). Digest on: overdue custom / non-default clocks join the standing digest instead of a daily per-task alarm. A future snooze still wakes once.  
- **Opt-in digest:** Settings toggle (off by default). One standing alarm at the default reminder time. Coalesces overdue + due-today tasks whose **effective** fire minutes equal that default (due clock when reminder is null, else the override). None never joins. A 2pm-due task on “When due” stays off a 9am digest **on the due day**, then joins while overdue so leftover custom clocks do not each RTC every morning. Future snoozes stay per-task. At fire: N=0 silent; N=1 existing per-task card (Done/Snooze); N≥2 one notification (count + total minutes, tap pending). After that window, a task pinned the same day still gets a same-day notification **once** (device prefs per task id — Settings resume / clock change do not re-post). Import replace after that window uses an appeared stamp so restored due-today members are not silent just because backup `createdAt` is old. Changing the default clock after today’s digest has posted schedules tomorrow, not a second fire today. Tasks already in the digest do not get a second. If the standing alarm never ran (boot / force-stop / import after the window), `rescheduleAll` posts that digest **once** for the local day (device prefs; not synced). The standing RTC and that miss-replay share the same local-day mark — `onDigestFired` does not post again if the mark is already today (dead process + retained alarm). Cadence math unchanged if a digest is missed. Clock/timezone broadcasts debounce 2s so OEM chatter does not rewrite every alarm twice.  
- Notification: title + duration hint; actions Done / Snooze (notification snooze = 1 hour). Shade Done **and** Snooze are one-shot (in-flight guard + expected due); paused/archived refuse both. In-app Done is the same (shared in-flight + expected due; honesty only if the completion applied and the estimate is at least 10 minutes). In-app Snooze uses the due captured when the sheet opens so a leftover shade Done cannot attach a wakeup to the next cycle. In-app Done / Snooze / Skip / Pause / Archive dismiss the posted card, cancel shade actions, **and dismiss the digest snapshot** so leftover count/minutes do not lie.  
- In-app snooze: 1h / later today / **tomorrow at the task’s reminder clock** (due clock when When due or None) / pick clock time (past → tomorrow). Tomorrow is not local midnight.  
- Exact when `canScheduleExactAlarms()`; otherwise `setAndAllowWhileIdle`  
- **Notifications off:** no `RTC_WAKEUP` while the app cannot post. Do not ask for `POST_NOTIFICATIONS` on cold start. First reminder-bearing pin explains, then the system dialog (API 33+). Pending shows a quiet banner if they are still off; Settings has status + system settings path. Grant/return from that screen, and Settings resume when notify/exact flags **changed** (seeded at construction — not a duplicate `rescheduleAll` on first Settings visit this process), call `rescheduleAll`.  
- **Exact-alarm UX (API 31+):** optional special access — one-shot explain on first task save if denied; Settings always has status + system settings path. Never required. Grant/revoke broadcasts (and return from the system screen) call `rescheduleAll` so alarms upgrade or fall back. Do not use `USE_EXACT_ALARM`. The prompt keeps the editor; Save is one-shot (in-flight + `saved`) and adopts the inserted row id so a second tap cannot duplicate. Back or Not now during those prompts still `popToList` (catalog under the editor is dropped). Dirty Back on a new task says it will not be created; on an existing task, unsaved edits are dropped.  
- Missing a reminder must not corrupt cadence (snooze vs skip vs pause — product map)  

## Distribution

- Debug/release APK for sideload — see [`04-sideload.md`](./04-sideload.md). Release uses R8 minify + resource shrink; Material icons-core only.  
- Privacy policy: [`05-privacy.md`](./05-privacy.md) (in-app Settings → Privacy; host the doc when submitting to Play)  
- Play listing / Data safety: [`06-play.md`](./06-play.md). Human upload steps: [`08-publish.md`](./08-publish.md). Publisher **Ordinary Tools**. Name **Errata** (not UpKeep).  
- **Android Auto Backup is off** (`allowBackup=false`; cloud and device-to-device extraction excluded). Move path is user Backup export/folder.  

## Backup (export / import)

- **In tree:** JSON `schemaVersion` 2 via kotlinx.serialization; SAF CreateDocument / OpenDocument. v1 files import with generated UUIDs and a confirm warning (prefer a current export before linking Google on two devices). Missing `defaultReminderKind` is `WHEN_DUE`; task `reminderMinutesOfDay` `-1` is none.  
- Contents: settings, all tasks (incl. paused/archived), completions  
- **Import:** replace-all in a Room transaction after user confirm; then `rescheduleAll` (replays a missed digest if today’s window has passed; if today’s digest already posted, restored due-today members get one same-day fallback even when `createdAt` is old). Does not prune restored history (retention still applies on later Dones). If Google is linked, import bumps `tasksGeneration` / `historyGeneration` and syncs now so the Drive copy follows this device (cloud-only tasks and older history from before the import are dropped on merge). If not linked, keep the file’s generations so a later Link merges with Drive instead of wiping it.  
- **Optional folder:** user picks a tree (`OpenDocumentTree`); persistable URI on this device only (SharedPreferences, not in the JSON). Writes/reads `errata-backup.json` on demand. Last write wins; no merge, no folder watch.  
- **Drive / other clouds (picker):** Export or Import and pick the provider in the system sheet when the OEM offers it. Tree pickers often omit Drive.  
- **Optional Google Drive App Data:** Settings → Link Google. Credential Manager + `drive.appdata` only. Hidden `errata-sync.json` in appDataFolder (list pages until `nextPageToken` is empty, then coalesces duplicates to the newest; a first upload with no cached id lists and patches if a file already exists; patch never skips If-Match). Merge: tasks by uuid (`updatedAt` wins); completions union; shared settings newer `updatedAt` (not appearance); purge/reset use generations. WorkManager: one unique one-shot name (45s debounce after writes, or Sync now / process start replaces that delay; Activity rotation does not), plus a 24h CONNECTED catch-up. Debounce and process-start skip while last error is auth or unreadable JSON; Link and Sync now still run. Access token cache TTL ~50 min; 401/403 clears cache and retries once. Persistent auth **or unreadable Drive JSON** (including a blank body) cancels WM until Link / Sync now (corrupt is not overwritten). Coordinator mutex; skip apply if local moved during the round. If-Match etag; retry on 412 (cap 3). No FGS. Unlink keeps local; wipe lists every `errata-sync.json` (all pages) and must delete all of them before unlink (partial failure stays linked). Pin starters debounce a sync like other writes. Play services required. Human OAuth: [`07-google-sync.md`](./07-google-sync.md).  
- `INTERNET` is in the APK for this opt-in path. No Errata server.  

## Battery budget

**Reminders:** Each task with a reminder gets at most one `AlarmManager` RTC wakeup at a time (due-clock or override reminder time, or snooze instant). Per-task fires on the due day; if digest is off, one extra local day after due is allowed, then no further wakeup (the task stays on pending). **None** sets no wakeup. When **morning digest** is on, there is one standing wakeup at the default reminder time covering overdue/due-today tasks whose effective fire minutes equal that default, plus overdue custom clocks (due-day custom clocks stay per-task). Future snoozes stay per-task. Tasks pinned after that window get a same-day notification without a second wakeup. Import after that window does the same for restored due-today members. If the digest alarm never ran, the next `rescheduleAll` after the window posts it once (local-day mark, no extra wakeup). Alarms are not set while notifications are disabled. Clock and timezone changes (and sideload/Play replace) rebuild those wakeups from current local wall time. Pending `nowTick` reclassifies the active set on resume and once a minute while home is STARTED (no wakeup; fine for a personal pin count). **Widget:** while pinned, one additional inexact midnight wakeup so “due today” stays honest. **Google sync (linked only):** one unique WorkManager one-shot (`errata-sync`), 45s debounce after writes or immediate on Sync now / **process start** (skipped while last error is auth or corrupt), plus a 24h CONNECTED catch-up; serialized in-process. No foreground service, no wake lock held across UI. Process start and boot reschedule alarms (and refresh the widget), then release; rotation does not rewrite alarms or enqueue a Drive sync. If linked and not in a sticky auth/corrupt error, process start enqueues one sync. Justification: user-expected due reminders without continuous background work; opt-in Drive merge without polling.

**OEM / Doze caveats:** Exact alarms fall back to `setAndAllowWhileIdle` when denied. Force-stop kills alarms until next open or boot. Some OEM “quick boot” paths are covered by extra receiver actions; if reminders stay silent after reboot, open the app once. Inexact fires can be minutes late under Doze.
