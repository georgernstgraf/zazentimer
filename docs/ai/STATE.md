# Project State

Current status as of 2026-05-16.

## Current Focus
#183 — AGP 9.1.1 `connectedAndroidTest` validation on API 31-36.

## Completed (this cycle)
- [x] Researched AGP history: `connectedAndroidTest` is standard (88.1%), `am instrument` rare (1.9%)
- [x] Researched AGP 10.0: not yet released, expected H2 2026, will remove all opt-out flags
- [x] Confirmed AGP 9.1.1 `connectedDebugAndroidTest` works on API 31 (24/25), 34 (25/25), 36 (23/25)
- [x] Set `gradleMaxApi=36` in `gradle.properties`
- [x] Committed and pushed: `fc6e53f`

## Pending
- [ ] #183 Phase 4: Full validation run APIs 23-36 with `gradleMaxApi=36`
- [ ] #183 Phase 3: Consider removing `am instrument` path from script
- [ ] #64 — Play Store (Sub-issues #114 and #113)

## Blockers
None

## Next Session Suggestion
Run full validation: `scripts/run-instrumentation.sh --continue-on-error --ignore-dirty-git` (all APIs 23-36) with the new `gradleMaxApi=36` setting.
