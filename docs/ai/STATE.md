# Project State

Current status as of 2026-05-27.

## Current Focus
Fix Room MIGRATION_1_2 crash: PK columns missing `NOT NULL` + unwanted `DEFAULT 0` on `rank`

## Completed (this cycle)
- [x] Remove 35 unused English strings from values/strings.xml (#223)
- [x] seed.ts: delete obsolete master_strings for idempotency (#224)
- [x] Inline language seeding into seed.ts, remove Python/JSON pipeline (#225)
- [x] Remove custom types from db.ts, use Prisma-generated types (#226)
- [x] Remove direct Prisma dependency from voting_api.tsx (#226)
- [x] Performance: groupBy for string vote counts, filter proficiencies (#227)
- [x] Fix: respect langId query param in /strings/:sid/comparison (#228)
- [x] Fix: load bell list fresh from DB in Adjust Bell Volumes dialog (#234)
- [x] Bell sliders: direct volume, normal direction, matched steps to system slider (#235)
- [x] Fix MIGRATION_1_2: added `NOT NULL` to all 4 PK columns, removed `DEFAULT 0` from `rank` (#207)
- [x] Add `fallbackToDestructiveMigration(true)` as safety net in DbOperations.kt

## Pending
- [ ] #64 — Play Store
- [ ] #202 — Translation Epic (runs in progress)

## Blockers
None

## Next Session Suggestion
Check latest translation run results. If all models have completed enough locales, implement voting auto-resolve → export strings.xml.
