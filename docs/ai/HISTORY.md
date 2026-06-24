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

## 2026-06-15 (SUPERSEDED 2026-06-15, origin: code, reason: #270 correct freezer flags): Wrong app-freezer disable flag
- **Attempt**: Commit `b3c9837` tried to disable the API-33+ app freezer with `adb shell device_config put activity_manager native_with_freezer false`.
- **Why it failed**: `activity_manager native_with_freezer` is the wrong namespace; the freezer is actually controlled by `settings global cached_apps_freezer` + the boot flag `activity_manager_native_boot use_freezer`. The test process kept being frozen (`ActivityManager: freezing …zazentimer.debug.test` 15× in logcat), producing the API-34 `am instrument` hang.
- **Superseded by**: Correct flags in commit `0c79ad0`; active rule in PITFALLS.md ("App freezer freezes the instrumented test process").

## 2026-06-15 (SUPERSEDED 2026-06-15, origin: PITFALLS.md, reason: case-correct process match): `pkill -f "qemu.*android"` was dead code
- **Attempt**: Teardown scripts used `pkill -9 -f "qemu.*android"` to clean up qemu between runs.
- **Why it failed**: qemu's cmdline contains `Android` (capital A, from the SDK path) and `-avd`, never lowercase `android`. The pattern never matched; only the accidental `pkill -f "emulator.*-avd"` (matching the SDK path) did any work, fragilely.
- **Superseded by**: `pgrep -f "qemu-system-x86_64"` (matches the binary name directly) across stop-emulator.sh / start-emulator.sh / kill-test-run.sh; teardown centralized in `emulator_graceful_kill`. Active rule in PITFALLS.md.

## 2026-06-17 (SUPERSEDED 2026-06-20, origin: DECISIONS.md, reason: #270 follow-up implemented): Keep `runBlocking`-in-`onPause` as-is
- **Choice**: Leave the `runBlocking { … }` DB writes in the editing fragments' `onPause` unchanged. Do NOT async-ize them now.
- **Reason**: The only place this pattern actually broke something was `MainFragment.onPause` rank persistence, already fixed in #273 via drop-time `onDragEnd` (so that `onPause` save is now just a redundant safety net). The remaining `runBlocking`s worked — edits persisted; the one real deadlock (`withTransaction`-in-`runBlocking`) was already removed in 768ed2b. Converting was marginal: the main win is less main-thread jank on navigation (~ms), weighed against new app-scope infra + a subtle async write/read-ordering risk concentrated in the two **load-bearing** saves (`SessionEditFragment` name/desc, `SectionEditFragment`), where the jank benefit is smallest and the risk highest. Poor risk/reward → defer.
- **Revisit trigger**: re-open if (a) navigation jank becomes noticeable, (b) a `runBlocking`-on-main deadlock reappears, or (c) someone re-adds `withTransaction` inside one of these. The trigger was met after #268 stabilized the Hilt/repository layer, and the conversion was implemented via an app-scoped `CoroutineScope(SupervisorJob()+Dispatchers.IO)` with synchronous UI-value capture in `onPause()`.
- **Superseded by**: 2026-06-20 decision "Migrate remaining production `runBlocking` callsites to an app-scoped CoroutineScope (#270 follow-up)".

---

## 2026-06 cycle (origin: this cycle's commits, reason: bugs fixed and relocated from active discovery)

The following bugs were discovered and fixed during the 4-API matrix + #291 + #292 work on 2026-06-22. They were never in active PITFALLS.md long-term — the fix landed in the same cycle. Entries preserved here so future readers understand the context behind the corresponding active rules.

## 2026-06-22 (origin: PITFALLS.md temporary, reason: fixed in `bb58408` same day): `ZazenTimerBackupTest.setup` used `java.nio.file.Paths` on API <26
- The test's `@Before` called `Paths.get(context.noBackupFilesDir.toURI())` to create the `demo_sessions_created` marker file. `java.nio.file.Paths` was added in Android API 26 — on API 23 the test process threw `NoClassDefFoundError` at setup, before any test method ran. **Fix**: `context.noBackupFilesDir` is already a `java.io.File` — no Path conversion needed; replaced with `File(context.noBackupFilesDir, "demo_sessions_created").createNewFile()`.

