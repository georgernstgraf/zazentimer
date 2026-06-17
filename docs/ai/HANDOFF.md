# Hand Off

## Open Tasks
1. [ ] See **#272** (OPEN — do NOT close yet) — API 23 test failures. **Could not reproduce on `claw` under Xvfb, but the display environment is an uncontrolled variable.** Must validate on a **real X11** host (`think`) before closing.
   - **Done on `claw`:** created `test_api23` (x86_64) + `test_api23_x86` (32-bit) AVDs. Full `scripts/run-instrumentation.sh --api 23` green. Raw `am instrument` of the 3 methods → `OK (3 tests)` on **both** ABIs → **ABI ruled out**. `git log`: zero commits to tests/dialog/drag since 2026-06-15. Evidence comment on #272.
   - **Why not closed:** claw forces **Xvfb**. Focus-sensitive matches (`isDialog()`) and `ItemTouchHelper` drag touch sequencing can differ under Xvfb vs real X11. Remaining variables: (a) baseline **snapshot state**, (b) **display environment**.
   - **Action on `think` (real `$DISPLAY`):** (1) ensure a **clean** `test_api23` (regenerate via `scripts/create-emulator-snapshots.sh 23` OR cold-boot); (2) `scripts/run-instrumentation.sh --api 23`; (3) green → close #272; red → capture stack trace + `logs/api23-*-logcat.txt` and fix.

2. [ ] **#270 follow-up**: migrate remaining `runBlocking` callsites in `SectionEditFragment` (`fillDataFromViews`, `installPlayGongListener`, `installBellSelectionListener`) and `MainFragment.onPause()` to `lifecycleScope.launch` where structurally feasible. (`MainFragment.onPause`'s `runBlocking { assignRanks }` is now a safety net behind the drop-time persist from #273 — lower priority.)

## Completed (verified green this cycle)
- **#273** fixed & closed (commit `88f44ae`): session ranks now persist on drag-end via `clearView → onDragEnd` (async `assignRanks`), fixing the Settings→back revert. `SessionRankPersistenceTest` now uses a real drag gesture + identity assertions.
- **#271** fixed (commit `252bbd6`): score-based settlement in `voting_api.tsx`; new pure `prisma/lib/settlement.ts` + repo's first `deno test`.
- 7 issues closed with commit references (#270, #255, #245, #269, #256, #271, #273).
- #272 investigated on `claw`/Xvfb (not reproducible here; both ABIs pass).

Last updated: 2026-06-17
