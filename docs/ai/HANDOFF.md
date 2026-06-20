# Hand Off

## Open Tasks
1. [ ] **#267** (deferred) — large naming sweeps (test-method + layout-ID normalization to snake_case). ~83 test methods + ~50 layout IDs. Cosmetic, high blast radius. Revisit at a quieter moment or when detekt naming rules are adopted. The issue itself recommends opportunistic one-offs.

2. [ ] **Env followup (claw)** — API 34 freezer skip-check fooled by `setting=disabled` vs flag-not-applied on fast-boot resume (remedy: `--cold-boot` or re-baseline to `setting=null`); API 36 `system_server` crash on claw. Both documented in PITFALLS. Not code regressions.

## Completed (verified green this cycle)
- **#270 follow-up** complete: remaining `runBlocking` callsites in production code migrated to an application-scoped `CoroutineScope(SupervisorJob() + Dispatchers.IO)`. UI values are captured synchronously in `onPause()`; DB/audio writes run asynchronously and survive fragment destruction. `./gradlew test`, `assembleDebug`, `assembleDebugAndroidTest`, and `detekt` all pass.
- **#272** closed after real-X11 validation on `think`: API 23 (x86_64 + x86) instrumented tests green; the three failing tests (`testDeleteSession`, `testDeleteCancel`, `dragReorder_persistsAfterNavigationAndEdit`) now pass.
- **#281** closed: choppy emulator bell audio fixed via QEMU PA env vars + audio-input disable + `MediaPlayer` reuse in `Audio.playAbsVolume()` (commits `8c763dc`, `2ab171a`, `7a0cbaf`).
- **#283** closed: snapshot protections added — configurable `adb wait-for-device` timeout, purge-after-boot-timeout, purge-on-test-failure (commits `223006f`, `7a0cbaf`).
- **#284** closed: API 31/32/34 FOREIGN KEY / empty-bells-table crashes no longer reproduce after baseline/snapshot hardening; full matrix green on `think`.
- **#285** closed: `MainFragment.onDragEnd()` lifecycle race fixed by using `lifecycleScope` instead of `viewLifecycleOwner.lifecycleScope` (commit `7a0cbaf`).
- **#268** complete (commits `fd1fe88`→`13fa256`): `DbOperations` façade dissolved — all consumers inject specific repos + `DatabaseOwner` via Hilt. Issue closed.
- **#273** fixed (commit `88f44ae`): session ranks persist on drag-end via `clearView → onDragEnd`.
- **#271** fixed (commit `252bbd6`): score-based settlement in `voting_api.tsx`.
- onPause `runBlocking` refactor deferred + recorded (commit `a55037b`).

Last updated: 2026-06-19
