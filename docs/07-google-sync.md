# Google Drive App Data — setup and device checks

Optional sync. Core Errata still works fully offline. Agents cannot create the Cloud OAuth client.

## Human: Cloud Console (required before Link Google works)

1. [Google Cloud Console](https://console.cloud.google.com/) → new project (e.g. Errata).
2. Enable **Google Drive API**.
3. **APIs & Services → OAuth consent screen**
   - User type: **External**
   - App name: **Errata**
   - Publishing: **Testing**
   - Scopes: `openid`, `email`, `profile`, and `https://www.googleapis.com/auth/drive.appdata`
   - Test users: add the Google accounts you will sign in with (your phone and tablet).
4. **Credentials → Create credentials → OAuth client ID**
   - **Android:** package `com.errata.app`. SHA-1 of the debug keystore (and later release / Play App Signing).
   - **Web application:** no redirect required. Copy the client id (`….apps.googleusercontent.com`).
5. Put the **Web** client id in `local.properties` (not committed):

```properties
errata.googleWebClientId=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
```

Rebuild the APK after this line exists. Settings → Google shows **Link Google** only when this id is present.

### Debug SHA-1

```bat
keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Copy the SHA-1 into the Android OAuth client. Add the release SHA-1 the same way when you sign a release APK.

Client ids are public. Never create or commit a Web client **secret**.

Testing users only until you host [`05-privacy.md`](./05-privacy.md) and complete brand verification. Production “any Google account” is later, with Play Console.

## What the app does

- Credential Manager Google sign-in, then `drive.appdata` only.
- Hidden file `errata-sync.json` in Drive **appDataFolder**. Merge, not last-write-wins. SAF folder export stays.
- WorkManager: 45s debounce after writes, on app open, one 24h catch-up. No foreground service.

## Two-device check (after OAuth works)

Do this on a phone and a tablet signed into the **same** test Google account.

1. Device A: pin a task, mark Done, confirm it appears on B after Sync now / opening the app.
2. Device B: edit the title; A should show the new title after sync.
3. Pin the same chore independently on both **before** linking — you get two tasks (no fuzzy merge).
4. Yearly task with few Dones: typical lateness still has samples after a 90-day retention setting.
5. Purge history on A; B should drop old Dones but keep a Done made after the purge.
6. Reset on A with **Also clear the Google copy** checked — both empty. Unchecked — A empty until sync restores from Google.
7. Unlink on A: local list stays. Unlink and delete Google copy: Drive file gone; B will upload again if still linked.
8. Airplane mode write, then reconnect — debounce/catch-up uploads without a stuck spinner of task titles.

Then ask before Play Console submit vs more polish.

## Battery

Linked devices only. One daily WM plus debounce. Not a poll loop.
