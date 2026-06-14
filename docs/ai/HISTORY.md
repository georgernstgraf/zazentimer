# History

Superseded entries archive. Chronological record of decisions, pitfalls, and
convention changes that are NO LONGER in force. Entries here are preserved for
their reasoning ("why did we used to do X") and moved here from the active
knowledge files when superseded by a later change. Never delete from this file.

Format: `## YYYY-MM-DD (SUPERSEDED YYYY-MM-DD, origin: <FILE>, reason: <why|#NNN>): <Title>`

---

## 2026-05-14 (SUPERSEDED 2026-05-16, origin: DECISIONS.md, reason: #182 DND/mute removed entirely): DND uses INTERRUPTION_FILTER_PRIORITY with alarm-allowing policy
- **Choice**: Changed "None" mute mode from `INTERRUPTION_FILTER_NONE` to `INTERRUPTION_FILTER_PRIORITY` with a custom `NotificationManager.Policy` that allows alarms (`PRIORITY_CATEGORY_ALARMS`). Refactored `AudioStateManager` to save `activeMuteMode` at mute time. Simplified DND restore guard to compare only the filter. Refactored `Meditation.finishMeditation()` into `stopImmediate()` and `finishAfterLastBell()` with shared `cleanup()`.
- **Reason**: `INTERRUPTION_FILTER_NONE` suppressed all audio including alarms. DND restore was failing due to `NotificationManager.Policy.equals()` being unreliable. `unmutePhone()` was re-reading preferences which could differ from what `mutePhone()` used. Single `finishMeditation()` had race conditions with `BellPlayer`'s `onDone` callback.
- **Superseded because**: Phone has its own DND system; app should not modify it. All DND/mute code removed in #182. `AudioStateManager` no longer exists.

## 2026-05-14 (SUPERSEDED 2026-06-14, origin: DECISIONS.md, reason: #208 clean slate reset DB to V2): Avg volume migration for bell volumes
- **Choice**: When multiple sections used the same bell with different volumes, the migration takes the average.
- **Reason**: Average preserves the intent of all sections rather than favoring one arbitrarily.
- **Superseded because**: The migration (V7→V8 era) no longer exists after #208 reset the DB to V2. V2's `CREATE TABLE` bakes the per-session bell-volume schema in directly; no migration averaging applies.

## 2026-05-19 (SUPERSEDED 2026-06-14, origin: DECISIONS.md, reason: #208 clean slate reset DB to V2): Foreign key bellId → bells._id with DB migration 7→8
- **Choice**: Added `FOREIGN KEY (bellId) REFERENCES bells(_id)` to both `sections` and `session_bell_volumes` tables via MIGRATION_7_8, plus `@ForeignKey` annotations on Room entities.
- **Reason**: No FK constraint existed — `bellId = 0` was silently allowed, causing sections with the same bell to collapse into a single entry in the Bell Volume dialog.
- **Superseded because**: MIGRATION_7_8 no longer exists. The FK constraint is still in force — it is declared directly in V2's `CREATE TABLE` (`AppDatabase.MIGRATION_1_2`, `FOREIGN KEY (bellId) REFERENCES bells(id)`) and via `@ForeignKey` annotations. The current-state fact is documented in ARCHITECTURE.md. Only the migration *story* is historical.

## 2026-05-19 (SUPERSEDED 2026-06-07, origin: DECISIONS.md, reason: #241 sanitizeBellUris replaces it): ensureBellsTableConsistent at every startup
- **Choice**: `ZazenTimerActivity` calls `MigrationHelper.ensureBellsTableConsistent()` at every app startup (not just on backup restore), before demo session creation.
- **Reason**: The bells table may be stale after backup restore, manual DB modification, or upgrade from older versions.
- **Superseded because**: `sanitizeBellUris()` (in `DbOperations`, invoked from `ZazenTimerActivity.onCreate`) is the sole bell-sync function. `MigrationHelper.kt` was deleted entirely (its `seedBuiltinBells()` is fully redundant with `sanitizeBellUris()`).

## 2026-05-19 (SUPERSEDED 2026-06-14, origin: DECISIONS.md, reason: #208 clean slate reset DB to V2): 3NF Normalization — remove bell/belluri/resourceName (#199)
- **Choice**: Dropped `sections.bell`, `sections.belluri`, `session_bell_volumes.bell`, `session_bell_volumes.belluri`, `bells.resourceName`. Made `sections.rank`/`bellcount`/`bellpause` NOT NULL. Changed `session_bell_volumes` unique constraint from `(fk_session, bell, belluri)` to `(fk_session, bellId)`.
- **Reason**: These columns were duplicate representations of bell identity — the canonical source is `bells._id` via FK constraint.
- **Superseded because**: The normalization's END STATE is baked into V2 (no `bell`/`belluri`/`resourceName` columns exist). Only the migration (`MIGRATION_9_10`) that achieved it is gone. The current normalized schema is documented in ARCHITECTURE.md.

