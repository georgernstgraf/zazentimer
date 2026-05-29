# Hand Off

No pending tasks. Last cleared: 2026-05-27.

## Recently Completed
- Removed 35 unused English strings from values/strings.xml (#223)
- seed.ts idempotent: deletes obsolete master_strings (#224)
- Inline language seeding into seed.ts, removed Python script (#225)
- Removed custom types + direct Prisma from voting_api.tsx (#226)
- Performance: groupBy for vote counts, filtered proficiencies (#227)
- Fixed langId param in comparison page (#228)
- Fixed stale bell list in Adjust Bell Volumes dialog (#234)
- Bell sliders: direct volume, normal direction, matched to system steps (#235)
- Fixed Room MIGRATION_1_2: missing `NOT NULL` on PK columns + unwanted `DEFAULT 0` on `rank` (#207)
- Added `fallbackToDestructiveMigration(true)` as safety net
- Added RoomMigrationTest with 6 test cases for MIGRATION_1_2 (#207)
- Created `prisma/export.ts` — exports best tiebreak-winner translations to parallel `values-*/strings.xml` structure
- Added `export` task to `prisma/deno.json`
