# Project State

Current status as of 2026-06-13.

## Current Focus
#255: Real V2 backup restore tests, single-device `am instrument` runner.

## Completed (this cycle)
- [x] #252: Extract `BellPlayer`, `MeditationRepository`, and `AlarmScheduler` interfaces; implement pure Kotlin fakes; add 20 `MeditationTest` methods.
- [x] #253: Fix stale in-memory session write in `MainFragment.suspendUpdateSessionList()` that overwrote edits from `SessionEditFragment`.
- [x] #254: Fix `clearText()+typeText()` race condition in `SessionCrudTest` by using `replaceText()`; enable emulator snapshot saving in `run-instrumentation.sh`.
- [x] #255: Add `fos.fd.sync()` to `BackupManager.receiveBytes()` before `fos.close()`.
- [x] #255: Create `@BackupTest` annotation; annotate `BackupRestoreInstrumentedTest`.
- [x] #255: Change fixture path from `/data/local/tmp/` to `/sdcard/Download/` (accessible by SAF).
- [x] #255: Add `MANAGE_EXTERNAL_STORAGE` to test manifest for API 30+.
- [x] #255: Rewrite `run-instrumentation.sh` — replace `connectedDebugAndroidTest` with `am instrument` targeting a single device serial.
- [x] #255: Dynamic runner/package discovery from `pm list instrumentation` after APK install.
- [x] #255: Two-phase test execution: Phase 1 (main tests), Phase 2 (backup restore last).
- [x] #255: Source-tree-based test class discovery (no hardcoded class names).
- [x] #255: Physical device preferred over emulator; single device guarantee.
- [x] All 14 API levels (23–36) instrumented tests PASS. Auto-tag `tested-2026-06-13` pushed.

## Pending
- [ ] #255: Run instrumented tests with new `run-instrumentation.sh` to verify.
- [ ] #255: Create `BackupRestoreUiTest.kt` (UiAutomator, SAF picker flow) — future task.
- [ ] F-Droid MR !39945 — await maintainer review
- [ ] Play Store release (first production deploy)

## Blockers
- None

## Next Session Suggestion
- Run `scripts/run-instrumentation.sh` to verify the new `am instrument` two-phase approach works on all API levels.
- If green, persist knowledge and close #255.