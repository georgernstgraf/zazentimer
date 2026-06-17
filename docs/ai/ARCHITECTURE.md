# Architecture

Living structural map of the system as of 2026-06-14.

## Overview
ZazenTimer is an Android application for timing meditation sessions. It uses a foreground service for background timing and a Repository-based architecture to synchronize state between the UI and the service.

## Components
- **MeditationService**: Foreground service managing the `Meditation` state machine and player logic.
- **MeditationRepository**: Interface implemented by `DbMeditationRepository` (singleton state holder providing `StateFlow` updates).
- **MeditationViewModel**: Bridges the UI and Repository; manages service binding.
- **DbOperations**: Room database wrapper with built-in `IdlingResource` for test synchronization.
- **ZazenClock**: Abstraction for system time to facilitate deterministic testing.
- **BellPlayer**: Interface implemented by `BellPlayerManager` (manages pooled `Audio` instances for bell playback).
- **AlarmScheduler**: Interface implemented by `SystemAlarmScheduler` (schedules/cancels exact alarms).
- **BellVolumeConfigDialog**: DialogFragment in session editor for configuring per-bell-type volumes and system alarm volume. Uses Hilt EntryPoints for manual `DbOperations` injection. Controls `AudioManager.STREAM_ALARM` via a seekbar at the top of the dialog.

## Database (Room, V2)

| Table | Key columns | Purpose |
|---|---|---|
| `bells` | `id`, `name`, `uri`, `is_builtin` | Bell metadata; FK target for sections and session_bell_volumes |
| `sessions` | `id`, `name`, `description`, `rank` | Meditation sessions; ordered by rank |
| `sections` | `id`, `fk_session`, `bellId`, `duration`, `rank`, `bellcount`, `bellpause` | Timed segments; `bellId` FK→bells.id, `fk_session` FK→sessions.id ON DELETE CASCADE |
| `session_bell_volumes` | `id`, `fk_session`, `bellId`, `volume` | Per-session per-bell volume; unique on (fk_session, bellId); FKs to sessions and bells |

### Bell Resolution Flow (V2)
1. **Startup**: `DbOperations.sanitizeBellUris()` seeds the 8 built-in bells from `BuiltinBells.definitions()` (pure config object, no in-memory state), syncs custom bells from filesDir, fixes stale URIs, and resolves any unresolvable entries. Runs EVERY startup inside `ZazenTimerActivity.onCreate()` lifecycleScope.
2. **Demo bell lookup**: `BellDao.getBuiltinByName(name)` finds the demo bell by its localized name (`BuiltinBells.DEMO_BELL_NAME_RES`). Exposed via `BellRepository.getDemoBell()` and `DbOperations.getDemoBell()`. `fallbackBellId()` throws `IllegalStateException` if no builtin bell exists (no silent `0` FK corruption).
3. **Section creation**: `DemoSessionCreator` resolves `bellId` via `getBellByUri(BuiltinBells.resourceUri(...))` before insert. `DbOperations.insertSection()` defaults bellId=0 to demo bell.
4. **Section edit**: `SectionEditFragment` resolves bell via `bellId` from DB; bell list is populated from `dbOperations.getAllBells()`.
5. **Playback**: `BellPlayer.playBell()` resolves bell via `getBellById(bellId)` lambda → uses `BellEntity.uri` directly; fallback to demo bell URI from `BuiltinBells.resourceUri(context, DEMO_BELL_RAW_RES)`.
6. **Volume**: `Meditation.getVolumeForSection()` matches `session_bell_volumes.bellId == section.bellId`
7. **UI**: `deriveBellVolumesFromSections()` groups by `bellId` only.

---

