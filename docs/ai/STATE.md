# Project State

Current status as of 2026-05-18.

## Current Focus
All session issues (#193, #195, #196, #194) completed and closed.

## Completed (this cycle)
- [x] #193 — Default bell volume 100% → 50%, centralized in Constants.kt
- [x] #193 — Duplicate bell volume sliders fixed (grouping by bellId)
- [x] #193 — MigrationHelper: fix volume save/deduplication, repair stale URIs, mark built-in bells
- [x] #193 — Meditation.getVolumeForSection: match by bellId instead of bell/bellUri
- [x] #193 — Demo sessions: use noBackupFilesDir marker instead of SharedPreferences flag
- [x] #195 — Back arrow hidden during meditation (setDisplayHomeAsUpEnabled(false))
- [x] #196 — System alarm volume slider in Bell Volumes dialog + section headers
- [x] #196 — VOLUME_CHANGED_ACTION receiver for external volume changes
- [x] #194 — Add Section FAB replaced with 3-dot menu item
- [x] #64 — Play Store automation scripts active

## Pending
- [ ] #64 — Promotion/Upload automation: Fastlane-like upload script
- [ ] #197 — Migrate from Room to Prisma (large refactoring)

## Blockers
None

## Next Session Suggestion
#64 Promotion Automation or #197 Prisma migration.
