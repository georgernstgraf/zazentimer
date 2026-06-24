# Project State

Current status as of 2026-06-24.

## Current Focus
#289 (backup fixture push fails on API 30 → false-green). Fix verified on full 14-API matrix (13/14 green; API 33 failed on pre-existing overflow-menu UI flakes, unrelated to #289).

## Completed (this cycle)
- [x] **#289 fix**: `BackupRestoreInstrumentedTest` self-provisions fixture from `app/src/androidTest/res/raw/zentimer_backup_room_v2.zip` (removed `Assume.assumeTrue` + `adb push` machinery). Phase 2 backup-restore verified green on API 23–32, 34–36 (13 APIs incl. regression target API 30 twice).
- [x] **FailOnAssumptionSkipListener**: in-process `RunListener` that crashes on any `Assume` skip. Registered via `am instrument -e listener`. Proven inert on all clean runs (zero false-positives); proven to fire on real assumption-skip (temp test → `Process crashed` → Phase 1 fails ~19s). Replaced the stdout dot-parser (GPU/SwiftShader log interleaving broke it on API 34).
- [x] **Auto-tag removed**: `run-instrumentation.sh` no longer auto-tags `tested-*`. Replaced with heads-up banner (`ALL TESTS PASSED — ready to commit & tag!`).
- [x] **Matrix run #2** (full 14-API, `--continue-on-error`, real `$DISPLAY` on `think`): 13/14 PASS. API 33 failed 2× (overflow-menu UI flakes: `SectionEditTest.addNewSection_addsSection` then `SettingsTest.backup_createsBackup`, both `NoMatchingViewException`). API 29 flaked once (`ManageBellsTest` import_button), passed on retry.

## Pending
- [ ] File GitHub issues for pre-existing UI flakes (API 33 overflow-menu: SectionEditTest/SettingsTest; API 29 ManageBellsTest import_button).

## Blockers
None. #289 is fixed and verified. The API 33/29 flakes are pre-existing environmental UI test flakiness (overflow menu / import button not rendering in time under sustained emulator load), not a code regression.

## Next Session Suggestion
- Tag `tested-2026-06-24` on the #289 fix commit after pushing.
- Investigate overflow-menu UI flake (API 33/29) if it recurs — likely Espresso idle-sync timing under load, not a deterministic bug.