## Voting API + Frontend (`prisma/voting_api.tsx`)
- **Stack**: Deno + Hono JSX (SSR) + htmx + Pico CSS — alles in einer Datei, kein Build-Schritt.
- **DB-Anbindung**: Lazy PrismaClient Singleton via `lib/prisma.ts` mit WAL Mode.
- **Settlement-Logik**: `lib/settlement.ts` (pure, DB-free) hält `SETTLED_SCORE_THRESHOLD` + `isSettled(score)` als Single Source of Truth; `db.ts` re-exportiert es, `voting_api.tsx` nutzt `isSettled(·.score)`. Erste unter `deno test` lauffähige Unit (`lib/settlement.test.ts`) — pure Module unter `prisma/lib/` dürfen `db.ts` nicht importieren (dessen top-level `await getPr()` eine DB-Verbindung öffnet).
- **REST Endpoints** (JSON):
  - `GET /api/stats` — Dashboard-Zahlen
  - `GET /api/models` — Alle LLM-Modelle
  - `GET /api/models/:id/proficiencies` — Proficiency-Level pro Sprache (via M:N-Junctions)
  - `GET /api/languages` — Alle Sprachen
  - `GET /api/strings?search=` — Master-Strings mit Suche
  - `GET /api/models/:mid/languages/:lid/votes` — Übersetzungen gruppiert pro String
  - `GET /api/models/:mid/languages/:lid/coverage` — Coverage-Statistik
  - `GET /api/strings/:sid/comparison?langId=` — Side-by-Side Modellvergleich
  - `POST /api/votes` — Vote erstellen
- **Frontend-Seiten** (JSX + htmx):
  - `/` — Dashboard mit Stat-Kacheln
  - `/models` — Dropdown → htmx-fragment: Proficiency-Tabelle + Coverage-Balken
  - `/models/:mid/languages/:lid` — Detail: alle Übersetzungen (Model→Sprache)
  - `/strings` — String-Suche (htmx keyup delay:300ms)
  - `/strings/:sid/comparison?langId=` — Side-by-Side mit Consensus-Markierung
- **htmx-Fragmente**:
  - `/models/proficiencies/table?modelId=` — Tabelle (Level, Coverage, Link)
  - `/strings/table?search=` — Suchergebnisse
  - `/strings/:sid/comparison/table?langId=` — Vergleichstabelle

## Prisma Schemas
Two Prisma-managed SQLite schemas coexist under `prisma/`:
- **Device DB** (`prisma/desired/` + `prisma/current/`): Documents the Room-managed app database. `desired/` is hand-crafted (SOLL), `current/` is auto-generated from device via `prisma db pull` (IST). Drift detection via `prismaCheckSchema` Gradle task.
- **Translation DB** (`prisma/translations/`): Stores multi-LLM translation candidates and voting results for the app's 123 locale files. 4 models: locales, strings, translations, votes. No auto-generation — schema evolves by hand. Validation via `prismaValidateTranslationsSchema` Gradle task.
- **Translate Pipeline** (`prisma/translate.ts`): Orchestrator that iterates (model, locale) pairs, dispatches proficiency assessment + translation to opencode server, verifies output, stores votes. `MODEL_PROVIDERS` built at import time from `prisma/translations/llmmodels_master.json`.

## Extracted Helpers
- **DemoSessionCreator** (`database/`) — Creates demo sessions on first launch; extracted from ZazenTimerActivity
- **WakeLockManager** (`service/`) — Manages screen wake lock lifecycle; extracted from MeditationViewModel
- **MeditationServiceState** (`service/`) — Static helper for `isServiceRunning()`; extracted from MeditationService
- **EntityMapper** (`database/`) — Maps between BO and Entity types for Room; extracted from DbOperations
- **AlarmScheduler** (`service/`) — Interface implemented by `SystemAlarmScheduler` (schedules/cancels exact alarms for section transitions; extracted from Meditation)
- **BellPlayer** (`service/`) — Interface implemented by `BellPlayerManager` (manages MediaPlayer lifecycle for bell playback; extracted from Meditation)
- **TimerAnimator + AnimationRunner** (`views/`) — Animation state machine; extracted from TimerView

---

## Test Infrastructure & UI Test Plan

The system utilizes unit, integration, and instrumented UI tests.

### Test Source Sets
- `src/test/` — JVM unit tests with Robolectric and MockK
- `src/androidTest/` — Instrumented tests on real devices or emulators
- `src/testFixtures/` — Shared test utilities (ScreenRobot, MeditationServiceIdlingResource, DevicePreFlightRule)

