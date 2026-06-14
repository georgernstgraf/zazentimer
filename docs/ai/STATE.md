# Project State

Current status as of 2026-06-15.

## Current Focus
Epic #256: "Code quality and project cleanup" (including sub-issues #257–#267). Completing the pre-flight readiness and doc refresh before dispatching pre-flight research agents.

## Completed (this cycle)
- [x] Create GitHub Epic #256 "Code quality and project cleanup" with 11 sub-issues (#257–#267) formally linked via the Sub-Issues API.
- [x] Create `HISTORY.md` as an append-only archive for historical and superseded entries (Phase 1).
- [x] Audit `DECISIONS.md`, relocating all stale database-migration stories (V7-V10, MIGRATION_7_8/9_10, 3NF drops, `ensureBellsTableConsistent()`) to `HISTORY.md` and correcting `_id` to `id` (Phase 2).
- [x] Split `PITFALLS.md`, moving all fixed-bug pitfalls (clearText race, session rank loss, dual selection, etc.) to `HISTORY.md` and leaving clean, one-line pointers in `PITFALLS.md` (Phase 3).
- [x] Overwrite `ARCHITECTURE.md` to represent V2 reality, use `id` primary keys, and fold the contents of `PLAY_STORE_SETUP.md` and `UI_TEST_PLAN.md` into it (Phase 4).
- [x] Update `CONVENTIONS.md` database and annotation rules to match the modern codebase (Phase 5).
- [x] **#270: Eliminate BellCollection in-memory cache** — DB is now the single source of truth for bells. Deleted `BellCollection`, `bo/Bell`, `BellCollectionTest`, `BellTest`. Added pure-config `BuiltinBells`, `BellDao.getBuiltinByName`, `BellRepository.getDemoBell`. All detekt + JVM tests + assembleDebug + assembleDebugAndroidTest green. Instrumented matrix verification pending (orchestrator runs `scripts/run-instrumentation.sh`).

## Pending (Epic #256 sub-issues)
- [ ] #257: Remove rubble (orphaned zip, scratch files, stale schema JSONs) (Low)
- [ ] #258: Refresh stale docs/ai/ knowledge files for V2 reality (Low)
- [ ] #259: Document Play Store vs F-Droid asset stores (Low)
- [ ] #260: Quick naming wins (log tag, ServCon, MyAnimator, German resources) (Low)
- [ ] #261: Delete dead code (Low)
- [ ] #262: Extract duplicated utilities + BellImporter (Medium)
- [ ] #263: Split DbOperations god class (462 lines) (Medium-High)
- [ ] #264: Backfill missing regression tests (Low-Medium)
- [ ] #265: Normalize bellId -> bell_id (snake_case FK; needs MIGRATION_2_3) (Medium)
- [ ] #266: Refactor TimerView god class (844 lines) (High)
- [ ] #267: (Optional, defer) Large naming sweeps (High blast)
- [ ] **Follow-up from #270**: Migrate remaining `runBlocking` fragment callsites to `lifecycleScope.launch` where structurally feasible. Spots kept on `runBlocking`: `SectionEditFragment.fillDataFromViews/installPlayGongListener/installBellSelectionListener` (synchronous bell-result usage right after); `MainFragment.onPause` rank persistence (lifecycle safety net).

## Blockers
- None

## Next Session Suggestion
- Run `scripts/run-instrumentation.sh` to verify #270 across the full API 23-36 matrix. The API 34 FK crash (`DuplicateSessionTest.testDuplicateSessionCreatesCopyWithPrefix`) and async-timing flakes should both be resolved (no in-memory cache to diverge from the DB).
