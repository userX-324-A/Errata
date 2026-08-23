# Errata docs

Design authority for the personal upkeep app.

Cursor rules under `.cursor/rules/` are day-to-day coding guardrails. This folder is product/architecture authority. Agent session contract: [`../AGENTS.md`](../AGENTS.md).

## Start here

| Doc | Purpose |
|---|---|
| [../AGENTS.md](../AGENTS.md) | **Agent entrypoint** |
| [00-vision.md](./00-vision.md) | What and why; non-goals |
| [03-product-map.md](./03-product-map.md) | **Feature catalog + cadence/free-window policy** |
| [01-architecture.md](./01-architecture.md) | Android shape, data, reminders |
| [02-roadmap.md](./02-roadmap.md) | Build order (remaining work only) |
| [04-sideload.md](./04-sideload.md) | Build + install APK without Play |

## Principles (do not violate)

1. Corrections on a cadence — not streak hustle.
2. Pending-first UX; free-window is first-class, not a buried filter.
3. Local-first; battery-conscious reminders.
4. Android MVP before any Windows port.
5. No feature without a product-map row and a purpose.
