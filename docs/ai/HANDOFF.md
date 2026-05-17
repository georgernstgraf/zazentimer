# Handoff

## Current Branch
`main` (Trunk-based development)

## Open Tasks
1. [ ] **#64 — Play Store**: Blocked by missing `PLAY_SERVICE_ACCOUNT_JSON` GitHub secret.

## Architecture Update (2026-05-17, #180)
- Added `bells` database table (V7) with FK from `sections.bell_id` and `session_bell_volumes.bell_id`
- `MigrationHelper.ensureBellsTableConsistent()` runs every startup: seeds built-in bells, syncs custom bells, fixes stale URIs from backup imports, deduplicates
- Bell references (bell, belluri) kept as migration buffer; bell_id is the canonical FK
- `BellPlayer.playBell()` has demo bell fallback if `getBellForSection()` returns null

## Key Files from #180
- `app/src/main/kotlin/at/priv/graf/zazentimer/database/BellEntity.kt` — bells table entity
- `app/src/main/kotlin/at/priv/graf/zazentimer/database/BellDao.kt` — bells table DAO
- `app/src/main/kotlin/at/priv/graf/zazentimer/database/AppDatabase.kt` — VERSION_7, MIGRATION_6_7
- `app/src/main/kotlin/at/priv/graf/zazentimer/MigrationHelper.kt` — `ensureBellsTableConsistent()` runtime repair
- `app/src/main/kotlin/at/priv/graf/zazentimer/service/BellPlayer.kt` — demo bell fallback
- `app/src/schemas/at.priv.graf.zazentimer.database.AppDatabase/7.json` — Room schema export for V7

## Pushed Tags
- `tested-2026-05-15` — APIs 23-36 all green (24/24 tests each)

## CI Status
- Release AAB build step fails on all commits (pre-existing, related to #64 release pipeline)
- Unit tests and lint pass locally
