# Project State

Current status as of 2026-05-16.

## Current Focus
#183 — Full validation run APIs 23-36 with rewritten `run-instrumentation.sh`.

## Completed (this cycle)
- [x] Researched AGP history: `connectedAndroidTest` is standard (88.1%), `am instrument` rare (1.9%)
- [x] Confirmed AGP 9.1.1 `connectedDebugAndroidTest` works on API 31, 34, 36
- [x] Set `gradleMaxApi=36` in `gradle.properties` (commit `fc6e53f`)
- [x] Rewrote `run-instrumentation.sh` — reconstructed lost unstaged changes from log file forensics
- [x] Knowledge persisted (commit `1df188f`)

## Pending
- [ ] #183 Phase 4: Full validation run APIs 23-36 with `gradleMaxApi=36`
- [ ] Commit the rewritten `run-instrumentation.sh` (currently unstaged, +209/-103 lines)
- [ ] #64 — Play Store (Sub-issues #114 and #113)

## Blockers
None

## Next Session Suggestion
1. Commit the rewritten `run-instrumentation.sh` (issue #183 reference)
2. Run full validation: `scripts/run-instrumentation.sh --continue-on-error --ignore-dirty-git`
