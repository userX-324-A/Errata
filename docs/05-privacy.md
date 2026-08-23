# Privacy — Errata

Play-usable policy. Must match the APK. Host this file (GitHub / Pages) and paste that URL in Play Console when you submit. The in-app Privacy screen states the same facts offline — Errata does not open a browser and does not include `INTERNET`.

**Contact when hosted:** replace this line with an email you actually read.

Last updated: 23 August 2026.

## Who we are

Errata is a personal Android app for recurring upkeep (chores on a cadence, estimates, reminders, a pending list). There is **no Errata account**.

## What is stored

On **this device** only (local database and app preferences):

- Tasks you pin (title, notes, schedule, estimates, optional area label)
- Completion history
- Settings (appearance, default reminder time, digest, cadence default, work-start)
- A persistable folder URI if you choose a backup folder (that URI is not included in exported JSON)

## What is not collected

Errata does **not** run Errata servers, ads, analytics, or crash-reporting SDKs. Task titles are not sent to third parties by the app.

The app has **no `INTERNET` permission**. It cannot phone home.

## Permissions

| Permission | Why |
|---|---|
| Notifications | Due reminders (and optional morning digest) |
| Boot completed | Reschedule reminders after reboot |
| Exact alarms (optional, Android 12+) | Fire at the clock time you picked; inexact fallback if declined |

File and folder access uses the **system picker** (SAF) when you export, import, or choose a backup folder. That is not a blanket storage permission.

## Copies you make

You can export or import a JSON file, or write/read `errata-backup.json` in a folder you pick. Last write wins; import replaces all data on that device after you confirm. If the system picker offers Drive or another provider, that copy lives wherever **you** put it — Errata does not sign into Drive.

## System cloud backup

Android Auto Backup / cloud backup of Errata app data is **off**. Moving to another device is your export, folder file, or a new install.

## Reminders

Notifications are local. Errata is not a medical device and does not provide clinical advice.

## Changes

If storage, permissions, or network use change, this policy and the in-app Privacy screen must change with them.
