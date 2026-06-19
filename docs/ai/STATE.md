# Project State

Current status as of 2026-06-19.

## Current Focus
None active. The #268 architectural refactor (DbOperations dissolution) is complete. The session's major workstreams are shipped.

## Completed (this cycle)
- [x] **#268 complete** (6 commits `fd1fe88`→`13fa256`, pushed): the `DbOperations` god-class façade is **deleted**. All consumers (production + test) inject the specific repositories (`SessionRepository`/`SectionRepository`/`BellRepository`/`BellSanitizer`) and `DatabaseOwner` directly via Hilt. `rg "DbOperations" app/src/` → zero type references. Phased 0→4 (Hilt foundation → single-repo consumers → multi-repo fragments → last prod consumers → test consumers → delete). `ARCHITECTURE.md` updated. Issue closed.
  - **Key design**: `DatabaseOwner` (`@Singleton @Inject`) owns the Room `AppDatabase` lifecycle (build/close/reopen/version); the 4 repos fetch DAOs **dynamically** from it (reopen-safe — Hilt singletons can't be rebuilt, and `close`/`reopen` recycle the connection).
  - **Blocker the issue body missed**: `DbOperations` wasn't a pure façade — it owned the Room lifecycle. Solved by `DatabaseOwner`.
- [x] **#273 fixed & closed** (commit `88f44ae`): session drag-reorder lost on Settings→back; fix = `clearView → onDragEnd → async assignRanks`; `SessionRankPersistenceTest` rewritten with a real drag gesture + identity assertions.
- [x] **#271 fixed & closed** (commit `252bbd6`): score-based settlement in `voting_api.tsx`; new pure `prisma/lib/settlement.ts` + repo's first `deno test`.
- [x] **9 issues closed** total this cycle: #270, #255, #245, #269, #256, #271, #273, #268, + #272 investigated.
- [x] **#272 investigated on `claw`/Xvfb** — not reproducible (both x86_64 + x86 32-bit pass; ABI ruled out; code unchanged since 2026-06-15). Pending real-X11 validation on `think`.
- [x] **onPause `runBlocking` refactor deferred** (commit `a55037b`) — recorded in DECISIONS.md under #270 follow-up.
- [x] **#267 deferred** — cosmetic naming sweeps; high blast radius right after #268's churn. Revisit at a quieter moment or when detekt naming rules are adopted.

## Pending
- [ ] **#272 — validate on real X11 (`think`)** before closing. Clean `test_api23` baseline, run `scripts/run-instrumentation.sh --api 23` under real `$DISPLAY`. Xvfb-vs-real-X11 is the remaining variable.
- [ ] **#267** (deferred) — large naming sweeps (test-method + layout-ID normalization). Cosmetic, high blast radius. Revisit when stable or when detekt naming rules are adopted.
- [ ] **#270 follow-up** — migrate remaining `runBlocking` callsites to async. Deferred (see DECISIONS.md "Keep runBlocking-in-onPause as-is"). Revisit trigger: jank, deadlock reappearance, or `withTransaction` re-added.
- [ ] **Env followup (claw)**: API 34 freezer skip-check fooled by `cached_apps_freezer=disabled` setting vs boot flag `use_freezer=false` not taking on fast-boot resume → `run-instrumentation.sh` skips re-provisioning → 900s hang. Remedy: `--cold-boot` or re-baseline to `setting=null`. API 36 `system_server` crash on claw after freezer-provisioning reboot. Both are claw/Xvfb-specific, not code regressions.

## `claw` AVD inventory
- `test_api23` (x86_64) + `test_api23_x86` (32-bit) — created this session for #272 repro.
- `test_api31`, `test_api34`, `test_api36` — existing; 34/36 baselines rebuilt (freezer-provisioned) but still hit claw env instability at runtime.

## Blockers
- #272 needs a **real-X11** host (`think`) to rule out the display-environment variable. `claw` is Xvfb-only.
- Full API matrix on `claw` is unreliable for ≥34 (freezer/cgroup/system_server instability — PITFALLS). Full-matrix gates may need `think` or a real display.

## Next Session Suggestion
- On `think`: clean `test_api23` baseline + run `scripts/run-instrumentation.sh --api 23` under real `$DISPLAY`. Green → close #272.
- Otherwise: pick a new feature/bug, or revisit #267/#270 at a quieter moment.
