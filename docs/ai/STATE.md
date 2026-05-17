# Project State

Current status as of 2026-05-17.

## Current Focus
#183 — Emulator test infrastructure stabilized for API 36. Remaining test failures are Espresso timing issues in individual test methods, not infrastructure.

## Completed (this cycle)
- [x] API 36 emulator test infrastructure fixes:
  - Service readiness check after `boot_completed` (`service check activity`)
  - 30s service stabilization (3x consecutive checks at 10s intervals)
  - Skip package cleanup on API 36+ (was killing system services via Broken Pipe cascade)
  - `testTimeoutSeconds=120` in Gradle instrumentation runner args
  - Test manifest with `WRITE_EXTERNAL_STORAGE` for API ≤32
  - `animationsDisabled = true` in testOptions
- [x] Removed all 18 `SystemClock.sleep` calls from instrumented tests:
  - Replaced with `Espresso.onIdle()` for setup waits
  - Replaced with `UiAutomator Until.hasObject()` for polling loops
  - Removed `onIdle()` from `@Before` methods (caused `TestLooperManager already held`)
- [x] Created `AbstractZazenTest` base class with `Timeout(2, MINUTES)`, `hiltRule`, `activityRule`
- [x] Refactored all 8 test classes to extend `AbstractZazenTest`
- [x] `IdlingPolicies` timeout reduction in `HiltTestRunner.onStart()`
- [x] Created `scripts/kill-test-run.sh` and `scripts/summarize-tests.sh`
- [x] Knowledge persisted

## Pending
- [ ] Fix remaining 7 Espresso test failures on API 36 (test code quality, not infrastructure)
- [ ] Apply same infrastructure fixes to API 23-35 (service check, stabilization, cleanup skip)
- [ ] Full validation run APIs 23-36
- [ ] #64 — Play Store (blocked by missing `PLAY_SERVICE_ACCOUNT_JSON` secret)

## Blockers
None

## Next Session Suggestion
1. Fix the 7 failing tests on API 36 — mostly `MainScreenNavigationTest` cascading from `MainScreenDeadStateTest.testButStartEnabledAfterStoppingMeditation` timeout
2. Run `scripts/run-instrumentation.sh --api 36` to verify
3. Apply fixes to all APIs: `scripts/run-instrumentation.sh` (full matrix)
