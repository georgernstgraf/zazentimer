# Decisions

Architectural and technical decisions made in this project.
Each entry documents WHAT was decided and WHY.

## 2026-05-10: State Management via Singleton Repository
- **Choice**: Moved meditation state from `MeditationService` and UI-level polling in `MeditationViewModel` to a singleton `MeditationRepository`.
- **Reason**: Stabilize state across fragment lifecycles and eliminate UI-driven polling races that caused flakiness in Espresso tests.
- **Considered**: Keeping state in Service (suffered from binding races), using local ViewModel state (lost on navigation).
- **Tradeoff**: Repository must be carefully synchronized; state is global to the app process.

## 2026-05-10: Time Abstraction via ZazenClock
- **Choice**: Introduced `ZazenClock` interface and `SystemClock` implementation.
- **Reason**: Decouple business logic from `System.currentTimeMillis()` to allow deterministic time-travel in tests.
- **Considered**: Static mocks (brittle), manual offset injection.
- **Tradeoff**: Slight increase in boilerplate for dependency injection.

## 2026-05-10: Database Idling via CountingIdlingResource
- **Choice**: Wrapped all `DbOperations` methods with `CountingIdlingResource` increments/decrements.
- **Reason**: Enable Espresso to automatically wait for asynchronous database operations without manual `Thread.sleep()`.
- **Considered**: `IdlingThreadPoolExecutor` (doesn't catch all Room async work), manual waits in tests.
- **Tradeoff**: All DB operations now depend on the idling resource infrastructure.

## 2026-05-10: Script-Based Test Infrastructure over Gradle Managed Devices
- **Choice**: Use `scripts/run-instrumentation.sh` with dynamic AVD resolution instead of Gradle Managed Devices (GMD).
- **Reason**: Heterogeneous developer environments have varying AVD naming conventions (test_apiX, Medium_Phone_API_X, etc.). GMD enforces a single naming scheme, breaking local developer workflows.
- **Considered**: Pure GMD approach (inflexible for mixed environments), ad-hoc bash without Gradle integration (no source of truth).
- **Tradeoff**: Manual emulator lifecycle management in the script; less IDE integration for test launching.

## 2026-05-10: am instrument Fallback for API 31+
- **Choice**: Use `am instrument` CLI command for API 31+ instead of the Gradle UTP runner.
- **Reason**: AGP's Unified Test Platform runner has a known bug on API 35 (and intermittently on 31-34) reporting "0 tests found". `am instrument` bypasses this reliably.
- **Considered**: Staying with Gradle runner and accepting flakiness (unacceptable), using only Gradle runner on all APIs (fails on 35).
- **Tradeoff**: Slightly more complex APK installation flow; extra retry logic for focus errors.

## 2026-05-11: Full detekt cleanup — fix all violations, not suppress
- **Choice**: Fixed all 337 detekt violations across 22 source files instead of adjusting threshold config.
- **Reason**: User wanted proper code quality fixes, not suppressed warnings. Extracted 10 helper classes, ~160 MagicNumbers to named constants, and removed dead code.
- **Considered**: Adjusting `detekt.yml` thresholds (quick but hides problems), selective fixes (partial).
- **Tradeoff**: Large diff (~2400 insertions, ~1450 deletions); 9 new files; risk of regressions in test infra during parallel agent work.

## 2026-05-11: POST_NOTIFICATIONS runtime permission for targetSdk 36
- **Choice**: Added `POST_NOTIFICATIONS` manifest permission + runtime request in `ZazenTimerActivity.onStartPressed()` gated on API 33+.
- **Reason**: targetSdk=36 requires this permission for foreground service notifications; missing it causes guaranteed crash on API 33+.
- **Considered**: Auto-grant only (test-only hack), always-deny handling (blocks meditation start).
- **Tradeoff**: Permission request dialog appears on first meditation start; user can deny and still proceed.

## 2026-05-11: HOST execution mode for instrumented tests
- **Choice**: Changed `execution` from `"ANDROIDX_TEST_ORCHESTRATOR"` to `"HOST"` in `build.gradle.kts`.
- **Reason**: The `am instrument` path doesn't install the orchestrator APK; `HOST` mode matches this reality. Also avoids orchestrator instability (SIGKILL, `clearPackageData` races) on some API levels.
- **Considered**: Installing orchestrator manually in script (complex), keeping orchestrator and accepting flakiness.
- **Tradeoff**: No process-level test isolation; potential state leakage between tests (mitigated by explicit `resetDatabaseForTest()` in `@Before`).

## 2026-05-11: gradleMaxApi=30 — Gradle runner for 29-30, am instrument for 31+
- **Choice**: Set `zazentimer.test.gradleMaxApi=30` in `gradle.properties`.
- **Reason**: Gradle runner (`connectedDebugAndroidTest`) works reliably on API 29-30. API 31+ emulators have split APK install races and UTP bugs. `am instrument` with streamed install is more stable.
- **Considered**: gradleMaxApi=34 (API 34 agent recommended for orchestrator support), gradleMaxApi=29 (too aggressive, API 30 Gradle runner works).
- **Tradeoff**: API 31-36 tests use manual APK install + `am instrument` flow (slightly slower but more reliable).
