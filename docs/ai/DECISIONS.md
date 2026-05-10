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
