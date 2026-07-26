# Enterprise Performance Management Dashboard

Offline, single-file Business Intelligence platform for the Performance
Management Department — sales, bonus/KPI, and forecast analytics. No
backend, no cloud, no build step: open `index.html` directly in a browser.

## Status: Phase 1 — Foundation (complete)

This is an incremental build. Each phase below is either **complete** (fully
functional, production-quality, no placeholders) or **scheduled** (not yet
started — visiting its nav item shows an honest "scheduled" state rather
than fake data).

| Phase | Scope | Status |
|---|---|---|
| 1 | Configuration, Theme Engine, Language Engine, Authentication, Authorization, App Shell | **Complete** |
| 2 | IndexedDB, Excel Import Wizard, Validation, Normalization, Version History | Scheduled |
| 3 | Formula Engine, Business Rule Engine, Filter Engine | Scheduled |
| 4 | Dashboard KPIs, Charts, Tables, Tree Grid | Scheduled |
| 5 | Sales / Bonus / Forecast / Comparison Center / Rankings / Trend Analysis | Scheduled |
| 6 | AI Insight Engine, Export, Backup, KPI/Bonus Rule Config Center, Reports | Scheduled |
| 7 | Performance optimization, accessibility, error handling, memory | Scheduled |
| 8 | Quality assurance / regression testing | Scheduled |

## Running it

Open `index.html` in any modern browser (Chrome/Edge recommended). No
server, no internet connection, no dependencies required.

## Credentials (client-side only — see security note below)

| Role | Password |
|---|---|
| Administrator | `Performance2026.` |
| Standard User | `PSD2026.Performance` |

## Security note (required disclosure)

Passwords are compared as SHA-256 digests, never stored or checked as
plaintext — this is the strongest *client-side* safeguard reasonably
available. It is **not** real security: this is a fully offline,
source-visible application, so anyone with browser devtools can read the
logic and brute-force one of two known strings. Genuine access control
requires a server, which is explicitly out of scope for this project.
