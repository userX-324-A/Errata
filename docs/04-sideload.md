# Sideload — Errata APK

Personal installs without Play Store. Fine for tablet/phone while USB is flaky.

## Build a debug APK

From the repo root on Windows:

```bat
gradlew.bat :app:assembleDebug
```

Output:

`app\build\outputs\apk\debug\app-debug.apk`

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

Debug APKs are signed with the local debug keystore — good enough for personal sideload. Release/Play signing comes later if you ship publicly.

## After install

- Grant **notifications** when prompted (reminders).
- Optional: **Backup** from the pending screen overflow (⋮) to export/import JSON between devices.

## Rebuild and update

Build a new APK the same way, copy it over, and install again (Android will update the existing `com.errata.app` package). Use **Backup → Export** first if you care about data on a wipe.
