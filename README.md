# Errata

Personal **upkeep** for the mundane: recurring life tasks (nails, beard, bathrooms, anything that slips), with schedules, estimated duration, reminders, and a focused pending queue.

Android-first. Offline and battery-conscious. No streak guilt, no social feed, no cloud required.

## Status

Greenfield scaffold — product intent and agent guardrails first; app implementation follows in this repo.

## Start here

| Doc | Purpose |
|---|---|
| [AGENTS.md](AGENTS.md) | Agent session contract |
| [docs/README.md](docs/README.md) | Design authority index |
| [docs/00-vision.md](docs/00-vision.md) | What we are building and why |
| [docs/01-architecture.md](docs/01-architecture.md) | Planned shape (Android, data, reminders) |

## Principles

1. **Corrections, not hustle** — Errata is life’s small fixes on a cadence, not a gamified habit streak machine.
2. **Pending-first** — The primary screen is what is due / overdue / soon, not a wall of statistics.
3. **Local-first** — Data stays on device. Sync/export is optional and explicit later.
4. **Battery is a feature** — No always-on services for vanity. Prefer scheduled exact alarms / WorkManager only when needed; survive Doze.
5. **Ancient-device friendly** — Target and test for older Android (tablet sideload first while the phone USB port is dead).
6. **One concern per change** — Reminder reliability ≠ UI polish ≠ schema.

## Workspace

Open this folder (`Errata`) as the Cursor workspace so `.cursor/rules/` apply.
