# Architecture

Living structural map of the system as of 2026-05-17.

## Overview
ZazenTimer is an Android application for timing meditation sessions. It uses a foreground service for background timing and a Repository-based architecture to synchronize state between the UI and the service.

## Components
- **MeditationService**: Foreground service managing the `Meditation` state machine and player logic.
- **MeditationRepository**: Singleton state holder providing `StateFlow` updates to both Service and UI.
- **MeditationViewModel**: Bridges the UI and Repository; manages service binding.
- **DbOperations**: Room database wrapper with built-in `IdlingResource` for test synchronization.
- **ZazenClock**: Abstraction for system time to facilitate deterministic testing.
- **BellPlayer**: Manages pooled `Audio` instances for bell playback. Receives explicit `volume: Int` parameter (volume is per-session, per bell type). Has demo bell fallback if `getBellForSection()` returns null.
- **BellVolumeConfigDialog**: DialogFragment in session editor for configuring per-bell-type volumes.

## Database (Room, V7)
| Table | Key columns | Purpose |
|-------|-----------|---------|
| `bells` | `_id`, `name`, `uri`, `is_builtin`, `resource_name` | Bell metadata; FK target for sections and session_bell_volumes |
| `sessions` | `_id`, `name`, `description` | Meditation sessions |
| `sections` | `_id`, `fk_session`, `bell`, `belluri`, `bell_id`, `duration`, `rank`, `bellcount`, `bellpause` | Timed segments within a session; `bell_id` FK→bells._id |
| `session_bell_volumes` | `_id`, `fk_session`, `bell`, `belluri`, `bell_id`, `volume` | Per-session per-bell volume; `bell_id` FK→bells._id |
| `settings` | `_id`, `param`, `value`, `def` | App settings (key-value) |

### Bell Resolution Flow
1. Startup: `MigrationHelper.ensureBellsTableConsistent()` seeds built-in bells (8 rows), syncs custom bells from filesDir, fixes stale URIs, resolves `bell_id=0` entries
2. Section creation: `DemoSessionCreator` / `SectionEditFragment` set `bell=-2` (BELL_INDEX_NONE) + valid `bellUri` from BellCollection; `bellId` left as 0
3. Playback: `BellPlayer.playBell()` resolves bell via `BellCollection.getBellForSection()` (URI match); fallback to `getDemoBell()` if null
4. Volume: `Meditation.getVolumeForSection()` matches `session_bell_volumes.bell_id == section.bell_id`

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
- **Bell Volume Flow**: Section `bell_id` → `session_bell_volumes` lookup via `bell_id` → `Meditation.getVolumeForSection()` → `BellPlayer.playBells(section, volume)` → `Audio.playAbsVolume(bell, volume)`.
- **Bell Volume Config**: `BellVolumeConfigDialog` → reads `session_bell_volumes` → displays bell name from `bells` table → saves to `DbOperations.saveBellVolumes()`.
- **Bell Import Repair**: `BackupManager.restore()` → Room migrations (3→4→5→6→7) → MIGRATION_6_7 seeds bells from existing URIs → `ensureBellsTableConsistent()` fixes stale URIs at next startup.

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
