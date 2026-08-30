# Privacy — Errata

Play-usable policy. Must match the APK. Hosted at [https://userX-324-A.github.io/Errata/privacy.html](https://userX-324-A.github.io/Errata/privacy.html). Paste that URL in Play Console and Cloud OAuth consent. The in-app Privacy screen states the same facts offline.

**Publisher:** Ordinary Tools  
**Contact:** [ordinary.tools.apps@gmail.com](mailto:ordinary.tools.apps@gmail.com)

Last updated: 23 August 2026.

## Who we are

**Ordinary Tools** publishes Errata, a personal Android app for recurring upkeep (chores on a cadence, estimates, reminders, a pending list). There is **no Errata account** and no Ordinary Tools servers. The app is free. We do not sell in-app purchases or process payments.

## What is stored

On **this device** (local database and app preferences):

- Tasks you pin (title, notes, schedule, estimates, optional area label)
- Completion history  
- Settings (appearance, default reminder time, digest, cadence default, work-start, how long to keep completion history)
- A persistable folder URI if you choose a backup folder (that URI is not included in exported JSON)
- If you link Google: the email you signed in with, and sync status, on this device only

## Optional Google Drive copy

Linking Google is **opt-in**. If you do:

- Errata asks Google for sign-in and for the **Drive app data** scope only (`drive.appdata`). It cannot see the rest of your Drive.
- A hidden file (`errata-sync.json`) is stored in Google Drive’s app folder for Errata. It uses **your** Drive quota. You do not pick a folder or manage the file in the Drive UI.
- That file includes task titles, notes, schedules, completion history, and shared settings (not appearance, not your backup-folder URI).
- Traffic uses HTTPS to Google. Errata does not run a backend.
- Unlink stops sync. You can also delete the Google copy from Settings. Uninstalling Errata does **not** delete the Drive file unless you chose delete.
- Switching Google accounts: unlink (delete the copy if you want), then link the other account. Two Google accounts are not merged.

You can skip Google entirely. Folder export / JSON backup still work.

## What is not collected

Errata does **not** run Ordinary Tools servers, ads, analytics, or crash-reporting SDKs. Task titles are not sent anywhere except **Google Drive app data if you linked Google**.

The app has the `INTERNET` permission so that optional Google path can run. It does not phone home to Ordinary Tools.

## Permissions

| Permission | Why |
|---|---|
| Internet | Optional Google Drive app data when you link |
| Network state | So background sync can wait for connectivity |
| Notifications | Due reminders (and optional morning digest) |
| Boot completed | Reschedule reminders after reboot |
| Exact alarms (optional, Android 12+) | Fire at the clock time you picked; inexact fallback if declined |

File and folder access uses the **system picker** (SAF) when you export, import, or choose a backup folder. That is not a blanket storage permission.

## Copies you make

You can export or import a JSON file, or write/read `errata-backup.json` in a folder you pick. Last write wins; import replaces all data on that device after you confirm. If Google is linked, import then updates the Drive copy to match this device. If it is not linked, the backup’s generations stay so a later Link merges with Drive instead of wiping it.

If the system picker offers Drive or another provider, that copy lives wherever **you** put it — separate from optional app-data sync.

Completion history can be trimmed automatically (age cap; last eight Dones per task are always kept) or cleared from Settings. Deleting all tasks from Settings removes tasks and history but keeps settings. If Google is linked, reset can also clear the Google copy.

## System cloud backup

Android Auto Backup / cloud backup of Errata app data is **off**. Moving to another device is your export, folder file, optional Google link, or a new install.

## Reminders

Notifications are local. Errata is not a medical device and does not provide clinical advice.

## Changes

If storage, permissions, or network use change, this policy and the in-app Privacy screen must change with them.