---

## Pitfalls moved here (fixed bugs; see PITFALLS.md for one-line pointers)

## 2026-05-19 (SUPERSEDED, origin: PITFALLS.md, reason: fix landed, migration gone after #208): Room MIGRATION_1_2: PK must be `NOT NULL` explicitly
- Room validates the migrated database against entity annotations using `PRAGMA table_info`. Even though SQLite treats `INTEGER PRIMARY KEY AUTOINCREMENT` as implicitly NOT NULL, `table_info.notnull` returns 0 for the PK if `NOT NULL` is not explicitly written. All PK columns in migration CREATE TABLE statements MUST include `NOT NULL`. Do NOT include `DEFAULT` values on columns where the entity annotation does not declare a default.
- **Status**: The current `MIGRATION_1_2` (`AppDatabase.kt:35`) correctly includes `NOT NULL` on all PK and required columns. Pitfall retained as a permanent constraint reminder in PITFALLS.md; the migration-specific framing here is historical.

## 2026-05-20 (SUPERSEDED, origin: PITFALLS.md, reason: #244 fix landed): Session drag-reorder lost after Add/Delete/Duplicate
- Drag reorder mutated only the in-memory `MainFragment.sessions` ArrayList — ranks were NOT written to DB. Actions like `addNewSession()`, `onCardDeleteSession()`, and `onCardCopySession()` called `suspendUpdateSessionList()` which cleared the in-memory list and reloaded from DB, silently overwriting drag order. **Fix**: persist `sessions[i].rank = i` at the TOP of `suspendUpdateSessionList()` before reloading.
- **Status**: Fixed in #244. Rank persistence now happens in both `MainFragment.onPause()` and at the top of `suspendUpdateSessionList()` (see CONVENTIONS.md).

## 2026-05-24 (SUPERSEDED, origin: PITFALLS.md, reason: #253 fix landed): Dual-selection after removeItem/insertItem/moveItem
- `SessionListAdapter.removeItem()`, `insertItem()`, and `moveItem()` never updated `selectedPosition`. When `setSelectedPosition()` was called afterward, `previous = selectedPosition` was stale. **Fix**: replaced targeted `notifyItemChanged(previous/selectedPosition)` with full-list refresh `for (i in items.indices) notifyItemChanged(i)`.
- **Status**: Fixed in #246. Full-list refresh is now the convention (see CONVENTIONS.md).

## 2026-05-13 (SUPERSEDED, origin: PITFALLS.md, reason: #254 fix landed): Espresso `clearText()` + `typeText()` race condition
- `clearText()` and `typeText()` are separate IME actions that can interleave, causing duplicated characters (e.g., "UUpdated Session Name"). **Fix**: always use `replaceText()` — it atomically replaces field content without going through the IME.
- **Status**: Fixed in #254. `replaceText()` is now the convention (see CONVENTIONS.md).

## 2026-05-19 (SUPERSEDED, origin: PITFALLS.md, reason: #253 fix landed): Stale in-memory write in `suspendUpdateSessionList()`
- Writing stale `sessions` objects back to DB in `MainFragment.suspendUpdateSessionList()` overwrites edits made by other fragments (e.g., `SessionEditFragment.onPause()` saving a new name). **Fix**: rank persistence is handled by `onPause()`. The `onResume()` path must only read fresh data, never write stale objects.
- **Status**: Fixed in #253.

## 2026-05-16 (SUPERSEDED, origin: PITFALLS.md, reason: behavior changed): Emulator snapshot `-no-snapshot-save` prevents state persistence
- The default `SNAPSHOT_FLAG="-no-snapshot-save"` in `run-instrumentation.sh` caused the emulator to load snapshots on boot but discard all state changes on shutdown, losing system configuration between runs. **Fix**: changed default to `SNAPSHOT_FLAG=""` so snapshots are saved on shutdown. `--cold-boot` (`-no-snapshot`) remains available.
- **Status**: Applied in #254. Current convention documented in CONVENTIONS.md.

## 2026-05-19 (SUPERSEDED, origin: PITFALLS.md, reason: migration gone after #208): Migration CREATE TABLE: PK-Spalte braucht explizites `NOT NULL`
- (German original.) Room validiert die migrierte DB via `PRAGMA table_info`, das für PK-Spalten auch dann `notnull=0` zurückgibt wenn SQLite sie implizit als NOT NULL behandelt. `id INTEGER PRIMARY KEY AUTOINCREMENT` reicht nicht — es muss `... NOT NULL` heißen.
- **Status**: Same as the English entry above. Current `MIGRATION_1_2` complies.

## 2026-05-19 (SUPERSEDED, origin: PITFALLS.md, reason: migration gone after #208): Migration CREATE TABLE: kein `DEFAULT` ohne Entity-Default
- Room prüft `dflt_value` im Schema-Validator. Ein `DEFAULT 0` auf einer Spalte, für die das Entity keinen Default deklariert, führt zu `Migration didn't properly handle`.
- **Status**: Historical migration-era pitfall. Current `MIGRATION_1_2` has no such defaults.

