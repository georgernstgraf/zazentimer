# Architecture

Living structural map of the system as of 2026-05-27.

## Overview
ZazenTimer is an Android application for timing meditation sessions. It uses a foreground service for background timing and a Repository-based architecture to synchronize state between the UI and the service.

## Components
- **MeditationService**: Foreground service managing the `Meditation` state machine and player logic.
- **MeditationRepository**: Singleton state holder providing `StateFlow` updates to both Service and UI.
- **MeditationViewModel**: Bridges the UI and Repository; manages service binding.
- **DbOperations**: Room database wrapper with built-in `IdlingResource` for test synchronization.
- **ZazenClock**: Abstraction for system time to facilitate deterministic testing.
- **BellPlayer**: Manages pooled `Audio` instances for bell playback. Receives `getBellById: suspend (Int) -> BellEntity?` lambda for DB-backed bell resolution. Falls back to `getDemoBell()` if no bell found.
- **BellVolumeConfigDialog**: DialogFragment in session editor for configuring per-bell-type volumes and system alarm volume. Uses Hilt EntryPoints for manual `DbOperations` injection. Controls `AudioManager.STREAM_ALARM` via a seekbar at the top of the dialog.

## Database (Room, V10)
| Table | Key columns | Purpose |
|-------|-----------|---------|
| `bells` | `_id`, `name`, `uri`, `is_builtin` | Bell metadata; FK target for sections and session_bell_volumes |
| `sessions` | `_id`, `name`, `description`, `rank` | Meditation sessions; ordered by rank |
| `sections` | `_id`, `fk_session`, `bellId`, `duration`, `rank`, `bellcount`, `bellpause` | Timed segments; `bellId` FK→bells._id |
| `session_bell_volumes` | `_id`, `fk_session`, `bellId`, `volume` | Per-session per-bell volume; unique on (fk_session, bellId); `bellId` FK→bells._id |

### Bell Resolution Flow (V10)
1. Startup: `MigrationHelper.ensureBellsTableConsistent()` seeds built-in bells via URI (8 rows), syncs custom bells from filesDir, fixes stale URIs, resolves `bellId=0` entries. Runs EVERY startup.
2. Section creation: `DemoSessionCreator` resolves `bellId` via `getBellByUri()` before insert. `DbOperations.insertSection()` defaults bellId=0 to demo bell.
3. Section edit: `SectionEditFragment` resolves bell via `bellId` from DB (no direct field access).
4. Playback: `BellPlayer.playBell()` resolves bell via `getBellById(bellId)` lambda (DB lookup → BellEntity.uri → BellCollection.getBellByUri()); fallback to getDemoBell().
5. Volume: `Meditation.getVolumeForSection()` matches `session_bell_volumes.bellId == section.bellId`
6. UI: `deriveBellVolumesFromSections()` groups by `bellId` only (no bellUri fallback needed)

## Voting API + Frontend (`prisma/voting_api.tsx`)
- **Stack**: Deno + Hono JSX (SSR) + htmx + Pico CSS — alles in einer Datei, kein Build-Schritt.
- **DB-Anbindung**: Lazy PrismaClient Singleton via `lib/prisma.ts` mit WAL Mode.
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
- **Translate Pipeline** (`prisma/translate.ts`): Orchestrator that iterates (model, locale) pairs, dispatches proficiency assessment + translation to opencode server, verifies output, stores votes. Uses `MODEL_PROVIDERS` (per-model provider mapping, replaces `PROVIDER_RANKING`). Seed at `prisma/translations/llmmodels_master.json` (12 models, seed deletes obsolete). Skills at `.opencode/skills/translate/SKILL.md` and `.opencode/skills/proficiency/SKILL.md` (allow reading output files).

## Extracted Helpers (2026-05-11, #142)
- **DemoSessionCreator** (`database/`) — Creates demo sessions on first launch; extracted from ZazenTimerActivity
- **MigrationHelper** — Handles old-version data conversion (bell indices, settings); extracted from ZazenTimerActivity
- **WakeLockManager** (`service/`) — Manages screen wake lock lifecycle; extracted from MeditationViewModel
- **MeditationServiceState** (`service/`) — Static helper for `isServiceRunning()`; extracted from MeditationService
- **EntityMapper** (`database/`) — Maps between BO and Entity types for Room; extracted from DbOperations
- **AlarmScheduler** (`service/`) — Schedules/cancels exact alarms for section transitions; extracted from Meditation
- **BellPlayer** (`service/`) — Manages MediaPlayer lifecycle for bell playback; extracted from Meditation
- **TimerAnimator + AnimationRunner** (`views/`) — Animation state machine; extracted from TimerView

## Test Infrastructure
- **Test Source Sets**:
  - `src/test/` — JVM unit tests with Robolectric
  - `src/androidTest/` — Instrumented tests (emulator/device)
  - `src/testFixtures/` — Shared test utilities (ScreenRobot, MeditationServiceIdlingResource, DevicePreFlightRule) via `java-test-fixtures` plugin
