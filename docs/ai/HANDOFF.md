# Hand Off

## Open Tasks
1. [ ] **API 23 test compatibility (Android 6)**: `SessionCrudTest.testDeleteSession` (SessionCrudTest.kt:86), `SessionCrudTest.testDeleteCancel` (SessionCrudTest.kt:105), and `SessionRankPersistenceTest.dragReorder_persistsAfterNavigationAndEdit` (SessionRankPersistenceTest.kt:93) fail deterministically on both attempts. UI/Espresso behavior differs on API 23 (delete-dialog flow, RecyclerView ItemTouchHelper drag-reorder). Likely needs API-23-specific handling or `@RequiresApi` guards. Open a follow-up issue if not a quick fix.
2. [ ] **#270 follow-up**: migrate remaining `runBlocking` callsites in `SectionEditFragment` (`fillDataFromViews`, `installPlayGongListener`, `installBellSelectionListener`) and `MainFragment.onPause()` to `lifecycleScope.launch` where structurally feasible.

## Completed (verified green this cycle)
- Emulator / instrumented-test lifecycle stabilization: **13/14 of the API 23–36 matrix PASSES** (APIs 24–36; API 34 — the original hang — green first-try). All work on `main` (HEAD `2f5cd59`, pushed). Baselines regenerated clean; matrix runs `-no-snapshot-save`. See `STATE.md` and the emulator pitfalls in `PITFALLS.md` for the accumulated lifecycle rules a future agent must follow.

Last updated: 2026-06-15
