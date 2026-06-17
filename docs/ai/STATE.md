# Project State

Current status as of 2026-06-17.

## Current Focus
**#272** (OPEN) — API 23 instrumented-test failures. **Could not reproduce on `claw` under Xvfb**, but kept open pending validation on a **real X11** host (`think`). Caveat: claw forces Xvfb; UI tests (dialog focus, `ItemTouchHelper` drag) can differ under a virtual framebuffer vs real X11 — "passes on claw/Xvfb" ≠ "passes on real X11".

## Completed (this cycle)
- [x] **#273 fixed & closed** (commit `88f44ae`, pushed): session drag-reorder was lost on Settings→back navigation. Root cause: the drag was never persisted at drop time (`SessionTouchHelperCallback` had no `clearView` hook), and after 9bc8a66 `onPause() → runBlocking { assignRanks }` was the only saver — unreliable during in-app fragment transactions. Fix: `clearView → onDragEnd()` → `lifecycleScope.launch { assignRanks }` (async) persists ranks the instant the grip handle is released; `onPause()` kept as safety net. `SessionRankPersistenceTest` rewritten with a **real drag gesture** (DOWN on `dragHandle` → MOVEs → UP) + identity assertions + a Settings→back regression case. Verified: `OK (2 tests)` on API 34; detekt/JVM/ktlint green.
- [x] **#271 fixed & closed** (commit `252bbd6`): score-based settlement in `voting_api.tsx`; new pure `prisma/lib/settlement.ts` + repo's first `deno test`.
- [x] **6 issues closed earlier**, plus #273: #270, #255, #245, #269, #256, #271.
- [x] **#272 investigated on `claw`/Xvfb** — not reproducible (both x86_64 + x86 32-bit pass; ABI ruled out; code unchanged since 2026-06-15). Pending real-X11 validation.

## Pending
- [ ] **#272 — validate on real X11 (`think`)** before closing. Clean `test_api23` baseline (regenerate via `create-emulator-snapshots.sh 23` or cold-boot), run `scripts/run-instrumentation.sh --api 23` under real `$DISPLAY`. Two uncontrolled variables remain: (a) baseline snapshot state, (b) Xvfb-vs-real-X11 display env.
- [ ] **#270 follow-up** — migrate remaining `runBlocking` callsites in `SectionEditFragment` and `MainFragment.onPause()` to `lifecycleScope.launch` where structurally feasible. (Note: `MainFragment.onPause()`'s `runBlocking { assignRanks }` is now a *safety net* behind the drop-time persist — lower priority, but still part of the fragile runBlocking-in-onPause class.)
- [ ] **#268** (deferred) — full DbOperations Hilt-migration; high blast radius.
- [ ] **#267** (deferred) — large naming sweeps; cosmetic.
- [ ] Occasional host-load flakes on a few APIs (32/29/28) — test-timing sensitivity.

## `claw` API 23 AVD inventory (created this session)
- `test_api23` — x86_64, pixel_6, default tag. **Canonical matrix AVD** (resolver's exact-`test_api<N>` match).
- `test_api23_x86` — x86 32-bit. Dormant (only used by explicit name for the ABI comparison).

## Blockers
- #272 needs a **real-X11** host to rule out the display-environment variable. `claw` is Xvfb-only.

## Next Session Suggestion
- On `think`: clean `test_api23` baseline + run `scripts/run-instrumentation.sh --api 23` under real `$DISPLAY`. Green → close #272.
