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
