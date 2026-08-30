# Publish — remaining human steps

Repo work for listing copy, Data safety answers, assets, and the hosted policy is done. **You** do Play Console, Cloud OAuth, testers, and the AAB upload. Agents cannot.

**Public URLs (after Pages is live):**

| Use | URL |
|---|---|
| Homepage (OAuth + Play) | https://userX-324-A.github.io/Errata/ |
| Privacy policy | https://userX-324-A.github.io/Errata/privacy.html |
| Contact | ordinary.tools.apps@gmail.com |

Developer name **Ordinary Tools**. App name **Errata**. Free. No IAP.

`drive.appdata` is a **non-sensitive** scope. You do not need Google’s heavy Drive verification. Light **brand verification** (homepage + privacy + support email) is enough so testers/users do not see an “unverified app” warning.

---

## 1. Cloud Console (OAuth)

[Google Cloud Console](https://console.cloud.google.com/) → the Errata project.

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
4. Android OAuth client (`com.errata.app`): keep debug + upload SHA-1s. After the first AAB, Console → App integrity → copy **App signing key certificate SHA-1** into this Android client as a third fingerprint. Without it, Play-installed builds cannot Link Google.

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
- App icon: `play/icon-512.png`
- Feature graphic: `play/feature-graphic-1024x500.png`
- Screenshots: at least two **phone** (Pixel), plus **tablet** if the form asks. Use fake chore titles. Capture yourself; do not upload shots of real tasks.
- Privacy policy URL: `https://userX-324-A.github.io/Errata/privacy.html`
- Contact: `ordinary.tools.apps@gmail.com`
- **Data safety:** no collection by Ordinary Tools; data shared with Google **only if** the user links Google; encrypted in transit (HTTPS); users can delete on-device / unlink / delete Google copy
- Content rating questionnaire (IARC). Not a medical device. No violence, no kids target.
- Ads: no
- App access: Google is **optional**. Reviewers can use the app without signing in. Leave credentials blank.
- News / COVID / financial: no
- Target SDK: if Console requires API 36, bump `compileSdk`/`targetSdk` in `app/build.gradle.kts` and rebuild the AAB (today they are 35).

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
4. Add the same Gmails as Cloud OAuth **test users** until you publish the consent screen.
5. You + Pixel + tablet can be 1–2 of the 12 if those Google accounts opt in via Play.
6. Ask testers: install, open, pin one fake task, leave installed. If count drops below 12, the 14-day window can restart.
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
