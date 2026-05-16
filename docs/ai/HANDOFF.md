# Handoff

## Current Branch
`main` (Trunk-based development)

## Open Tasks
1. [ ] **Full instrumentation test run (APIs 23-36)**: Script updated with Xvfb-per-API restart. All 14 AVDs installed. Run with `--continue-on-error --ignore-dirty-git` to collect all failures, then save to `logs/test-failures-YYYY-MM-DD.md`.
2. [ ] **#64 — Play Store**: Sub-issues #114 (AAB build) and #113 (privacy/legal). Blocked by missing `PLAY_SERVICE_ACCOUNT_JSON` GitHub secret.

## AVD Inventory (all installed)
`test_api23` `test_api24` `test_api25` `test_api26` `test_api27` `test_api28` `test_api29` `test_api30` `test_api31` `test_api32` `test_api33` `test_api34` `test_api35` `test_api36`

## Known Test Failures (from partial runs)
- **API 31-33**: `SettingsTest.testRestore` — `NoMatchingViewException: No views found matching "Restore from Backup"`. Likely the Settings screen scrolls and the item is off-screen.
- **API 35**: Process crash during instrumented tests.
- **API 36**: APK install failure (`Can't find service: package`) — emulator may need longer boot stabilization.
- **API 30 (Gradle runner)**: `RootViewWithoutFocusException` on `BackupRestoreTest.testFreshAppLaunch` + "Unknown API Level" error from Gradle.

## CI Status
- Release AAB build step fails on all commits (pre-existing, related to #64 release pipeline)
- Unit tests and lint pass locally

## Success Criteria
- `scripts/run-instrumentation.sh` returns exit code 0 on a full run with real display.
- Tag `tested-YYYY-MM-DD` is automatically created and pushed.
