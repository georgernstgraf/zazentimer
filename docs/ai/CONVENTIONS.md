# Conventions

Coding patterns, naming rules, and style agreements for this project.
Follow these without question. Do not deviate unless explicitly told.

## Naming
- Use `ZMT_` prefix for Log tags in ViewModels and Services.

## State Management
- Use `MeditationRepository` for all timer-related state.
- UI components should observe `MeditationRepository.meditationState` via `StateFlow` (collected in ViewModels).
- Never use UI-level polling (e.g. `Handler.postDelayed` or `delay()` loops) to update timer views.

## Testing
- Use `ZazenClock` for all time-related logic.
- Instrumented tests must register `IdlingResourceManager.countingIdlingResource`.
- Prefer `StateFlow` over `LiveData` for new state streams to better support coroutine-based testing.

## Test Infrastructure
- API levels for instrumentation tests are defined in `gradle.properties` (`zazentimer.test.apis`).
- `scripts/run-instrumentation.sh` reads API levels dynamically — never hardcode them.
- Shared test utilities (ScreenRobot, IdlingResource, PreFlightRule) live in `src/testFixtures/`.
- `DevicePreFlightRule` is applied in `HiltTestRunner.onStart()` to ensure screen is awake and animations disabled.
- Android Test utilities use `java-test-fixtures` via `testFixtures { enable = true }` in AGP, NOT the standalone plugin.

## Database
- All asynchronous DB operations in `DbOperations` must be wrapped with `withIdling { ... }` to ensure Espresso synchronization.
