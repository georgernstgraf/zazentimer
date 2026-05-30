# Hand Off

No pending tasks. Last cleared: 2026-05-29.

## Recently Completed
- Fixed Room MIGRATION_1_2: missing `NOT NULL` on PK + unwanted `DEFAULT 0` (#207)
- Added `fallbackToDestructiveMigration(true)` + RoomMigrationTest with 6 cases
- Created `prisma/export.ts` — in-place values-* regeneration from voting DB
- Added `export` task to `prisma/deno.json`
- Deleted `scripts/apply_translations.py`
- #233: escapeXml negative lookbehind + \&amp; repair + \'%s\' filter
- #233: getEvaluation random tiebreak + getBestTranslation(text, bcp47)