### Test Runner & Scripting
- **`HiltTestRunner`**: Custom runner invoking `HiltTestApplication`, with `DevicePreFlightRule.execute()` called on start.
- **`run-instrumentation.sh`**: Main script orchestrating full matrix: unit tests + per-API-level instrumented tests (API 23-36). Runs emulators, manages logs under `logs/`, and utilizes Xvfb for headless runs.
- **`summarize-tests.sh`**: Parses JUnit XML and log output into markdown report summaries.

### UI Test Matrix (Single Source of Truth)

All critical UI workflows and edge cases are mapped here.
Status Legend: 🔴 Not Automated | 🟡 Partially Automated | 🟢 Fully Automated

| Scenario / Goal | Steps / Interventions | Expected Outcome | Bugs/Regressions Addressed | Status |
| :--- | :--- | :--- | :--- | :--- |
| **Fresh Launch** | Launch app first time | Default sessions created; "Zazen Meditation Timer" visible | #8 Crash on legacy styles | 🟢 |
| **Start Timer** | Select session -> Press Start | FGS starts; transitions to active view; countdown begins | PendingIntent mutability | 🟢 |
| **Rotation** | Rotate device | UI scales properly; selected session stays selected | State loss | 🟢 |
| **Copy Session** | Card Menu -> Copy | Deep-copied session created with "(Copy)" suffix | #52 SQLiteConstraintException | 🟢 |
| **Delete Session** | Card Menu -> Delete -> Confirm | Session and its sections/volumes removed from list | UI drift | 🟢 |
| **Metadata Edit** | Rename / change desc -> Back | Metadata saved in DB; updated on main screen | Stale lists | 🟢 |
| **Add Section** | Click "Add Section" overflow option | Navigates to Section Edit screen | Legacy FAB crash | 🟢 |
| **Play Sound Test** | Tap "Play" icon in section editor | Gong sound plays at current configured volume | Sound channel leak | 🟢 |
| **Theme Toggle** | Settings -> Dark Theme | Switch app style immediately without crashes | `DialogPreference` crash | 🟢 |
| **Backup / Restore** | SAF dialog backups / restores | Backup created at /sdcard/Download/; DB overwrites sync WAL/SHM | WAL/SHM corruption; fsync sync | 🟢 |
| **Back Nav** | Press back arrow during running timer | Navigation is blocked (only Stop allows exit) | Back nav loopholes | 🟢 |

---

## Play Store Automation Setup

Automation scripts are located in `scripts/play_store/` and communicate with the Google Play Developer API.

### Local Setup
1. Run the setup script to bootstrap the virtual environment and dependencies:
   ```bash
   ./scripts/play_store/setup.sh
   ```
2. Place the Cloud Service Account JSON key at `google/play-api-key.json` within the project. This path is gitignored for security.

### Available Scripts
All operations use the python interpreter from the local `.venv`:

- **Check Release Status**: Lists active tracks, version codes, and current release notes.
  ```bash
  .venv/bin/python3 scripts/play_store/check_status.py
  ```
- **Update Release Notes**: Updates release text for a specific track.
  ```bash
  .venv/bin/python3 scripts/play_store/update_notes.py <track> "<notes>" [language]
  ```
  - `<track>`: `alpha`, `internal`, `beta`, `production`
  - `[language]`: Optional, defaults to `de-DE`

### CI/CD Pipeline
For GitHub Actions runners, the raw Service Account JSON key content must be stored in a GitHub repository Secret named `PLAY_SERVICE_ACCOUNT_JSON` to allow automated production tracking.

---

## Knowledge Files (`docs/ai/`)

| File | Purpose | Update mode |
|---|---|---|
| `HANDOFF.md` | Open tasks for next session | Overwrite |
| `DECISIONS.md` | Chronological record of choices currently in force | Append; prune superseded → HISTORY.md |
| `ARCHITECTURE.md` | Living structural map (this file) | Overwrite |
| `CONVENTIONS.md` | Ongoing rules, conventions, and style boundaries | Append; prune superseded → HISTORY.md |
| `PITFALLS.md` | Hard-won failure knowledge (permanent constraints) | Append; prune fixed bugs → HISTORY.md |
| `DOMAIN.md` | Core business logic, terms, and context | Append |
| `STATE.md` | Point-in-time status of the project | Overwrite |
| `HISTORY.md` | Superseded entries archive (chronological audit log) | Append-only |
