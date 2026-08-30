# Product map — Errata

Authority for **what** we build and **why**. Roadmap order lives in [`02-roadmap.md`](./02-roadmap.md). Architecture mechanics live in [`01-architecture.md`](./01-architecture.md).

**Rule:** every feature must answer who it helps, when, and what pain it removes. If it doesn’t earn a row here, it doesn’t ship.

## Locked product axes

1. **Cadence default:** from last completion, with **light catch-up** when badly overdue. Users can override **per task** and set a **global default** (from-completion / fixed-anchor / from-completion + catch-up). Late completion is normal.
2. **Morning job:** **both first-class** — home is the pending due queue; a **free-window** (“I have N minutes” / “must stop by T”) recommends what fits. Not a buried filter.

## How to read tiers

| Tier | Meaning |
|---|---|
| **0** | Trust the list — MVP daily driver |
| **1** | Fit real life — free-window + lifecycle without shame |
| **2** | Calm power — less friction, still personal |
| **3** | Earn it — core in tree; leftovers in Later |

---

## Tier 0 — Trust the list

### Task pin + editor

| | |
|---|---|
| **Helps** | Anyone adding a recurring upkeep item |
| **When** | First-run and whenever life changes |
| **Pain removed** | “I know I should do this every N days but nowhere captures duration + cadence together” |
| **Does** | Create/edit title, cadence, estimate minutes, optional notes, reminder preference |
| **Does not** | Require projects, tags, or assignees |

### Cadence engine

| | |
|---|---|
| **Helps** | People whose chores aren’t daily habits |
| **When** | On create and after every Done |
| **Pain removed** | Habit apps force daily; calendars ignore “from when I last did it” |
| **Does** | Interval every N days; weekly weekday sets; monthly day-of-month (clamp missing days); weekday of month (1st–4th or last of one weekday); yearly months and/or northern civil season starts. Interval after-Done modes: **from-completion**, **fixed-anchor**, **from-completion + catch-up** (global default + per-task override). Calendar kinds ignore those modes and stay on the grid. |
| **Does not** | Punish missed intervals; invent streaks |

