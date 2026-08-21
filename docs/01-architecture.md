# Architecture — Errata (planned)

Living doc. Update when stack decisions land in code.

## Target shape

```
┌─────────────────────────────────────┐
│  UI (Compose / Material 3)          │
│  Pending queue · Task editor · Set  │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│  Domain                             │
│  Cadence rules · Due calculation    │
│  Completion · Snooze                │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│  Data (Room)                        │
│  Tasks · Completions · Settings     │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│  Reminders                          │
│  Boot reschedule · Notifications    │
│  Done / Snooze actions              │
└─────────────────────────────────────┘
```

## Stack decisions

| Choice | Status | Notes |
|---|---|---|
| Kotlin | Planned | |
| Jetpack Compose + M3 | Planned | Revisit if MVP velocity needs Views |
| Room | Planned | Local source of truth |
| `minSdk` | **TBD** | Bias older tablets; record exact value when Gradle exists |
| DI framework | Avoid until needed | |
| Internet permission | **Absent** by default | |
| Exact alarms | Opt-in per UX | Document permission flows |

## Cadence model (intent)

- Support at least: every N days (from last completion or fixed anchor — **pick one default, allow override**)  
- Weekly / monthly patterns as follow-ons  
- Store `lastCompletedAt`, `nextDueAt`, `estimateMinutes`, `reminderTime`  

## Reminder policy (intent)

- Per-task or global default time-of-day  
- After reboot, restore schedules  
- Prefer no sticky FGS  
- Notification: title + duration hint; actions Done / Snooze  

## Distribution

- Debug/release APK for sideload (Downloads / cloud drive / local network)  
- Play Store optional later; name **Errata** appears clearer than UpKeep for store collision  

## Battery budget

Any new background component needs a one-paragraph justification in the PR and a note here.
