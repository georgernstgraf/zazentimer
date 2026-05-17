# Project State

Current status as of 2026-05-18.

## Current Focus
#192 — Closed. Backup restore crash fixed and integration tests implemented.

## Completed (this cycle)
- [x] #64 — Play Store automation: Service Account connected, local `.venv` setup via `scripts/play_store/setup.sh`, scripts active in `scripts/play_store/`.
- [x] #192 — Backup restore crash fixed: corrected Room migration 6→7 schema (indices and column naming) and updated version check in `BackupManager`.
- [x] #192 — Implemented `RestoreIntegrationTest` (Robolectric) to verify database restoration and migration process using Lena's backup.
- [x] #183 — `MainScreenDeadStateTest` fixed: `inRoot(isDialog())` for API 36 AlertDialog focus loss
- [x] #183 — Removed `am instrument` fallback path; all APIs now use `connectedDebugAndroidTest` (Gradle)
- [x] #183 — `run-instrumentation.sh` restructured: flat early-exit pattern, `stdbuf -oL` for pipe buffering
- [x] #183 — Full matrix validation: APIs 23-36 all PASS with 24/24 instrumented tests
- [x] #180 — bells table V6→V7 migration, runtime repair, UI integration, 5 migration tests

## Pending
- [ ] #64 — Promotion/Upload automation: Implement full Fastlane-like upload script in Python.

## Blockers
None

## Next Session Suggestion
Test the Alpha release download with real users and proceed with automated production promotion script.
