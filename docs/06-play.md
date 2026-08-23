# Play listing — Errata

Packaging notes for a future Play submit. **Not a Console upload.** Sideload remains [`04-sideload.md`](./04-sideload.md). Privacy authority: [`05-privacy.md`](./05-privacy.md).

Name **Errata** (not UpKeep).

## Short description (≤80 characters)

Recurring upkeep on a cadence. Pending list, estimates, reminders. On-device.

## Full description

Errata is for the chores that slip: bathrooms, filters, paperwork, anything you mean to do on a cadence.

Pin a task, set how often (every N days, weekdays, or a day of the month), estimate how long it takes, and work from a pending list — overdue, due today, soon. When time is short, say how many minutes you have.

Completing late is normal. No streaks, no social, no Errata account. Data stays on this device. Export a JSON file when you want a copy; optional folder write/read for tablet and phone. Reminders are local. Not a medical device.

## Data safety (Play form)

Answer from the **current APK**, not future plans.

| Question | Answer |
|---|---|
| Does the app collect user data? | **No** (nothing sent to Errata; no Errata servers) |
| Is data shared with third parties? | **No** |
| Encrypted in transit? | N/A — the app has no `INTERNET` permission |
| Users can request deletion? | Data is on-device; uninstall or use Backup → import replace / clear by not restoring |
| Data collected types | None collected by the developer |
| Security practices | Data not encrypted in transit (no network). On-device Room. User-chosen export files are plain JSON |

Independent copies the **user** saves via the system file picker (including Drive if they pick it) are not “collection” by Errata.

## Permissions (declare as used)

- `POST_NOTIFICATIONS` — reminders
- `RECEIVE_BOOT_COMPLETED` — reschedule after reboot
- `SCHEDULE_EXACT_ALARM` — optional on-time reminders (not required)

No `INTERNET`, no ads, no full storage permission.

## Submit later (not this slice)

- Paid Play developer account
- Privacy policy **URL** (host `05-privacy.md`)
- Phone + tablet screenshots (pending, editor, settings)
- Content rating questionnaire
- Release signing / AAB (do not commit keystores)
- Feature graphic

## Store claims to avoid

Streaks, coaching, family assignment, cloud accounts, medical treatment, “we sync to Drive for you.”