## 2026-05-19 (SUPERSEDED, origin: PITFALLS.md, reason: migration gone after #208): Migration tests via `RoomMigrationTest`
- Tests rufen `MIGRATION_X_Y.migrate(db)` direkt auf einer via `FrameworkSQLiteOpenHelperFactory` erstellten V-Datenbank auf. `MigrationTestHelper` von `room-testing` wird nicht verwendet.
- **Status**: Historical. The current migration test for `MIGRATION_1_2` follows the modern convention in CONVENTIONS.md.

## 2026-05-19 (SUPERSEDED, origin: PITFALLS.md, reason: migration gone after #208): bellId = 0 silently breaks FK
- Before V8 FK constraint, `bellId = 0` was silently allowed in the DB. Multiple sections with different bells but all having `bellId = 0` collapsed into a single entry in `deriveBellVolumesFromSections()`. As of V8, the FK constraint prevents `bellId = 0` at the DB level.
- **Status**: The FK constraint (now in V2) prevents this. `DbOperations.insertSection()` defaults `bellId=0` to the demo bell. Active constraint reminder retained in PITFALLS.md.

## 2026-05-19 (SUPERSEDED, origin: PITFALLS.md, reason: superseded by sanitizeBellUris): New sections with bellId=0 fail FK in V10
- `SectionEntity.bellId` defaults to 0 (no bell selected). The FK constraint `bellId → bells._id` rejects this at insert. `DbOperations.insertSection()` now defaults to the demo bell when `bellId <= 0`.
- **Status**: Active behavior, but the "V10" version reference is stale (we are on V2). The constraint and default behavior are the same in V2. Active reminder retained in PITFALLS.md without the V10 reference.

## 2026-05-13 (SUPERSEDED, origin: PITFALLS.md, reason: #255 fix landed): Stale WAL/SHM files corrupt restored database
- When `BackupManager.restoreEntries()` overwrites the database file, the old `-wal` and `-shm` companion files remain. On reopen, SQLite applies stale WAL content on top of the new database, causing corruption. **Fix**: Always delete `-wal` and `-shm` after overwriting the database file and before reopening.
- **Status**: Fixed. Active constraint reminder retained in PITFALLS.md.

## 2026-05-13 (SUPERSEDED, origin: PITFALLS.md, reason: #255 fix landed): `openOutputStream(uri)` does NOT truncate
- Writing to a content URI via `contentResolver.openOutputStream(uri)` (without mode) opens the existing file at byte 0 but keeps the file's length. For ZIP backups, the old End-of-Central-Directory remains at the tail, causing `unzip -t` to report CRC mismatch. **Fix**: Always use `openOutputStream(uri, "wt")` to truncate.
- **Status**: Fixed in #237. Active constraint reminder retained in PITFALLS.md.

## 2026-06-13 (SUPERSEDED, origin: PITFALLS.md, reason: #255 fix landed): `fos.fd.sync()` required before `fos.close()` on restore
- Without `fsync`, the restored database file may not be committed to disk before Room reopens it, causing `SQLiteDatabaseCorruptException`. **Fix**: Call `fos.fd.sync()` before `fos.close()` in `BackupManager.receiveBytes()`.
- **Status**: Fixed in #255. Active constraint reminder retained in PITFALLS.md.

## 2026-05-19 (SUPERSEDED, origin: PITFALLS.md, reason: bellUri columns removed in #199, baked into V2): Room migration must match Entity annotations exactly
- When adding `@ForeignKey` or `@Index` to an Entity, the migration SQL must produce the EXACT schema Room expects — including ALL foreign keys AND ALL indices with the EXACT names Room generates. A missing index causes `IllegalStateException: Migration didn't properly handle: <table>`.
- **Status**: Permanent constraint — but the migration-specific framing is historical. Active reminder retained in PITFALLS.md in generalized form.

## 2026-05-24 (SUPERSEDED, origin: PITFALLS.md, reason: #208 clean slate): F-Droid dynamic version incompatible with auto-update
- `VersionTagSource`/`CommitCountSource` dynamically compute versionCode/versionName from git tags. F-Droid's `UpdateCheckData` regex can only match static literals. **Fix**: Convert to static `versionCode = N` / `versionName = "X.Y.Z"`.
- **Status**: Applied (version is now static). Active F-Droid convention reminder retained in CONVENTIONS.md.

## 2026-05-24 (SUPERSEDED, origin: PITFALLS.md, reason: knowledge no longer needed): `_minProficiency` was never checked
- The underscore-prefixed parameter in `runOne()` was dead code. Models below proficiency threshold were never skipped despite `--min-proficiency` flag. **Fix**: Renamed to `minProficiency` and actual check added.
- **Status**: Fixed in #222.