**Default global mode:** from-completion + catch-up (see [Cadence policy](#cadence-policy) below).

### Due calculation + pending queue

| | |
|---|---|
| **Helps** | Tired-morning you |
| **When** | Opening the app |
| **Pain removed** | Wall of todos with no “what’s waiting” signal |
| **Does** | Home lists **overdue → due today → soon**; each row: plain due language + `~N min` |
| **Does not** | Charts, heatmaps, or calendar-first shell |

**Soon horizon:** tasks with `nextDueAt` within the next **7 days** (not yet due today). Tunable later in Settings if needed.

### Done

| | |
|---|---|
| **Helps** | Closing the loop |
| **When** | In-app or from notification |
| **Pain removed** | Completing late feels like “breaking a streak” elsewhere |
| **Does** | Log completion; recompute `nextDueAt` from the task’s cadence mode. In-app and shade Done are one-shot (in-flight + expected due) so a double tap does not skip a cycle. |
| **Does not** | Score, badge, or scold |

### Snooze

| | |
|---|---|
| **Helps** | When now isn’t possible but the task isn’t done |
| **When** | List or notification |
| **Pain removed** | Binary done-or-ignore; reminders that nag forever |
| **Does** | Delay due/reminder without completing. Presets: **1 hour**, **later today**, **tomorrow** (next day at the task’s reminder or due clock, not midnight), **pick time**. Pause, resume, and due/grid edits clear it. |
| **Does not** | Count as completion or skip-the-cycle |

### Reminders

| | |
|---|---|
| **Helps** | Remembering without opening the app |
| **When** | Chosen time-of-day |
| **Pain removed** | Silent lists; or battery-killing always-on services |
| **Does** | Per-task **None** (pending only, no alarm), **When due** (`reminderMinutesOfDay` null), or a clock (`0`–`1439`). Settings default kind (none / when due / clock) and due clock seed **new** pins and starters — they do not retarget existing tasks. Digest ignores None. Per-task wakeup on the due day (and one extra day if digest is off); overdue custom clocks join the digest instead of a daily RTC. Notification shows title + duration; actions **Done** / **Snooze** (same expected-due guard; leftover shade is dismissed after in-app Done/Skip/Snooze/Pause/Archive); reschedule after boot |
| **Does not** | Sticky foreground service; high-frequency polling |

**Default fire model:** **per-task** at the due clock (or the per-task override). Identical times do **not** auto-coalesce; opt-in **morning digest** is the only batch wakeup.

### Export / import JSON

| | |
|---|---|
| **Helps** | Tablet ↔ phone / backup before wipe |
| **When** | Before second-device pain or OS reset |
| **Pain removed** | Data trapped on one install; cloud-account tax |
| **Does** | User-initiated SAF file export/import of tasks, completions, settings (`schemaVersion` 2). **Import replaces** all local data after confirm. v1 / uuid-less files mint new ids and warn — prefer a current export before linking Google on two devices. If Google is linked, Drive then follows this device. Reached from **Settings**. |
| **Does not** | Require an account or INTERNET; merge/diff import (v1) |

---

## Tier 1 — Fit real life

### All tasks library

| | |
|---|---|
| **Helps** | Finding a task that isn’t on the pending home (completed cycle, later due, paused) |
| **When** | “Where did that chore go?” after Done, or editing cadence off-queue |
| **Pain removed** | Pending-only home hides everything that isn’t due soon |
| **Does** | Browse/edit all pinned tasks; open editor; pause/archive from here |
| **Does not** | Replace pending as the morning home; become a mega project list |

### Free-window / “fit my time”

| | |
|---|---|
| **Helps** | You with a hard stop (leaving, work start, appointment) |
| **When** | Before a free pocket of time |
| **Pain removed** | Staring at five dues and guessing what fits |
| **Does** | User sets **available minutes** and/or **must stop by clock time T**; lists each candidate ≤ the window; leftover is after the best pick |
| **Does not** | Pack several tasks into N minutes; sync a full work calendar product |

**Candidate set:** overdue + due today + soon (7-day).  
**Ranking:** urgency band first (overdue → due today → soon); within band prefer the largest estimate that still fits (each ≤ window). Leftover is after that best pick, not after packing the list. If nothing fits: calm empty state + widen the window or show all.

**“Leaving / committed to work”** = free-window end time (optional saved default work-start in Settings) — not a separate calendar app.

### Quick session start

| | |
|---|---|
| **Helps** | Same as free-window, faster |
| **When** | One-handed, low patience |
| **Pain removed** | Digging through settings to say “I have 20 minutes” |
| **Does** | One-tap chips: **15 / 30 / 45** minutes and **until saved work-start** when configured |
| **Does not** | Replace the full free-window picker |

### Duration honesty

| | |
|---|---|
| **Helps** | Future free-window quality |
| **When** | Right after Done |
| **Pain removed** | Estimates that forever lie; recommendations that overrun |
| **Does** | Lightweight “took longer / shorter?” adjust `estimateMinutes` — no guilt copy |
| **Does not** | Time-tracking stopwatch as core loop |

### Pause / archive

| | |
|---|---|
| **Helps** | Seasonal or paused life (travel, injury, winterized gear) |
| **When** | Task shouldn’t due/remind for a while, or is retired |
| **Pain removed** | Deleting a task just to silence it; losing history |
| **Does** | **Pause** = confirm-gated; hidden from due/reminders until Resume (snooze is cleared); **Archive** = retired, history kept, not in pending |
| **Does not** | Soft-delete without an explicit archive path |

### Skip (this cycle)

| | |
|---|---|
| **Helps** | Rare “not this time” without lying via Done |
| **When** | Task is due but intentionally won’t happen this cycle |
| **Pain removed** | Fake completions that wreck cadence math |
| **Does** | Advances/postpones per cadence mode without a completion record; confirm copy so it isn’t a trash can; same expected-due one-shot as Done |
| **Does not** | Replace Snooze (temporary) or Pause (indefinite) |

**Boundary:** Snooze = later still this obligation; Skip = abandon this cycle; Pause = freeze the task; Done = you did it. Skip ships **without** a reason field; skip-with-reason is not a goal.

---

## Tier 2 — Calm power

### Soft areas (not projects)

| | |
|---|---|
| **Helps** | Scanning a longer list on tablet |
| **When** | Many tasks across house/body/car |
| **Pain removed** | Visual soup without inventing GTD |
| **Does** | Optional **area** label (Bathroom, Car, Body, Paper, …); filter/group on pending — **never required** to create |
| **Does not** | Nested projects, multi-tag taxonomies, team boards |

### Batch / digest reminder

| | |
|---|---|
| **Helps** | Mornings with many same-time dues |
| **When** | Opt-in in Settings |
| **Pain removed** | Ten separate wakes; notification spam |
| **Does** | One digest: count + total minutes + open pending; None stays off it; custom clocks stay per-task on the due day, then join while overdue. Per-task still available as default. Missed standing alarm (boot / force-stop / import after the window) posts once that local day. Same-day new pins notify once. In-app Done / Snooze / Skip dismisses the digest card |
| **Does not** | Force digest on everyone |

### Home-screen widget

| | |
|---|---|
| **Helps** | Glance without opening the app |
| **When** | Home screen |
| **Pain removed** | “Is anything due?” uncertainty |
| **Does** | Pending count + total minutes due today; two-row (and taller) tiles also list a few titles; tap opens pending |
| **Does not** | Live animated engagement bait; frequent background refresh |

### History glance

| | |
|---|---|
| **Helps** | Honest self-calibration |
| **When** | Task detail or quiet settings surface |
| **Pain removed** | Amnesia about “I always do this late” without streak theater |
| **Does** | Last completed; calm fact like typical lateness — **no XP/streaks** |
| **Does not** | Leaderboards or shame charts |

### History hygiene

| | |
|---|---|
| **Helps** | Anyone who Dones for years and does not want a forever log |
| **When** | Settings; quietly on launch and at most once per local day after Done |
| **Pain removed** | Completions accumulating with no off switch |
| **Does** | Age cap (90 days / 1 year / 2 years / keep all) while always keeping the last eight Dones per task; SQL prune (no full-table load); purge history (tasks stay); reset all tasks (settings stay, empty app) |
| **Does not** | Shame, streaks, or cloud delete |

### Exact-alarm permission UX

| | |
|---|---|
| **Helps** | Users who need a precise reminder time |
| **When** | First time a precise schedule is requested on modern Android |
| **Pain removed** | Silent failures when OEM denies exact alarms |
| **Does** | Clear why + system settings path; fall back to inexact when denied |
| **Does not** | Demand exact alarms for every task |

### Notification permission UX

| | |
|---|---|
| **Helps** | People who want due reminders without a mystery prompt on first open |
| **When** | First reminder-bearing pin (API 33+); later if still off |
| **Pain removed** | Cold `POST_NOTIFICATIONS` before any task; deny-once with no way back except Settings |
| **Does** | Explain, then the system dialog; Pending banner + Settings path if still silent |
| **Does not** | Ask on cold start; block the list if they skip |

---

## Tier 3 — Earn it (core in tree)

| Feature | Purpose |
|---|---|
| Richer cadence | Weekly weekday sets, monthly day-of-month, weekday of month (1st–4th / last), yearly months and civil seasons |
| Templates / starters | Catalog on every Add task (area groups + blank); empty-state still multi-check pin; user-chosen, not auto-seed |
| Multi-device via file | Export/import plus optional user-chosen folder (`errata-backup.json`, last write wins). Drive via the system picker |
| Optional Google Drive | Opt-in Google sign-in; hidden Drive **appDataFolder**; merge across devices; WorkManager, no FGS. SAF stays |
| Play Store packaging | Privacy policy + listing/Data safety copy |

Windows / desktop remains **out** until Android is boringly solid (see vision).

## Later

| Feature | Purpose |
|---|---|
| Host privacy URL | https://userX-324-A.github.io/Errata/privacy.html |
| Play Console closed beta | Ordinary Tools account, screenshots, AAB, 12 testers — [`08-publish.md`](./08-publish.md) |

---

## Explicitly deferred / never

- Streaks, XP, social, coaching feeds  
- Family assignment / multi-user chores  
- Full GTD, kanban, tags sprawl, nested projects  
- Always-on tracking / sticky FGS for “engagement”  
- Medical or clinical treatment claims  
- Cloud account as **default** path (optional Google is opt-in)  
- Windows client before Android MVP is solid  

---

## Cadence policy

Schedule **kind** is orthogonal to after-Done **mode**.

| Kind | Next due after Done or Skip |
|---|---|
| **Every N days** (default) | After-Done mode below |
| **Weekly** | Next selected weekday strictly after both the event and the open due day (early Done/Skip consumes that slot), same clock time as the due that was open |
| **Monthly** | Next occurrence of day-of-month 1–31; if that day does not exist, clamp to the last day of the month. Same consume + keep-time rule |
| **Weekday of month** | Next 1st / 2nd / 3rd / 4th / last of one weekday strictly after both the event and the open due day. 1st–4th always exist; last is the 4th or 5th depending on the month. Same keep-time rule |
| **Yearly** | Union of selected months (shared day 1–31, clamp missing days) and northern civil season starts (20 Mar / 21 Jun / 22 Sep / 21 Dec). Same consume + keep-time rule |

### Modes (per task; global default in Settings; interval tasks only)

| Mode | Next due after Done |
|---|---|
| **From completion** | `completedAt + interval` |
| **Fixed anchor** | Next slot on the original grid (late Done does not shift the grid). Editing due day, interval, mode, or schedule kind retargets the grid to the saved due day. |
| **From completion + catch-up** (default) | Like from-completion, but if badly overdue, compress the wait slightly so seasons don’t drift forever |

### Catch-up formula (v1 sketch)

Inputs: `intervalDays`, `completedAt`, `scheduledDueAt` (the due that was open), `overdue = completedAt − scheduledDueAt`.

1. If `overdue ≤ 0.5 × interval` → treat as **from completion** (`completedAt + interval`).  
2. If `overdue > 0.5 × interval` →  
   `nextDue = completedAt + max(floor, interval − catchUp)`  
   where `catchUp = min(overdue × 0.5, 0.25 × interval)`  
   and `floor = max(1 day, 0.5 × interval)`.  
3. Never schedule `nextDue` earlier than `completedAt + floor`. After snapping to the scheduled time-of-day, if that instant is still before the floor, bump one local calendar day.  
4. UI copy stays neutral (“Next due …”) — no “you were behind” framing.

Exact constants may be tuned after real use; change them in this doc when code lands.

### Late completion

Always allowed. Missing a reminder must not corrupt cadence math (snooze/skip/pause rules above).

---

## Free-window policy

```text
Open app → Pending queue (home)
         → Set free window (minutes and/or stop-by time)
         → Rank candidates (overdue → due today → soon)
         → Show each ≤ window + leftover after best
         → Done / Snooze → back to queue
```

| Decision | v1 default |
|---|---|
| Candidates | Overdue + due today + soon (7 days) |
| Overrun | Do not recommend tasks whose estimate exceeds the **remaining** window. Until work / stop-by keep the clock and shrink remaining each tick; 15/30/45 stay a fixed pocket. |
| Packing | List every individual fit. Leftover after the best pick, not after summing the list |
| Empty fit | Calm message; offer show-unfiltered pending or adjust window |
| Work-start | Optional saved default for “until work” chip |

---

## Action boundaries (quick reference)

| Action | Obligation | Cadence |
|---|---|---|
| **Done** | Fulfilled | Recompute next due |
| **Snooze** | Still open | Due/reminder pushed |
| **Skip** | This cycle abandoned | Advance without completion |
| **Pause** | Frozen | No due/remind until resume |
| **Archive** | Retired | Out of pending; history kept |
