# Hand Off

## Open Tasks
1. [ ] See **#272** — API 23 (Android 6) test compatibility: 3 deterministic UI-test failures (`SessionCrudTest.testDeleteSession`, `SessionCrudTest.testDeleteCancel`, `SessionRankPersistenceTest.dragReorder_persistsAfterNavigationAndEdit`). Not an emulator issue; needs API-23-specific Espresso handling or `@RequiresApi` guards. Only blocker for a fully-green matrix tag.
2. [ ] **#270 follow-up**: migrate remaining `runBlocking` callsites in `SectionEditFragment` (`fillDataFromViews`, `installPlayGongListener`, `installBellSelectionListener`) and `MainFragment.onPause()` to `lifecycleScope.launch` where structurally feasible.

## Completed (verified green this cycle)
- Emulator / instrumented-test lifecycle stabilization: **13/14 of the API 23–36 matrix PASSES** (APIs 24–36; API 34 — the original hang — green first-try). All work on `main` (HEAD `2f5cd59`, pushed). Baselines regenerated clean; matrix runs `-no-snapshot-save`. See `STATE.md` and the emulator pitfalls in `PITFALLS.md` for the accumulated lifecycle rules a future agent must follow.

Last updated: 2026-06-15
