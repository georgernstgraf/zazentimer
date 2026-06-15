# Project State

Current status as of 2026-06-15.

## Current Focus
Emulator / instrumented-test lifecycle stabilization — was blocking #270 matrix verification. Now stable.

## Completed (this cycle)
- [x] Emulator lifecycle hardening, all in `scripts/` (commits 0c79ad0, 7f3d41a, d10d425, f66e631, efd1eba, 2f5cd59; all on `main`, pushed):
  - **App freezer (API ≥ 31)**: correct disable via `cached_apps_freezer=disabled` + `activity_manager_native_boot use_freezer=false`, marker-gated one-time reboot per AVD. (The earlier wrong flag `native_with_freezer` did nothing — see HISTORY.md.)
  - **`am instrument` hang net**: `timeout -s KILL 900` (adb ignores SIGTERM; SIGKILL returns 137, detect 124||137) + `am force-stop` on timeout.
  - **Graceful qemu teardown** (`emulator_graceful_kill`): polls CPU/IO/D-state, no time cap while progressing, SIGTERM→SIGKILL only after 60s sustained idleness.
  - **SIGKILL → purge AVD snapshot** (force-kill truncates the in-flight save); also wired into `kill-test-run.sh --force`.
  - **API-aware device routing**: `--api N` honored; physical device used only when its API matches.
  - **Matrix default `-no-snapshot-save`**: stops self-poisoning baselines (post-test snapshots can be non-resumable). Baselines written only by `create-emulator-snapshots.sh`.
  - **create-emulator-snapshots.sh**: fixed stray `local` (aborted under `set -e`); added freezer provisioning for API ≥ 31.
  - **push_backup_fixture**: guard `mkdir` with `|| true` (a bare adb call under `set -e` aborted the whole matrix once).
  - Fixed dead `pkill -f "qemu.*android"` (case mismatch) → `qemu-system-x86_64` across stop/start/kill-test-run.
- [x] All 14 AVD baselines regenerated clean (api23–30 wiped; api31–36 wiped + freezer-provisioned).
- [x] Full matrix: **13/14 PASS** (APIs 24–36). API 34 — the original hang — passes first-try.

## Pending
- [ ] **API 23 test compatibility**: 3 deterministic UI-test failures on Android 6 (`SessionCrudTest.testDeleteSession`, `SessionCrudTest.testDeleteCancel`, `SessionRankPersistenceTest.dragReorder_persistsAfterNavigationAndEdit`) — separate from the emulator work; likely needs API-23-specific Espresso handling or `@RequiresApi` guards.
- [ ] #270 follow-up: migrate remaining `runBlocking` callsites in `SectionEditFragment` and `MainFragment.onPause()` to `lifecycleScope.launch` where structurally feasible.
- [ ] Occasional host-load flakes: a few APIs (32/29/28) sometimes fail attempt 1 and pass on retry — test-timing sensitivity under host load, not an emulator regression.

## Blockers
- None. The emulator is stable; API 23 is a test-suite compatibility issue.

## Next Session Suggestion
- Triage the 3 API-23 test failures (open a follow-up issue if not a quick fix). Then re-run the matrix for a fully-green `tested-YYYY-MM-DD` tag.
