# Hand Off

## Open Tasks
1. [ ] See **#272** (OPEN — do NOT close yet) — API 23 test failures. **Could not reproduce on `claw` under Xvfb**, but the display environment is an uncontrolled variable. Must validate on a **real X11** host (`think`) before closing.
   - **Done on `claw`:** `test_api23` (x86_64) + `test_api23_x86` (32-bit) AVDs created; full `--api 23` green; raw `am instrument` of the 3 methods → `OK (3 tests)` on both ABIs → ABI ruled out. Code unchanged since 2026-06-15.
   - **Action on `think` (real `$DISPLAY`):** clean `test_api23` baseline (regenerate via `create-emulator-snapshots.sh 23` OR cold-boot); run `scripts/run-instrumentation.sh --api 23`; green → close #272; red → capture stack trace + logcat and fix.

2. [ ] **#267** (deferred) — large naming sweeps (test-method + layout-ID normalization to snake_case). ~83 test methods + ~50 layout IDs. Cosmetic, high blast radius. Revisit at a quieter moment (not right after #268's churn) or when detekt naming rules are adopted. The issue itself recommends opportunistic one-offs.

3. [ ] **#270 follow-up** (deferred) — migrate remaining `runBlocking` callsites in `SectionEditFragment` + `MainFragment.onPause()` to async. See DECISIONS.md "Keep runBlocking-in-onPause as-is" for the full 9-site inventory + app-scope conversion recipe + revisit trigger.

4. [ ] **Env followup (claw)** — API 34 freezer skip-check fooled by `setting=disabled` vs flag-not-applied on fast-boot resume (remedy: `--cold-boot` or re-baseline to `setting=null`); API 36 `system_server` crash on claw. Both documented in PITFALLS. Not code regressions.

## Completed (verified green this cycle)
- **#268** complete (commits `fd1fe88`→`13fa256`): `DbOperations` façade dissolved — all consumers inject specific repos + `DatabaseOwner` via Hilt. Issue closed.
- **#273** fixed (commit `88f44ae`): session ranks persist on drag-end via `clearView → onDragEnd`.
- **#271** fixed (commit `252bbd6`): score-based settlement in `voting_api.tsx`.
- 9 issues closed with commit references (#270, #255, #245, #269, #256, #271, #273, #268, + #272 investigated).
- onPause `runBlocking` refactor deferred + recorded (commit `a55037b`).

Last updated: 2026-06-19
