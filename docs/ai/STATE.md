# Project State

Current status as of 2026-06-22.

## Current Focus
None active. This cycle closed #290, #291, and #292. Full 4-API matrix (23, 27, 31, 35) is green on `think`. The codebase is at `f6badd3` on `origin/main`.

## Completed (this cycle)
- [x] **4-API matrix green (23/27/31/35)** on `think` — closed the test regressions blocking #290.
  - `bb58408` — API 23: `Paths.get` → `java.io.File` (API <26 compat).
  - `4f83abf` — API 31/35 backup test: `DatabaseOwner.close()` now actually consumes the WAL-checkpoint cursor (was a silent no-op).
  - `702af05` — API 31 `DuplicateSessionTest` FK crash: `BellSanitizer.sanitizeBellUris()` wrapped in transaction; `resetDatabaseForTest()` cancels the activity's onCreate `initializationJob` before mutating DB (eliminates the sanitize/createDemoSessions race the slower API 31 emulator exposed).
  - `bb349bc` — API 35 `SessionRankPersistenceTest` drag long-press navigation: drag ViewAction calls `view.cancelLongPress()` + `dragHandle.cancelLongPress()` after `ACTION_DOWN`.
- [x] **#291 (custom audio import tests)**: 10 parameterized tests (5 formats × good/bad) added to `ManageBellsTest.kt`. Fixtures moved from `app/src/test/resources/audio/` to `app/src/androidTest/res/raw/` (renamed per Android raw-resource rules). Two production bugs surfaced and fixed in follow-up commit `071c264`:
  - `BellSanitizer.importOrphanedBellFiles` now catches `BellImportException` per-file (log + delete corrupt file) — was an uncaught crash that bricked the app on every launch if any `bell_*` file in `filesDir/` was invalid audio.
  - `ManageBellsFragment` import-failure toast always uses `R.string.bell_import_failed` (was showing raw `e.message` = "Prepare failed.: status=0x1" — the Elvis fallback was unreachable).
- [x] **#292 (dialog-root flake)**: `SectionEditTest.kt:70` and `MeditationServiceTest.kt:100-108` now use `.inRoot(isDialog())` for AlertDialog button clicks. Commit `f6badd3`. Attempt-1 API 35 run passes cleanly (no `RootViewWithoutFocusException`, no retry needed).
- [x] **Knowledge persistence**: PITFALLS.md gained 6 new permanent-constraint entries (setsid vs nohup, Toast invisible to UiAutomator, auto-retry masking, testFixtures classpath, `e.message` anti-pattern, BellImportException catch requirement). CONVENTIONS.md gained 4 new entries under §Instrumented Test Reliability (extending the `.inRoot(isDialog())` rule, negative-assertion corroboration, indirect toast verification, BellValidator catch). HISTORY.md gained entries for the 7 fixed bugs from this cycle.

## Pending
None.

## Blockers
None. Full 4-API matrix green on `think`. `claw` still has known env instability for API ≥ 34 (freezer / system_server; see PITFALLS #126-127) — not a code regression.

## `claw` AVD inventory
- `test_api23` (x86_64) + `test_api23_x86` (32-bit) — created for #272 repro; both ABIs green.
- `test_api31`, `test_api34`, `test_api36` — existing; 34/36 baselines rebuilt (freezer-provisioned) but still hit claw env instability at runtime.

## Next Session Suggestion
- Pick a new feature/bug, or run the full 14-API matrix on `think` for a wider sanity check (the 4-API matrix is green; the other 10 APIs haven't been exercised this cycle).
