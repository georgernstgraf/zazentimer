# Project State

Current status as of 2026-05-22.

## Current Focus
#208 — Clean slate: remove old migrations, reset DB to V1, rename DB file.

## Completed (this cycle)
- [x] #204 — External song import: fixed auto-select, playback, and back-navigation crash
- [x] #205 — Added "Edit Section" to 3-dot overflow menu in session editor
- [x] #206 — Manage Bells settings screen with custom bell deletion + fixes
- [x] #208 — Removed all old migrations, reset DB to V1, renamed to `zazentimer.sqlite`
- [x] #208 — Replaced `ensureBellsTableConsistent()` with `seedBuiltinBells()`
- [x] #208 — Deleted MigrationTest.kt (516 lines, dead code)
- [x] #208 — Rewrote RestoreIntegrationTest to verify old backups fail
- [x] #208 — Gradle perf: parallel=true, caching=true, heap 4g

## Pending
- [ ] #202 — Multi-LLM translation pipeline (Prisma schema done, needs Python scripts)
- [ ] #64 — Promotion/Upload automation: Fastlane-like upload script

## Blockers
None

## Next Session Suggestion
Check instrumented test results from #208 scheduled run. Then #202.
