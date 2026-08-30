# Sideload — Errata APK

Personal installs without Play Store. Fine for tablet/phone while USB is flaky.

## Build a debug APK

From the repo root on Windows:

```bat
gradlew.bat :app:assembleDebug
```

Output:

`app\build\outputs\apk\debug\app-debug.apk`

## Build a release APK

Release is minified (R8). Needs the upload keystore in `local.properties` for a signed APK; unsigned still builds:

```bat
gradlew.bat :app:assembleRelease
```

Output:

`app\build\outputs\apk\release\app-release.apk`

## Copy to the tablet

Any of:

- USB file transfer
- Local network share / Nearby Share
- Cloud drive (Downloads folder on the tablet)

No app account required — just the APK file.

## Install on device

1. Open the APK from Files / Downloads.
2. If Android blocks it, allow installs from that source (Files, Drive, etc.) for this one install.
3. Confirm install. Open **Errata**.

Debug APKs are signed with the local debug keystore — good enough for personal sideload. Play upload uses a separate keystore (`keystore/`, not in git); see [`06-play.md`](./06-play.md).

## After install

- Grant **notifications** when you pin a task that reminds (not on first open). If you skip, Pending shows a quiet banner; Settings still has a path.
- Optional on Android 12+: **Alarms & reminders** special access (Settings → On-time reminders) if you want them at the clock time you picked. Declining is fine — reminders still fire, sometimes a few minutes late.
- Optional: **Backup** under Settings to export/import JSON, or a shared folder. Optional: **Link Google** (hidden Drive app folder) after you add an OAuth client — [docs/07-google-sync.md](./07-google-sync.md).
- **Privacy** under Settings states what stays on device and what optional Google link sends. Android cloud backup of Errata data is off.
- Optional: pin the **Errata** widget from the launcher widget picker. Default is a larger tile (count, minutes, and a few titles). Shrink it if you only want the count line. If you already had a tiny tile, remove and re-pin (or resize) to get the new default. It does not poll; it updates when you change tasks, when you resize it, and once around local midnight.

## Device checks (friends / tablet)

Unit tests cover cadence, reminder fire times, digest membership, backup, sync merge, and catalog Save returning to the list (pane stack). Walk the rest on a **real tablet or phone** (sideload or Play). Calm titles; no real personal notes.

### Shade

- [ ] Fresh install: first open does **not** show the system notification dialog; pin a reminder task — rationale, then the system prompt
- [ ] Deny notifications — Pending banner (Allow reminders); list still works; Settings still has a path
- [ ] Pin a task with a reminder; when the card appears, **Done** once — next due advances; the card is gone
- [ ] Double-tap shade **Done** before it dismisses — one completion, not two cycles
- [ ] Complete in-app, leftover shade **Snooze** — refused / gone; does not snooze the new cycle
- [ ] Open in-app Snooze, leftover shade **Done**, then confirm Snooze — refused; no wakeup on the next cycle
- [ ] Open in-app Skip, leftover shade **Done**, then confirm Skip — refused; does not skip the next cycle
- [ ] Pause or archive, leftover shade Done/Snooze — refused

### Digest (Settings → morning digest on)

- [ ] N=0 at the default clock — silent
- [ ] N=1 — the usual per-task card (Done / Snooze)
- [ ] N≥2 — one digest (count + total minutes); tap opens pending
- [ ] Force-stop overnight past the digest clock, open the app — one missed digest that local day, not a second card
- [ ] Pin a due-today task after the window, then open Settings — still one same-day card, not a second
- [ ] After today’s digest, change the default clock to later today — no second digest this local day
- [ ] Digest RTC with the app process dead (not force-stop) around the default clock — one card, not two
- [ ] Digest N≥2, Done in-app — the digest card is gone (count/minutes do not linger)
- [ ] After today’s digest, import a backup with due-today tasks — one same-day card, not silence until tomorrow

### Pending home

- [ ] Until work after that clock — “that clock has passed,” not “Nothing fits in 0 min”; Show all still works
- [ ] Caught-up empty (pins exist, nothing due) — Add FAB still there
- [ ] In-app Done on a 5-minute chore — no honesty sheet; 10+ minutes still asks

### Import and Google

- [ ] Settings → Backup import (SAF) replace-all while offline — use a **current** export. An older uuid-less file warns before replace
- [ ] Import while **linked** — the other device matches the file after Sync now (see [`07-google-sync.md`](./07-google-sync.md) two-device step 7). First Link: if the app is killed during Drive consent, it should still link (or show a sign-in error), not stay silently unlinked

### Catalog Save

- [ ] Compact: Add task → starter → Save returns to Pending, not the catalog. If the notify/exact prompt appears, Back or Not now still returns to Pending. Back from the editor before Save still shows the catalog
- [ ] Empty state: tap a starter row opens the editor (minutes); checkbox + Pin selected still pins several as-is
- [ ] Two-pane: same Save keeps the list visible; detail clears. Tap the same row (or Blank / the same starter again) — editor stays open

### Tablet layout

- [ ] Compact / phone: bottom bar; editor is a full-screen push
- [ ] Medium and expanded (7–10″ portrait and landscape): navigation rail; Pending, All tasks, and catalog keep the list beside the editor; Save does not hide the list
- [ ] Wide two-pane: editor can use two columns; medium two-pane keeps a single-column editor

## Rebuild and update

Build a new APK the same way, copy it over, and install again (Android will update the existing `com.errata.app` package). Use **Backup → Export** first if you care about data on a wipe.
