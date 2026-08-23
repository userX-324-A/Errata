# Roadmap — Errata

Checked history of what shipped, plus **Later** for named leftovers.  
**Why** for each item: [`03-product-map.md`](./03-product-map.md). Do not add features that lack a map row.

Phase 0–3 **core** is complete: pending daily driver, cadence through yearly/seasons, starters, folder backup, privacy/listing text.

## Phase 0 — Trust the list (scaffold → MVP)

Tier 0 in the product map.

- [x] Repo, README, AGENTS, Cursor rules, docs vision/architecture
- [x] Android project (Gradle, Compose shell, pending empty state)
- [x] Product map + cadence/free-window policy docs
- [x] Room schema v1 + cadence/due helpers (modes + catch-up formula)
- [x] Pending list UI (overdue → due today → soon) + task editor
- [x] Done + Snooze (in-app)
- [x] Reminder scheduling + boot receiver + notification Done/Snooze
- [x] Due / reminder date + time pickers (editor)
- [x] Sideload APK instructions for tablet
- [x] Export/import JSON (before any second-device pain)

## Phase 1 — Fit real life (daily driver)

Tier 1 (+ settings needed to unlock axes).

- [x] **All tasks library** — browse/edit every pinned task (including not-soon); home stays pending-first
- [x] Settings: default reminder time, global cadence mode, optional default work-start
- [x] Snooze presets (1h / later today / tomorrow / pick)
- [x] Free-window recommendations + quick session chips (15 / 30 / 45 / until work)
- [x] Duration honesty after Done
- [x] Pause / archive task
- [x] Skip this cycle (only after Done/Snooze feel clear — see product map boundaries)

## Phase 2 — Calm power

Tier 2.

- [x] Soft area labels (optional; never required)
- [x] Opt-in morning digest reminder
- [x] Home-screen widget (count + minutes; battery-aware)
- [x] History glance (calm facts, no streaks)
- [x] History hygiene (retention, purge history, reset tasks)
- [x] Exact-alarm permission UX + inexact fallback

Phase 2 is complete.

## Phase 3 — Earn it (core shipped)

Tier 3 core is in tree. Ask before starting anything under **Later**.

- [x] Richer cadence (weekly weekday sets + monthly day-of-month)
- [x] Nth-weekday (“first Saturday”: 1st–4th or last of one weekday)
- [x] Yearly / seasonal (selected months + northern civil season starts)
- [x] Templates / starters for empty state
- [x] Play Store packaging + privacy text (still local-first)
- [x] Multi-device via file / optional user-chosen folder (no account)
- [x] Optional Google Drive App Data (opt-in; needs your OAuth client — [`07-google-sync.md`](./07-google-sync.md))

## Later

Not holes in the daily driver. Implement only if asked.

- [ ] Host privacy policy URL (public HTTPS; repo is private so Pages is blocked) — [`privacy.html`](./privacy.html)
- [ ] Play Console closed beta (Ordinary Tools account, screenshots, AAB) — [`06-play.md`](./06-play.md)

## Explicitly not on the roadmap

See product map **Deferred / never**: streaks, social, family chores, GTD sprawl, sticky FGS, cloud accounts, Windows before Android is boringly solid.
