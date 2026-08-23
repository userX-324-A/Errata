# Play listing — Errata

Closed-beta packaging. Sideload remains [`04-sideload.md`](./04-sideload.md). Privacy authority: [`05-privacy.md`](./05-privacy.md).

**App name:** Errata (not UpKeep)  
**Developer name:** Ordinary Tools  
**Contact:** ordinary.tools.apps@gmail.com  
**Price:** Free. No in-app products, no donations.

Privacy URL (public HTTPS, not a Google Doc). This GitHub repo is **private**, so Pages is not available on the free plan. Cheapest: a tiny **public** repo with only [`privacy.html`](./privacy.html), or Cloudflare Pages. Then paste that URL into Play and Cloud OAuth consent.

## Short description (≤80 characters)

Recurring upkeep on a cadence. Pending list, estimates, reminders. On-device.

## Full description

Errata is for the chores that slip: bathrooms, filters, paperwork, anything you mean to do on a cadence.

Pin a task, set how often (every N days, weekdays, a day of the month, the first/last weekday of the month, or yearly/seasonal dates), estimate how long it takes, and work from a pending list — overdue, due today, soon. When time is short, say how many minutes you have.

Completing late is normal. No streaks, no social, no Errata account. Data stays on this device unless you opt in to Google Drive app data. Export a JSON file when you want a copy; optional folder write/read for tablet and phone. Reminders are local. Not a medical device.

Published by Ordinary Tools. Free. ordinary.tools.apps@gmail.com

## Data safety (Play form)

Answer from the **current APK**, not future plans.

| Question | Answer |
|---|---|
| Does the app collect user data? | **No** collection by Ordinary Tools (no our servers). Opt-in Google Drive app data is the user’s Google account. |
| Is data shared with third parties? | **Only if the user links Google** — then task data is stored in Google Drive app data |
| Encrypted in transit? | **Yes** for the Google path (HTTPS). No other network. |
| Users can request deletion? | Data is on-device; Settings can clear history, delete all tasks, unlink, and delete the Google copy; uninstall removes local data. Contact ordinary.tools.apps@gmail.com |
| Data collected types | None collected by the developer |
| Security practices | HTTPS to Google when linked. On-device Room. User-chosen export files are plain JSON |

Independent copies the **user** saves via the system file picker (including Drive if they pick it) are not “collection” by Ordinary Tools. Optional Link Google is Drive **appDataFolder**, documented in [`07-google-sync.md`](./07-google-sync.md).

## Permissions (declare as used)

- `INTERNET` / network state — optional Google Drive app data
- `POST_NOTIFICATIONS` — reminders
- `RECEIVE_BOOT_COMPLETED` — reschedule after reboot
- `SCHEDULE_EXACT_ALARM` — optional on-time reminders (not required)

No ads, no full storage permission.

## Closed beta checklist

Do these while Play identity verification is pending. Console upload waits on that.

1. **Account** — Personal developer account (not organization). Identity in progress. Developer name **Ordinary Tools**.
2. **Privacy URL** — Host [`privacy.html`](./privacy.html) on a public HTTPS URL (see above). Do not use a GitHub blob link.
3. **AAB** — `gradlew.bat :app:bundleRelease` → `app\build\outputs\bundle\release\app-release.aab`. Signed with the local upload keystore (`keystore/`, not in git).
4. **OAuth SHA-1s** — Android client `com.errata.app` needs:
   - Debug: `02:CE:49:CD:32:81:57:D7:14:09:E5:2B:9C:E5:FD:36:80:7C:D2:11`
   - Upload key: `C0:C4:BC:0F:E6:55:60:FD:AC:42:5A:81:E8:E0:15:7B:8B:5E:03:62`
   - Play App Signing SHA-1 (add after the first AAB upload; Console → App integrity)
5. **OAuth testers** — Keep consent **Testing**. Add closed-test Gmails as Cloud test users. Do not start Drive scope verification until production.
6. **Store assets** — [`play/`](../play/README.md): 512 icon, 1024×500 feature graphic, phone + tablet screenshots (fake task titles).
7. **App content** — Content rating; target audience not children; not a medical device; no ads; app access: Google link is optional, reviewers can use the app without signing in.
8. **Closed test** — After verification: upload AAB → closed testing → **12 testers opted in for 14 days** → apply for production.

`targetSdk` is 35 today. If you submit after **31 August 2026**, Play may require 36 — bump compile/target then.

## Store claims to avoid

Streaks, coaching, family assignment, Errata/Ordinary Tools cloud accounts, medical treatment, “we sync to Drive for you” without saying it is opt-in Google, payments or donations.