- **Test Runner**:
  - `HiltTestRunner` — Custom `AndroidJUnitRunner` injecting `HiltTestApplication`, with `DevicePreFlightRule.execute()` called in `onStart()` for self-healing tests
   - `scripts/run-instrumentation.sh` — Orchestrates full test matrix: unit tests + per-API-level instrumented tests (API 23-36). Flat early-exit structure (`print_summary(); exit 1` on pre-flight failures), `stdbuf -oL` for pipe output. Launched via `at` scheduler for resilience against shell timeouts.
    - `scripts/summarize-tests.sh` — Parses logs + JUnit XML into markdown report table. Falls back to `Finished N tests`/`OK (N tests)` when Gradle progress lines are incomplete. Usage: `scripts/summarize-tests.sh [--date YYYY-MM-DD] [--markdown]`
- **Emulator Management Scripts** (sourceable libraries):
  - `scripts/start-emulator.sh` — Library: `emulator_launch`, `emulator_wait_boot`, `emulator_x11_prepare`, `emulator_configure_system`, `emulator_resolve_avd`, `emulator_kill_stale`. Standalone: starts emulator for Prisma DB extraction. Sourced-or-executed guard.
  - `scripts/stop-emulator.sh` — Library: `emulator_kill_serial`, `emulator_kill_all`. Standalone: kills emulator + Xvfb. Sourced-or-executed guard.
  - `scripts/create-emulator-snapshots.sh` — Creates clean, app-free emulator snapshots via `-wipe-data` + graceful shutdown. Sources `start-emulator.sh`.
  - `emulator_launch(avd, serial, logfile, ...flags)` — always requires logfile parameter; flags like `-noaudio`, `-no-snapshot-save`, `-wipe-data` passed explicitly.
- **Hostname-Based Configuration**: `run-instrumentation.sh` selects API list and display strategy via `case $(hostname -s)`. Host `claw` forces Xvfb + API 34 only. Host `think` uses X11 or `--force-xvfb`. Unknown hosts skip instrumented tests. API lists in `gradle.properties` via `zazentimer.test.apis.<hostname>`.
- **Execution Strategy**:
  - All APIs 23-36: Gradle `connectedDebugAndroidTest` runner (`gradleMaxApi=36`)
  - The `am instrument` fallback path has been removed — Gradle confirmed working on all tested API levels
  - API level source of truth: `zazentimer.test.apis` in `gradle.properties`
- **Background Launch**:
  - Use `at` scheduler: `echo "cd <dir> && scripts/run-instrumentation.sh --continue-on-error --ignore-dirty-git --debug >/dev/null 2>&1" | at now`
  - Never use `nohup &` from opencode bash tool — tool timeout kills the shell and all children
- **Idling Resources**:
  - `IdlingResourceManager` (prod source) — `CountingIdlingResource` for DB operations
  - `MeditationServiceIdlingResource` (testFixtures) — Custom `IdlingResource` for service binding state
- **Self-Healing**: `DevicePreFlightRule` in `HiltTestRunner.onStart()` ensures screen is awake, unlocked, and animations disabled before any test runs

## Data Flows
- **User Actions** → `MeditationViewModel` → `MeditationService` → `Meditation` logic.
- **Meditation Logic** → `MeditationRepository` → `StateFlow` updates.
- **StateFlow** → `MeditationViewModel` → UI (Fragments).
- **Service** → `DbOperations` (Read/Write session state).
- **ViewModel** → `DbOperations` (Read session list/configuration).
- **Bell Volume Flow**: Section `bellId` → `session_bell_volumes` lookup via `bellId` → `Meditation.getVolumeForSection()` → `BellPlayer.playBells(section, volume)` → DB lookup `getBellById(section.bellId)` → `BellCollection.getBellByUri(entity.uri)` → `Audio.playAbsVolume(bell, volume)`.
- **Bell Volume Config**: `BellVolumeConfigDialog` → reads `session_bell_volumes` → finds bell via `bellEntities[bv.bellId]` → displays bell name from `bells` table → saves to `DbOperations.saveBellVolumes()`.
- **Bell Import Repair**: `BackupManager.restore()` → Room migrations (3→4→5→6→7) → MIGRATION_6_7 seeds bells from existing URIs → `ensureBellsTableConsistent()` fixes stale URIs at next startup.

## Play Store Automation (`scripts/play_store/`)
- **`setup.sh`**: Bootstraps the local `.venv` environment from `requirements.txt`.
- **`check_status.py`**: Queries the Google Play Android Publisher API for current track and release states.
- **`update_notes.py`**: Updates release notes for specific tracks and languages using the API.
- **`.venv/`**: Local Python virtual environment (gitignored).

## Knowledge Files (`docs/ai/`)
| File | Purpose | Update mode |
|------|---------|------------|
| HANDOFF.md | Open tasks for next session | Overwrite |
| DECISIONS.md | Chronological record of choices | Append |
| ARCHITECTURE.md | Living structural map | Overwrite |
| CONVENTIONS.md | Ongoing rules to follow | Append |
| PITFALLS.md | Hard-won failure knowledge | Append |
| DOMAIN.md | Business/domain rules | Append |
| STATE.md | Current project status | Overwrite |
| PLAY_STORE_SETUP.md | Setup guide for automation scripts | Append |
