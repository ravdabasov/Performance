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

## Architecture

- **Delivery model**: one HTML file. Everything (CSS, JS, markup) is
  embedded, organized under clearly labeled `SECTION:` comment blocks
  mirroring the spec's requested module list.
- **No ES modules**: `type="module"` scripts are blocked by CORS when a
  file is opened via `file://` in most browsers. All engines are plain
  scripts namespaced under one `App` object instead, to guarantee
  double-click offline compatibility.
- **Icons**: the spec asks for both "Font Awesome" and "zero external
  runtime resources." Rather than embed a multi-hundred-KB base64 webfont,
  this build uses a small self-authored inline SVG icon set (`App.Icons`) —
  same visual role, zero network calls, zero licensing concerns.
- **Fonts**: `Inter, Segoe UI, Roboto, Arial, sans-serif` — degrades to the
  OS-native fallback the spec itself lists; nothing ever fetches a font
  over the network.
- **State**: `App.State` is a minimal centralized store (get/set/subscribe).
  Every engine reads from and writes to it — no engine keeps private UI
  state that another engine can't see.
- **Storage layering** (per spec): business data will live in IndexedDB
  (Phase 2); only UI-level preferences and the session token use
  LocalStorage/SessionStorage (`App.LocalStore`).

### Engines implemented in Phase 1

| Engine | Responsibility |
|---|---|
| `App.Config` | Single source of truth: roles, permissions, nav items, storage keys. Nothing elsewhere hardcodes these. |
| `App.I18n` | AZ/TR dictionaries, `t(key)` lookup, live DOM translation via `data-i18n*` attributes — no visible string is ever hardcoded in markup. |
| `App.Theme` | Dark/light theme via CSS custom properties; instant switch, no reload. |
| `App.Auth` | Login/logout, SHA-256 password comparison, expiring session (LocalStorage if "remember me", SessionStorage otherwise), session restore on reopen. |
| `App.Authz` | Permission checks (`can`, `isAdmin`, `guard`). Nav items requiring a permission the user lacks are never inserted into the DOM — not just visually hidden. |
| `App.Notify` | Toast notifications (success/info/warning/error), auto-hide + manual close, queued. |
| `App.Dialog` | Modal dialogs (confirmation/warning/success/information/error/question), used e.g. for the "unauthorized action" message. |
| `App.Shell` | Sidebar/header/footer rendering, routing between nav pages, theme/language button sync. |

## Security note (required disclosure)

Passwords are compared as SHA-256 digests, never stored or checked as
plaintext — this is the strongest *client-side* safeguard reasonably
available. It is **not** real security: this is a fully offline,
source-visible application, so anyone with browser devtools can read the
logic and brute-force one of two known strings. Genuine access control
requires a server, which is explicitly out of scope for this project.

## Sample / demo data

Not included in Phase 1 (no data layer exists yet). Once Phase 2 (Import
Engine) lands, an optional, clearly-labeled **"Load Sample Data"**
administrator-only toggle will be added for testing — separate from, and
never mixed with, real imported data.

## Testing performed for this phase

Verified with a headless-Chromium script (Playwright) covering: login
success/failure for both roles, password show/hide, Enter-to-submit,
remember-me vs. session-only persistence (including across a real app
"reopen" in a fresh browser context), theme switching, language switching
without reload, sidebar collapse persistence, logout, and permission-based
nav filtering (Standard User never receives the Settings nav item in the
DOM). Zero console errors, zero uncaught exceptions.
