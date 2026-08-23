# Play listing — Errata

Packaging notes for a future Play submit. **Not a Console upload.** Sideload remains [`04-sideload.md`](./04-sideload.md). Privacy authority: [`05-privacy.md`](./05-privacy.md).

Name **Errata** (not UpKeep).

## Short description (≤80 characters)

Recurring upkeep on a cadence. Pending list, estimates, reminders. On-device.

## Full description

Errata is for the chores that slip: bathrooms, filters, paperwork, anything you mean to do on a cadence.

Pin a task, set how often (every N days, weekdays, a day of the month, the first/last weekday of the month, or yearly/seasonal dates), estimate how long it takes, and work from a pending list — overdue, due today, soon. When time is short, say how many minutes you have.

Completing late is normal. No streaks, no social, no Errata account. Data stays on this device unless you opt in to Google Drive app data. Export a JSON file when you want a copy; optional folder write/read for tablet and phone. Reminders are local. Not a medical device.

## Data safety (Play form)

Answer from the **current APK**, not future plans.

| Question | Answer |
|---|---|
| Does the app collect user data? | **No** collection by Errata (no Errata servers). Opt-in Google Drive app data is the user’s Google account. |
| Is data shared with third parties? | **Only if the user links Google** — then task data is stored in Google Drive app data |
| Encrypted in transit? | **Yes** for the Google path (HTTPS). No other network. |
| Users can request deletion? | Data is on-device; Settings can clear history, delete all tasks, unlink, and delete the Google copy; uninstall removes local data |
| Data collected types | None collected by the developer |
| Security practices | HTTPS to Google when linked. On-device Room. User-chosen export files are plain JSON |

Independent copies the **user** saves via the system file picker (including Drive if they pick it) are not “collection” by Errata. Optional Link Google is Drive **appDataFolder**, documented in [`07-google-sync.md`](./07-google-sync.md).

## Permissions (declare as used)

- `INTERNET` / network state — optional Google Drive app data
- `POST_NOTIFICATIONS` — reminders
- `RECEIVE_BOOT_COMPLETED` — reschedule after reboot
- `SCHEDULE_EXACT_ALARM` — optional on-time reminders (not required)

No ads, no full storage permission.

## Submit later (not this slice)

- Paid Play developer account
- Privacy policy **URL** (host `05-privacy.md`)
- Phone + tablet screenshots (pending, editor, settings)
- Content rating questionnaire
- Release signing / AAB (do not commit keystores)
- Feature graphic

## Store claims to avoid

Streaks, coaching, family assignment, Errata cloud accounts, medical treatment, “we sync to Drive for you” without saying it is opt-in Google.
