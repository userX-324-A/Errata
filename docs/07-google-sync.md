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

### SHA-1 fingerprints

Debug (sideload from Android Studio / `installDebug`):

```
02:CE:49:CD:32:81:57:D7:14:09:E5:2B:9C:E5:FD:36:80:7C:D2:11
```

Release upload key (`keystore/errata-upload.jks`, Play AAB):

```
C0:C4:BC:0F:E6:55:60:FD:AC:42:5A:81:E8:E0:15:7B:8B:5E:03:62
```

After the first Play upload, add the **App Signing** SHA-1 from Play Console as a third Android client fingerprint.

```bat
keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Client ids are public. Never create or commit a Web client **secret**.

Testing users only until you publish the OAuth consent screen. `drive.appdata` is **non-sensitive**; brand verification (homepage + [privacy.html](https://userX-324-A.github.io/Errata/privacy.html)) is enough for any Google account. Heavy Drive verification is not required. Steps: [`08-publish.md`](./08-publish.md).

## What the app does

- Credential Manager Google sign-in, then `drive.appdata` only. Drive authorize is bound to that signed-in account (`AuthorizationRequest.setAccount`).
- Hidden file `errata-sync.json` in Drive **appDataFolder**. List follows `nextPageToken` until empty. If more than one exists, keep the newest `modifiedTime` and delete the others. First upload with no cached id lists first and patches an existing file instead of creating a second. Patch always sends If-Match (fetch metadata etag if the GET body omitted it). Tasks LWW by uuid `updatedAt`; completions union; shared settings LWW. Purge, **reset with clear Google copy** (tasks *and* history generations), and **linked import** bump generations so the other side drops superseded rows. Unlinked import keeps the file’s generations. Corrupt Drive JSON (including a blank body) fails the round, is not overwritten, and cancels work until Sync now — not retried forever. SAF folder export stays.
- WorkManager: one unique one-shot (`errata-sync`) — 45s debounce after writes (including Pin selected), Sync now / process start replace that delay (not Activity rotation); one 24h catch-up. Debounce and process-start skip while last error is auth or unreadable JSON; Link and Sync now still enqueue. Access token is cached ~50 minutes; Drive 401/403 drops the cache and retries once. If auth is still dead **or the Drive copy is unreadable**, work is cancelled until Link Google or Sync now — not retried forever. Rounds are serialized; a merge is not applied if local changed during the upload. No foreground service.
- Unlink and delete Google copy lists every `errata-sync.json` and only unlinks after all deletes succeed. Stay linked and retry if offline or a sibling remains.

## Two-device check (after OAuth works)

Do this on a phone and a tablet signed into the **same** test Google account.

1. Device A: pin a task, mark Done, confirm it appears on B after Sync now / opening the app.
2. Device B: edit the title; A should show the new title after sync.
3. Pin the same chore independently on both **before** linking — you get two tasks (no fuzzy merge).
4. Yearly task with few Dones: typical lateness still has samples after a 90-day retention setting.
5. Purge history on A; B should drop old Dones but keep a Done made after the purge.
6. Reset on A with **Also clear the Google copy** checked — both empty, including Dones. Unchecked — A empty until sync restores from Google.
7. Import a backup on A while linked — B should match the file (tasks that were only on Drive and not in the file disappear). Import while unlinked does not stamp wipe generations; a later Link merges with Drive.
8. Unlink on A: local list stays. Unlink and delete Google copy: every `errata-sync.json` gone; B will upload again if still linked. Airplane mode + delete Google copy should stay linked and say it failed.
9. Airplane mode write, then reconnect — debounce/catch-up uploads without a stuck spinner of task titles.

Then follow [`08-publish.md`](./08-publish.md) for Play closed testing.

## Battery

Linked devices only. One daily WM plus debounce. Not a poll loop.
