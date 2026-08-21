# AGENTS.md — Errata

Agent entrypoint for **Errata**: a personal Android (primary) app for recurring mundane upkeep — pin a task, set cadence, estimate duration, remind, complete from a focused pending list.

This file is the session contract. Cursor rules under `.cursor/rules/` are day-to-day coding guardrails. `docs/` is product/architecture authority.

**Not a BancFirst / banking system.** No Host, PCI, or money-path rules. Privacy and battery matter more than enterprise patterns.

---

## What this system is

| Concern | Intent |
|---|---|
| **Recurring tasks** | Anything that slips: grooming, cleaning, filters, paperwork, meds-adjacent *reminders* (not a medical device) |
| **Schedule** | Interval / weekly / monthly / custom cadence — not only “daily habit” |
| **Duration** | Estimated minutes so the pending list can be planned against free time |
| **Reminders** | Reliable enough on old Android; never at the cost of wrecking battery |
| **Pending queue** | Small focused list of due / overdue / upcoming — complete, snooze, skip-with-reason optional later |
| **Platform** | Android first; Windows/desktop is a **non-goal until** Android is solid |

Open **this folder** (`Errata`) as the Cursor workspace so `.cursor/rules/` apply.

---

## Read before you change anything

| Priority | Read | Why |
|---|---|---|
| 1 | This file + `.cursor/rules/` | Non-negotiable guardrails |
| 2 | [`docs/README.md`](docs/README.md) | Doc index |
| 3 | [`docs/00-vision.md`](docs/00-vision.md) | Product intent — do not turn this into Habitica |
| 4 | [`docs/01-architecture.md`](docs/01-architecture.md) | Planned stack, data, reminder model |

Update `docs/` when vision, architecture, reminder strategy, or minSdk changes.

---

## Hard rules (never violate)

1. **Local-first** — Default path stores data on device. No account, no forced cloud, no analytics SDK without an explicit product decision documented in `docs/`.
2. **Battery is a product requirement** — Do not add persistent foreground services, frequent polling, or wake locks “to be safe.” Justify every background path in docs or a rule.
3. **Reminders must be intentional** — Prefer Android-recommended scheduling (e.g. `AlarmManager` exact alarms only where UX requires it, or `WorkManager` for flexible work). Document OEM/Doze caveats; never spam notifications.
4. **Pending-first UX** — Home is the due queue. Charts, streaks, and social are out of scope unless vision is revised.
5. **No streak guilt** — Completing late is normal. Do not punish missed cadences with dark patterns.
6. **Min device reality** — Design for older tablets/phones and sideload installs. Avoid bleeding-edge-only APIs without fallback.
7. **Secrets** — No API keys in git. If a future sync backend appears, credentials via local secure storage / user config only.
8. **One concern per change** — Schema ≠ reminder engine ≠ Compose UI ≠ Windows port.
9. **Windows later** — Do not scaffold a dual-platform monorepo until Android MVP works on device.
10. **Scope fence** — Errata is personal upkeep. It is not a team CMMS (see UpKeep collision), not a lending tracker (Borrowed), not a sales chat (Drift).

---

## Session protocol

1. **Name the concern** — data model, reminder reliability, pending UI, settings, export, or docs.
2. **Check vision** — If the change adds gamification, cloud, or always-on tracking, stop and ask.
3. **Prefer boring stack** — Kotlin, Jetpack (Compose or Views as decided in architecture), Room, minimal deps.
4. **Test on real constraints** — Prefer emulator *and* tablet sideload path; document how to install APK without USB if needed (e.g. local network / downloads).
5. **Update docs** when architecture or reminder policy changes — skip churn for pure copy polish.
6. **Stop at the fence** — New background service, internet permission, account system, or Windows port: **ask first**.

---

## Cursor rules map

| Rule | Scope | When it matters |
|---|---|---|
| `errata-product.mdc` | always | Vision, non-goals, naming |
| `local-first-privacy.mdc` | always | Data stays on device; permissions hygiene |
| `battery-and-reminders.mdc` | always | Alarms, Doze, notification discipline |
| `android-stack.mdc` | Android/Gradle/Kotlin | Project layout, deps, minSdk |
| `pending-queue-ux.mdc` | UI | Due list, complete/snooze, duration |

---

## PR / change checklist

- [ ] **Scope** — Still personal upkeep; no CMMS / streak / social creep
- [ ] **Privacy** — No new internet/analytics/account without docs decision
- [ ] **Battery** — No new persistent background work without justification
- [ ] **UX** — Pending queue remains primary
- [ ] **Docs** — Vision/architecture updated if behavior or stack changed
- [ ] **Device** — Note how this was / will be verified on tablet or emulator

---

## Out of scope for agents (unless explicitly asked)

- Windows / desktop port before Android MVP
- Cloud sync, accounts, or ads
- Habit streaks, social, coaching feeds
- Persistent foreground service for “engagement”
- Rebranding away from Errata without a collision-driven reason
- Large unrelated refactors

---

## Related context (human)

Named **Errata** after UpKeep was unavailable (CMMS trademark/Play collision). Sibling personal play space: JustChats. Work systems (CardCore / XChange / EvolveExtended) are unrelated — do not import banking guardrails here.
