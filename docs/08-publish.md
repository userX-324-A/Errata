# Publish — remaining human steps

The Android daily driver is ready to sideload and to upload. Schema is Room **v9**; friend-test matrix: [`04-sideload.md`](./04-sideload.md). Listing copy, Data safety answers, store assets, and the hosted policy are in the repo. **You** do Play Console, Cloud OAuth, testers, and the AAB. Agents cannot.

This AAB (`versionName` **0.1.0**, `versionCode` **1**, `com.errata.app`): `minSdk` 26, `targetSdk` / `compileSdk` **35**. Local-first; no account gate; notifications are not requested on first open; morning digest is off; Google Drive app data is opt-in; home widget is optional.

**Public URLs (Pages is live):**

| Use | URL |
|---|---|
| Homepage (OAuth + Play) | https://userX-324-A.github.io/Errata/ |
| Privacy policy | https://userX-324-A.github.io/Errata/privacy.html |
| Contact | ordinary.tools.apps@gmail.com |

Developer name **Ordinary Tools**. App name **Errata**. Free. No IAP.

`drive.appdata` is a **non-sensitive** scope. You do not need Google’s heavy Drive verification. Light **brand verification** (homepage + privacy + support email) is enough so testers/users do not see an “unverified app” warning.

---

## 0. Before you upload

On a real tablet or phone (sideload debug or a signed release APK — not only the emulator):

1. Walk [`04-sideload.md`](./04-sideload.md#device-checks-friends--tablet): no cold notification dialog; pin a reminder task; Save returns to the list (including Back / Not now on the first-pin prompts); tablet two-pane keeps the list; tap the same row after Save and the editor stays open.
2. Optional Google: [`07-google-sync.md`](./07-google-sync.md). Play-installed **Link Google** will fail until the **App Signing** SHA-1 is on the Android OAuth client (step 1.4 after the first AAB). Sideload debug uses the debug SHA-1 already listed there.
3. Capture screenshots from a **throwaway** pin list (see [`play/README.md`](../play/README.md)). Do not upload shots of real chores.

Paid closed testers only need install → open → pin one fake task. Friends walking a real device should use the full sideload matrix.

---

## 1. Cloud Console (OAuth)

[Google Cloud Console](https://console.cloud.google.com/) → the Errata project. First-time setup detail: [`07-google-sync.md`](./07-google-sync.md).

1. OAuth consent screen:
   - App name **Errata**
   - User support email: `ordinary.tools.apps@gmail.com`
   - App logo: `play/icon-512.png` (optional)
   - App home page: `https://userX-324-A.github.io/Errata/`
   - Privacy policy: `https://userX-324-A.github.io/Errata/privacy.html`
   - Developer contact: `ordinary.tools.apps@gmail.com`
   - Scopes already: `openid`, `email`, `profile`, `https://www.googleapis.com/auth/drive.appdata`
2. **While closed testing:** leave publishing **Testing**. Add every tester Gmail (plus your Pixel/tablet account) under Test users. Recruit **15–16** so you stay ≥12 if someone drops.
3. **After Play production is approved:** set publishing to **In production**. Submit **brand verification** (not restricted-scope verification). Same homepage + privacy URLs.
4. Android OAuth client (`com.errata.app`): keep debug + upload SHA-1s. After the first AAB, Console → App integrity → copy **App Signing key certificate SHA-1** into this Android client as a third fingerprint. Without it, Play-installed builds cannot Link Google.

Debug SHA-1: `02:CE:49:CD:32:81:57:D7:14:09:E5:2B:9C:E5:FD:36:80:7C:D2:11`  
Upload SHA-1: `C0:C4:BC:0F:E6:55:60:FD:AC:42:5A:81:E8:E0:15:7B:8B:5E:03:62`

---

## 2. Play Console — create the app

[Play Console](https://play.google.com/console) → Create app.

- Name: **Errata**
- Default language: English (US)
- Type: App
- Free
- Declarations: not a government app; Meet Families Policy = no (not for kids)

Fill from [`06-play.md`](./06-play.md):

- Short / full description
- App icon: `play/icon-512.png` (terracotta proof mark on paper; listing theme is Light)
- Feature graphic: `play/feature-graphic-1024x500.png` (Fraunces wordmark + same mark)
- Screenshots: at least two **phone** (Pixel), plus **tablet** (7″ and/or 10″ if the form asks). Fake chore titles. Capture current UI: pending + free-window chips, editor (cadence + minutes), catalog or empty pin, Settings (Google unlinked is fine), tablet two-pane (list beside editor). See [`play/README.md`](../play/README.md).
- Privacy policy URL: `https://userX-324-A.github.io/Errata/privacy.html`
- Contact: `ordinary.tools.apps@gmail.com`
- **Data safety:** no collection by Ordinary Tools; data shared with Google **only if** the user links Google; encrypted in transit (HTTPS); users can delete on-device / unlink / delete Google copy
- Content rating questionnaire (IARC). Not a medical device. No violence, no kids target.
- Ads: no
- App access: Google is **optional**. Reviewers can use the app without signing in. Leave credentials blank.
- News / COVID / financial: no
- Target SDK: if Console requires API 36, bump `compileSdk`/`targetSdk` in `app/build.gradle.kts` and rebuild the AAB (today they are **35**).

---

## 3. Build and upload the AAB

On this machine (keystore already in `local.properties`, not in git):

```bat
gradlew.bat :app:bundleRelease
```

Upload `app\build\outputs\bundle\release\app-release.aab` to **Testing → Closed testing** (not Internal, not Production).

Turn on Play App Signing when asked (default). Then add the App Signing SHA-1 to Cloud (step 1.4).

---

## 4. Closed testers (paid is fine)

Requirement: **12 distinct Google accounts**, opted into **this closed track**, installed from the Play opt-in link, **opened the app**, stayed opted in **14 consecutive days**. Internal testing and USB sideload do not count.

1. Create an email list in Play Console with 15–16 addresses.
2. Hire a closed-test service that **installs from Play and opens the app**, not opt-in-only. Search current options (“Play closed testing 12 testers 14 days”). Skip the cheapest farms that only click the link.
3. Send them the Play **opt-in URL** (not the sideload APK).
4. Add the same Gmails as Cloud OAuth **test users** until you publish the consent screen. Do not require paid testers to Link Google.
5. You + Pixel + tablet can be 1–2 of the 12 if those Google accounts opt in via Play.
6. Ask testers: install, open, pin one fake task, leave installed. If count drops below 12, the 14-day window can restart. Friends walking a real tablet should use the [device check matrix](./04-sideload.md#device-checks-friends--tablet).
7. Keep a few notes (who, what they reported, what you changed) for the production-access questionnaire.

---

## 5. After 14 days

1. Play Dashboard → **Apply for production access**. Answer how you recruited, that testers used the app, and that it is a free local-first upkeep list. Do not claim streaks, medical treatment, or an Errata cloud account.
2. When production is unlocked: create a **Production** release (same or newer AAB).
3. Cloud OAuth → **In production** + brand verification.
4. Spot-check Link Google on a Play-installed build (signing SHA-1 must already be on the Android client).

---

## Do not

- Put `local.properties` or `keystore/*.jks` on GitHub
- Use a GitHub blob/raw URL as the privacy policy
- Turn on payments or donations
- Submit `drive` or `drive.file` scopes
- Count emulator-only testers toward the 12
- Ask Play reviewers or paid testers to complete the full sideload matrix (that is for you and friends)
