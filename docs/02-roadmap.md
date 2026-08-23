# Roadmap — Errata

Remaining work only. Update as slices ship.  
**Why** for each item: [`03-product-map.md`](./03-product-map.md). Do not add features that lack a map row.

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

- [ ] Soft area labels (optional; never required)
- [ ] Opt-in morning digest reminder
- [ ] Home-screen widget (count + minutes; battery-aware)
- [ ] History glance (calm facts, no streaks)
- [ ] Exact-alarm permission UX + inexact fallback

## Phase 3 — Earn it later

Tier 3 — only if Phase 0–2 are solid on device.

- [ ] Richer cadence (weekly / monthly / seasonal)
- [ ] Templates / starters for empty state
- [ ] Play Store packaging + privacy text (still local-first)
- [ ] Multi-device via file / optional user-chosen folder (no account)

## Explicitly not on the roadmap

See product map **Deferred / never**: streaks, social, family chores, GTD sprawl, sticky FGS, cloud accounts, Windows before Android is boringly solid.
