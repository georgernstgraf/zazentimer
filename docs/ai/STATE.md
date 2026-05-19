# Project State

Current status as of 2026-05-19.

## Current Focus
3NF normalization complete (#199). Schema V10: bell/belluri/resourceName removed, sessions.rank added.

## Completed (this cycle)
- [x] #199 — MIGRATION_9_10: Drop bell/belluri/resourceName, add sessions.rank, NOT NULL on rank/bellcount/bellpause
- [x] #199 — Entities/BOs/EntityMapper: Remove all bell/belluri/resourceName fields, add rank to Session
- [x] #199 — SessionDao: ORDER BY rank,name, getMaxRank(), updateRank()
- [x] #199 — DbOperations: insertSession assigns rank, switchSessionPositions, insertSection defaults bellId to demo
- [x] #199 — MigrationHelper: Rewrite without resourceName/bellUri/bell references
- [x] #199 — BellCollection.getBellByUri() replaces getBellForSection()
- [x] #199 — BellPlayer accepts getBellById lambda for DB-backed bell lookup
- [x] #199 — SectionEditFragment: bell resolved via bellId + DB query instead of bellUri field access
- [x] #199 — SessionEditFragment: deriveBellVolumesFromSections simplified to bellId-only
- [x] #199 — BellVolumeConfigDialog: fallback bell/bellUri paths removed
- [x] #199 — Unit tests (212) + instrumentation tests (24) all pass

## Pending
- [ ] #64 — Promotion/Upload automation: Fastlane-like upload script
- [ ] #197 — Migrate from Room to Prisma (large refactoring)

## Blockers
None

## Next Session Suggestion
Push `v*` tag to trigger release workflow. Then #64 or #197.