## 2026-06-22 (origin: PITFALLS.md temporary, reason: fixed in `4f83abf` same day): `DatabaseOwner.close()` WAL checkpoint cursor never consumed
- The close path called `db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").close()` without ever reading the returned `Cursor`. Android's `Cursor` executes its underlying statement lazily — `close()` on an unconsumed cursor releases the statement **without running the PRAGMA**. The checkpoint was silently skipped. On API ≤ 27, `RoomDatabase.close()` triggers SQLite's auto-checkpoint on last-connection-close, masking the bug; on API ≥ 31 that auto-checkpoint is no longer reliable. `BackupManager.backup()` ZIPs only the main DB file (not `-wal`), so the backup captured stale main-DB data — losing 1 of 8 sections on API 31, 1 of 2 sessions on API 35. **Fix**: consume the cursor via `cursor.use { cursor.moveToFirst() }` to force PRAGMA execution; log the `busy`/`log`/`checkpointed` row for diagnostics.

## 2026-06-22 (origin: PITFALLS.md temporary, reason: fixed in `702af05` same day): Race in `resetDatabaseForTest` vs `lifecycleScope.launch` (FK crash in `DuplicateSessionTest` on API 31)
- `ZazenTimerActivity.onCreate` launches a `lifecycleScope.launch { sanitizeBellUris(); createDemoSessions() }`. The test's `@Before` then calls `resetDatabaseForTest()` via `onActivity {}` on the main thread. Although `resetDatabaseForTest` uses `runBlocking(Dispatchers.IO)`, Room's suspend functions use `startUndispatchedOrReturn` — once the activity coroutine enters Room's suspend path it keeps running on Room's `TransactionExecutor` threads, fully independent of the main thread being blocked. Race window: Flow A's `createDemoSessions()` inserts session S; Flow B's `readSessions()` (queued on Room's 2-thread executor) returns S; Flow B's delete loop removes S (CASCADE removes sections); Flow A's `createSection(S, ...)` → `SectionRepository.insert` with `fk_session=S` → `FOREIGN KEY constraint failed`. Exposed specifically on slower API 31 emulator (on faster emulators the activity coroutine happened to finish before Flow B's read). **Fix**: (1) wrap `sanitizeBellUris()` body in `appDb().withTransaction` (atomic, concurrent calls serialize); (2) `resetDatabaseForTest()` cancels the activity's `initializationJob` and `runBlocking { join() }` before mutating DB — eliminates the concurrency at its source.

## 2026-06-22 (origin: PITFALLS.md temporary, reason: fixed in `bb349bc` same day): `SessionRankPersistenceTest` drag long-press navigating to edit on API 35
- The drag ViewAction injected `ACTION_DOWN` on the drag handle, then `ACTION_MOVE`s + `ACTION_UP`. The drag-handle `OnTouchListener` returns `false`, so `ACTION_DOWN` propagates to the itemView and schedules a long-press callback (~400ms via `ViewConfiguration.getLongPressTimeout()`). `ItemTouchHelper` then intercepts MOVE/UP, so the itemView never receives `ACTION_CANCEL` — the pending long-press fires and navigates to `SessionEditFragment` (`SessionListAdapter.setOnLongClickListener → onEditSession`). On the API 35 emulator this races the framework's own cancellation and reliably fires within the test window, leaving the test on the edit screen instead of the main screen. The "Settings overflow not found" failure was actually the SessionEditFragment's overflow menu (`session_edit_menu`), not a clipped main-screen popup. **Fix**: call `view.cancelLongPress()` + `dragHandle.cancelLongPress()` immediately after injecting `ACTION_DOWN`. Sub-agent's commit message originally mis-attributed this to issue #291; the actual #291 is unrelated audio-fixture work.

