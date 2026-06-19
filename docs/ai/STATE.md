# Project State

Current status as of 2026-06-19.

## Current Focus
None active. Recent cleanup closed #272, #281, #283, #284, and #285 after the full API matrix went green on `think`. The remaining open items are deferred (#267 naming sweeps, #270 `runBlocking` follow-up) and env-specific `claw` notes.

## Completed (this cycle)
- [x] **#272 closed** after real-X11 validation on `think`: API 23 (x86_64 + x86) instrumented tests green; `testDeleteSession`, `testDeleteCancel`, and `dragReorder_persistsAfterNavigationAndEdit` all pass.
- [x] **#281 closed** (commits `8c763dc`, `2ab171a`, `7a0cbaf`): choppy emulator bell audio fixed via QEMU PulseAudio env vars, disabled virtual audio input, and `MediaPlayer` reuse in `Audio.playAbsVolume()`.
- [x] **#283 closed** (commits `223006f`, `7a0cbaf`): snapshot protections added — configurable `adb wait-for-device` timeout, purge snapshot after boot timeout, purge snapshot on test failure.
- [x] **#284 closed**: API 31/32/34 FOREIGN KEY / empty-bells-table crashes no longer reproduce after baseline/snapshot hardening; full matrix green on `think`.
- [x] **#285 closed** (commit `7a0cbaf`): `MainFragment.onDragEnd()` lifecycle race fixed by using `lifecycleScope` instead of `viewLifecycleOwner.lifecycleScope`.
- [x] **#268 complete** (6 commits `fd1fe88`→`13fa256`, pushed): the `DbOperations` god-class façade is **deleted**. All consumers (production + test) inject the specific repositories (`SessionRepository`/`SectionRepository`/`BellRepository`/`BellSanitizer`) and `DatabaseOwner` directly via Hilt. `rg "DbOperations" app/src/` → zero type references. Phased 0→4 (Hilt foundation → single-repo consumers → multi-repo fragments → last prod consumers → test consumers → delete). `ARCHITECTURE.md` updated. Issue closed.
  - **Key design**: `DatabaseOwner` (`@Singleton @Inject`) owns the Room `AppDatabase` lifecycle (build/close/reopen/version); the 4 repos fetch DAOs **dynamically** from it (reopen-safe — Hilt singletons can't be rebuilt, and `close`/`reopen` recycle the connection).
  - **Blocker the issue body missed**: `DbOperations` wasn't a pure façade — it owned the Room lifecycle. Solved by `DatabaseOwner`.
- [x] **#273 fixed & closed** (commit `88f44ae`): session drag-reorder lost on Settings→back; fix = `clearView → onDragEnd → async assignRanks`; `SessionRankPersistenceTest` rewritten with a real drag gesture + identity assertions.
- [x] **#271 fixed & closed** (commit `252bbd6`): score-based settlement in `voting_api.tsx`; new pure `prisma/lib/settlement.ts` + repo's first `deno test`.
- [x] **onPause `runBlocking` refactor deferred** (commit `a55037b`) — recorded in DECISIONS.md under #270 follow-up.
- [x] **#267 deferred** — cosmetic naming sweeps; high blast radius right after #268's churn. Revisit at a quieter moment or when detekt naming rules are adopted.

## Pending
- [ ] **#267** (deferred) — large naming sweeps (test-method + layout-ID normalization). Cosmetic, high blast radius. Revisit when stable or when detekt naming rules are adopted.
- [ ] **#270 follow-up** — migrate remaining `runBlocking` callsites to async. Deferred (see DECISIONS.md "Keep runBlocking-in-onPause as-is"). Revisit trigger: jank, deadlock reappearance, or `withTransaction` re-added.
- [ ] **Env followup (claw)**: API 34 freezer skip-check fooled by `cached_apps_freezer=disabled` setting vs boot flag `use_freezer=false` not taking on fast-boot resume → `run-instrumentation.sh` skips re-provisioning → 900s hang. Remedy: `--cold-boot` or re-baseline to `setting=null`. API 36 `system_server` crash on claw after freezer-provisioning reboot. Both are claw/Xvfb-specific, not code regressions.

## `claw` AVD inventory
- `test_api23` (x86_64) + `test_api23_x86` (32-bit) — created for #272 repro; both ABIs green.
- `test_api31`, `test_api34`, `test_api36` — existing; 34/36 baselines rebuilt (freezer-provisioned) but still hit claw env instability at runtime.

## Blockers
- Full API matrix on `claw` is unreliable for ≥34 (freezer/cgroup/system_server instability — PITFALLS). Full-matrix gates may need `think` or a real display.

## Next Session Suggestion
- Pick a new feature/bug, or revisit #267/#270 at a quieter moment.
