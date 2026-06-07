# Project State

Current status as of 2026-06-07.

## Current Focus
F-Droid submission — build succeeds but APK not found (#242)

## Completed (this cycle)
- [x] #241: WAL/SHM deletion in BackupManager after database overwrite to prevent stale WAL corruption
- [x] #241: Room-based sanitizeBellUris() health check after restore
- [x] #241: Builtin bell sync — insert missing bells, update URIs for package name changes (debug ↔ production)
- [x] #241: Orphaned builtin cleanup — delete bells not in current BellCollection, reassign sections to demo bell
- [x] #241: Strict 1:1 custom bell sync — remove DB entries for missing files (reassign sections), insert DB entries for orphaned files on disk
- [x] #241: Startup health check — `dbOperations.sanitizeBellUris()` runs in `ZazenTimerActivity.onCreate()` lifecycleScope, replacing old `ensureBellsTableConsistent()`
- [x] #241: Deleted `MigrationHelper.kt`/`seedBuiltinBells()` — fully redundant with `sanitizeBellUris()`
- [x] #239: Closed — empty session bug already fixed by #236 (auto-create first section)
- [x] #64: Closed — Play Store pipeline complete, alpha→production manual
- [x] F-Droid: `SOUND_LICENSES.md`, `.fdroid.yml`, `scripts/release.sh` created
- [x] F-Droid: metadata submitted as MR !39945 to fdroiddata
