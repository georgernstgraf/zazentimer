# Handoff

## Current Branch
`main` (Trunk-based development)

## Open Tasks
1. [ ] **#183 — Fix 7 remaining test failures on API 36**: These are test-code issues, not infrastructure. Root cause: `MainScreenDeadStateTest.testButStartEnabledAfterStoppingMeditation` times out at 2min and leaves Espresso in a bad state, causing `TestLooperManager already held` cascade in `MainScreenNavigationTest` (5 tests). Also `MeditationServiceTest.testStopMeditationConfirmation` times out. Files: `app/src/androidTest/kotlin/at/priv/graf/zazentimer/MainScreenDeadStateTest.kt`, `MainScreenNavigationTest.kt`, `MeditationServiceTest.kt`.
2. [ ] **#183 — Apply infrastructure fixes to API 23-35**: The service check, stabilization, and package-cleanup-skip are currently only tested on API 36. Need full matrix run to verify all APIs.
3. [ ] **#64 — Play Store**: Blocked by missing `PLAY_SERVICE_ACCOUNT_JSON` GitHub secret.

## AVD Inventory (all installed)
`test_api23` `test_api24` `test_api25` `test_api26` `test_api27` `test_api28` `test_api29` `test_api30` `test_api31` `test_api32` `test_api33` `test_api34` `test_api35` `test_api36`

## API 36 Test Results (2026-05-17, latest run)
- **17/24 PASS**, 7 FAIL
- Passes: BackupRestoreTest (2), DuplicateSessionTest (2), MainScreenDeadStateTest (1 of 2), MainScreenNavigationTest (0 of 5 — all cascading), MeditationServiceTest (1 of 2), SectionEditTest (3), SessionCrudTest (3), SettingsTest (5)
- Fails: `MainScreenDeadStateTest.testButStartEnabledAfterStoppingMeditation` (2min timeout), `MainScreenNavigationTest` (5 tests, TestLooperManager cascade), `MeditationServiceTest.testStopMeditationConfirmation` (2min timeout)

## Key Files This Session
- `app/src/androidTest/kotlin/at/priv/graf/zazentimer/AbstractZazenTest.kt` — base class with Timeout rule
- `app/src/androidTest/kotlin/at/priv/graf/zazentimer/HiltTestRunner.kt` — IdlingPolicies timeout
- `app/src/androidTest/AndroidManifest.xml` — NEW, storage permissions for test APK
- `app/build.gradle.kts` — testTimeoutSeconds=120, animationsDisabled
- `scripts/run-instrumentation.sh` — service check, stabilization, package cleanup skip
- `scripts/kill-test-run.sh` — kill all test processes
- `scripts/summarize-tests.sh` — markdown test report

## How to Launch Long-Running Tests
```bash
echo "scripts/run-instrumentation.sh --api 36" | at now
```
Check: `atq` (queue), `ps aux | grep run-instrument` (running), `tail logs/instrumentation-$(date +%Y-%m-%d).log` (progress)

## CI Status
- Release AAB build step fails on all commits (pre-existing, related to #64 release pipeline)
- Unit tests and lint pass locally
