# Architecture

Living structural map of the system as of 2026-05-10.

## Overview
ZazenTimer is an Android application for timing meditation sessions. It uses a foreground service for background timing and a Repository-based architecture to synchronize state between the UI and the service.

## Components
- **MeditationService**: Foreground service managing the `Meditation` state machine and player logic.
- **MeditationRepository**: Singleton state holder providing `StateFlow` updates to both Service and UI.
- **MeditationViewModel**: Bridges the UI and Repository; manages service binding.
- **DbOperations**: Room database wrapper with built-in `IdlingResource` for test synchronization.
- **ZazenClock**: Abstraction for system time to facilitate deterministic testing.

## Test Infrastructure
- **Test Source Sets**:
  - `src/test/` — JVM unit tests with Robolectric
  - `src/androidTest/` — Instrumented tests (emulator/device)
  - `src/testFixtures/` — Shared test utilities (ScreenRobot, MeditationServiceIdlingResource, DevicePreFlightRule) via `java-test-fixtures` plugin
- **Test Runner**:
  - `HiltTestRunner` — Custom `AndroidJUnitRunner` injecting `HiltTestApplication`, with `DevicePreFlightRule.execute()` called in `onStart()` for self-healing tests
  - `scripts/run-instrumentation.sh` — Orchestrates full test matrix: unit tests + per-API-level instrumented tests (API 29-35)
- **Execution Strategy**:
  - API 29-30: Gradle `connectedDebugAndroidTest` runner
  - API 31-35: Manual `am instrument` (bypasses UTP bug on API 35+)
  - API level source of truth: `zazentimer.test.apis` in `gradle.properties`
  - Gradle runner threshold: `zazentimer.test.gradleMaxApi` in `gradle.properties`
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