## 2026-06-22 (origin: PITFALLS.md temporary, reason: fixed in `071c264` same day): `BellSanitizer.importOrphanedBellFiles` uncaught `BellImportException` crash
- `BellSanitizer.importOrphanedBellFiles:147` called `BellValidator.validate(context, uri)` without try-catch when re-importing orphaned `bell_*` files found on disk. A single corrupt bell file (failed download, partial backup restore, leftover dummy content from `deleteCustomBell_removesBell` test) would throw `BellImportException` uncaught into the `sanitizeBellUris` coroutine running in `ZazenTimerActivity.onCreate` → process crash → on next launch the same file is still there → crash loop. **A user in this state could never open the app.** Discovered while verifying #291 (attempt 1 of API 35 run crashed during `deleteCustomBell_removesBell` — an existing test staging a dummy-content bell file; the script's auto-retry masked it on the final PASS summary). **Fix**: per-file try-catch, `Log.w(TAG, "Orphaned bell file X is invalid audio, deleting", e)`, delete the corrupt file, continue.

## 2026-06-22 (origin: PITFALLS.md temporary, reason: fixed in `071c264` same day): `ManageBellsFragment` toast showed raw `e.message`
- The import-failure toast used `e.message ?: getString(R.string.bell_import_failed)`. `BellImportException(msg, cause)` always sets `message` from the cause — the Elvis fallback was unreachable. Users saw `"Prepare failed.: status=0x1"` (raw `IOException` from `MediaPlayer.prepare()`) instead of `"Failed to import bell"`. **Fix**: always use `getString(R.string.bell_import_failed)`; `Log.w(TAG, "Bell import failed", e)` for diagnostics.

## 2026-06-22 (origin: PITFALLS.md #41 already documented, reason: fixed in `f6badd3` same day): `SectionEditTest.editSectionConfig_opensEditor` + `MeditationServiceTest.clickStopConfirmButton` missing `.inRoot(isDialog())`
- Both clicked `android.R.id.button1` (AlertDialog / TimePickerDialog OK button) without targeting the dialog root. Under `EDGE_TO_EDGE_ENFORCED` (API 35+/36+), the activity window loses focus when the dialog appears → Espresso's default root matcher waits 10s for window focus → `RootViewWithoutFocusException`. The script's auto-retry masked it. **Fix**: chain `.inRoot(isDialog())` before `.perform(click())`. PITFALLS #41 already documents the general constraint; the convention update in CONVENTIONS.md now mandates auditing every dialog-button click. Filed as #292.

## 2026-06-24 (SUPERSEDED 2026-06-24, origin: PITFALLS.md, reason: #289 auto-tag removed): Auto-tag FAILED_APIS leak
- **Original entry**: Ensure `run-instrumentation.sh` clears `FAILED_APIS` on retry success so auto-tagging functions correctly.
- **Origin**: PITFALLS.md
- **Reason**: The entire auto-tag mechanism was removed in #289 (replaced with a heads-up banner). The `FAILED_APIS` clear-on-retry logic remains in the script, but the pitfall about auto-tagging correctness is moot.

## 2026-06-24 (origin: discovered/fixed same day, reason: #289): `Assume.assumeTrue()` false-green on API 30 backup-restore tests
- `BackupRestoreInstrumentedTest` used `Assume.assumeTrue(zipFile.exists())` to guard on the fixture. The script pushed the fixture via `adb push` to `/sdcard/Android/data/<pkg>/files/`, which failed on API 30 scoped storage (permission denied). The `Assume` skip caused `AndroidJUnitRunner` to report `OK (4 tests)` with 0 dots and exit 0 — a false green. The #289 issue went unnoticed until a targeted `--api 30` run revealed Phase 2 had zero dots. **Fix**: self-provision fixture from `app/src/androidTest/res/raw/` (no `adb push`); add `FailOnAssumptionSkipListener` as a future-regression guard; remove the dot-parser (GPU log interleaving broke it on API 34).
