# Handoff

## Current Branch
`main` (Trunk-based development)

## Open Tasks
1. [ ] **Commit rewritten `run-instrumentation.sh`** — 209 lines of changes unstaged. Key changes: timestamps on all output, phase markers, ADB command logging, per-run isolation (Xvfb restart, zombie kill, crash DB archival, logcat clear), crash DB preservation, emulator PID capture. Reference: #183.
2. [ ] **#183 — Full validation run**: `scripts/run-instrumentation.sh --continue-on-error --ignore-dirty-git` (all APIs 23-36) with `gradleMaxApi=36`. Then consider removing the `am instrument` path from the script.
3. [ ] **#64 — Play Store**: Sub-issues #114 (AAB build) and #113 (privacy/legal). Blocked by missing `PLAY_SERVICE_ACCOUNT_JSON` GitHub secret.

## AVD Inventory (all installed)
`test_api23` `test_api24` `test_api25` `test_api26` `test_api27` `test_api28` `test_api29` `test_api30` `test_api31` `test_api32` `test_api33` `test_api34` `test_api35` `test_api36`

## Known Test Failures (from partial runs)
- **API 31-33**: `SettingsTest.testRestore` — `NoMatchingViewException: No views found matching "Restore from Backup"`. Likely the Settings screen scrolls and the item is off-screen.
- **API 35**: Process crash during instrumented tests.
- **API 30 (Gradle runner)**: `RootViewWithoutFocusException` on `BackupRestoreTest.testFreshAppLaunch` + "Unknown API Level" error from Gradle.

## Key Files Changed This Session
- `scripts/run-instrumentation.sh` — **unstaged**, +209/-103 lines. Full rewrite with logging/isolation.

## CI Status
- Release AAB build step fails on all commits (pre-existing, related to #64 release pipeline)
- Unit tests and lint pass locally

## Success Criteria
- `scripts/run-instrumentation.sh` returns exit code 0 on a full run with real display.
- Tag `tested-YYYY-MM-DD` is automatically created and pushed.
