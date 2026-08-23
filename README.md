# Errata

Personal **upkeep** for the mundane: recurring life tasks (nails, beard, bathrooms, anything that slips), with schedules, estimated duration, reminders, and a focused pending queue.

Android-first. Offline and battery-conscious. No streak guilt, no social feed, no cloud required.

## Status

Android daily driver is usable (pending queue, cadence including yearly/seasons, starters, folder backup, history retention, optional Google Drive App Data, offline-first privacy). Sideload: [docs/04-sideload.md](docs/04-sideload.md). Google setup: [docs/07-google-sync.md](docs/07-google-sync.md). Later: Play Console submit.

## Start here

| Doc | Purpose |
|---|---|
| [AGENTS.md](AGENTS.md) | Agent session contract |
| [docs/README.md](docs/README.md) | Design authority index |
| [docs/00-vision.md](docs/00-vision.md) | What we are building and why |
| [docs/01-architecture.md](docs/01-architecture.md) | Planned shape (Android, data, reminders) |
| [docs/04-sideload.md](docs/04-sideload.md) | Build and install the APK |
| [docs/05-privacy.md](docs/05-privacy.md) | Privacy policy |

## Principles

1. **Corrections, not hustle** — Errata is life’s small fixes on a cadence, not a gamified habit streak machine.
2. **Pending-first** — The primary screen is what is due / overdue / soon, not a wall of statistics.
3. **Local-first** — Data stays on this device. Export/folder copy is optional and user-initiated.
4. **Battery is a feature** — No always-on services for vanity. Prefer scheduled exact alarms / WorkManager only when needed; survive Doze.
5. **Ancient-device friendly** — Target and test for older Android (tablet sideload first while the phone USB port is dead).
6. **One concern per change** — Reminder reliability ≠ UI polish ≠ schema.

## Workspace

Open this folder (`Errata`) as the Cursor workspace so `.cursor/rules/` apply.
