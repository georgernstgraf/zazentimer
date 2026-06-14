# Pitfalls

Things that do not work, subtle bugs, and non-obvious constraints.
Read this file carefully before making changes in affected areas.

## Active Codebase & System Constraints

- **Service Binding Race**: Fragments attempting to interact with `MeditationService` before `onServiceConnected` caused NPEs or lost commands; use `MeditationRepository` as the stable intermediary.
- **UTP / API 35 "0 tests found"**: AGP 9.1.1 UTP runner may report "0 tests found" on API 35. If it reappears, the `am instrument` fallback remains in `run-instrumentation.sh`.
- **Emulator Hardware**: Never use `-target google_apis` with newer emulators (36.5.10+); use `-target android`.
- **Database Race**: DB operations are async; without `IdlingResource`, tests may read old data before a write finishes (ensure `withIdling { ... }` is used in `DbOperations`).
- **java-test-fixtures Plugin Conflict**: Adding `id("java-test-fixtures")` to the plugins block conflicts with AGP's built-in `testFixtures { enable = true }`. Only use the AGP block.
- **Kotlin Init Order NPE**: Always declare LiveData/StateFlow fields before the `init` block that uses them.
- **runBlocking(Dispatchers.Main) Deadlock**: Calling `runBlocking(Dispatchers.Main)` from the Main thread deadlocks. Use `runBlocking { withContext(Dispatchers.Main) { } }` or avoid blocking calls on Main.
- **Captured Null ServiceConnection**: Always read mutable fields directly inside coroutine closures rather than capturing local vals before binding.
- **systemd-oomd Kills Emulators**: Reduce emulator memory to `-memory 2048` and run cache-clearing commands if needed to avoid OOM kills on VPS hosts.
- **No orchestrator with am instrument**: Set `execution = "HOST"` in `build.gradle.kts` to match the `am instrument` single-device test runner execution mode.
- **Emulator Not Advertising ADB**: If `adb wait-for-device` hangs, use `tmux` or ensure the ADB server is restarted (`adb start-server`).
- **Hilt @TestInstallIn requires `replaces`**: Omitting `replaces` from `@TestInstallIn` causes KSP processor failure.
- **@TestInstallIn replaces drops ALL bindings**: Every binding needed from a replaced module must be re-provided in the test module.
- **kotlinx-coroutines-test for instrumented tests**: Must add `androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")` separately for `androidTest/`.
- **Auto-tag FAILED_APIS leak**: Ensure `run-instrumentation.sh` clears `FAILED_APIS` on retry success so auto-tagging functions correctly.
- **LLM Translation Hallucinations**: Low-resource LLM translations can silently destroy `%s` / `%1$d` placeholders, causing crashes. Prompt sub-agents to fall back to English if confidence is low.
- **AGP 9.x disallowKotlinSourceSets**: Keep `android.disallowKotlinSourceSets=false` in `gradle.properties` to prevent ktlint conflicts.
- **SQLite DROP COLUMN**: SQLite versions before 3.35.0 (Android < 10) don't support `DROP COLUMN`. Re-create tables if migrating older databases.
- **ktlint vs detekt formatting conflicts**: Use block body format instead of single-line expression body when both rules conflict on line length.
- **BellPlayer onDone race**: Use `stopping` flag guard in `finishAfterLastBell()` to prevent double cleanup when `stopImmediate()` is called during playback.
- **Xvfb crash cascades**: The script restarts Xvfb per API level via `start_xvfb()` to prevent headless VPS crashes.
- **Xvfb not ready on start**: Always poll with `xdpyinfo -display :99` to ensure readiness before launching emulators.
- **Zombie qemu processes survive adb emu kill**: Use `pkill -9 -f "qemu.*android"` between runs to clean up ports.
- **Unstaged changes are fragile**: Commit important work promptly to prevent multi-agent overwrites.
- **ktlintFormat removes annotation imports in derived test classes**: Always verify compilation with `./gradlew compileDebugAndroidTestKotlin` after running `ktlintFormat`.
- **nohup inside bash tool is NOT background-safe**: Use `echo "cmd" | at now` to schedule background jobs via `atd`.
- **at job output goes to mail**: Always redirect `>/dev/null 2>&1` in `at` jobs to avoid lost/blocked output.
- **Package cleanup kills system services on API 36**: Skip package uninstalls on API 36+ to prevent package-manager broken pipes.
- **service check activity output varies by API**: Grep for case-insensitive `activity` to support all system check outputs.
- **Espresso.onIdle() in @Before crashes**: DB setup in `@Before` is synchronous; never call `onIdle()` there.
- **JUnit Timeout rule vs SystemClock.sleep**: Never use `SystemClock.sleep` inside tests as it blocks the main thread and defeats timeout rules. Use `onIdle()` or `Until.hasObject()`.
- **android.enableAdditionalTestOutput=false crashes AGP 9.x**: Do NOT use this property with AGP 9.x.
- **UTP runner writes to /sdcard on device**: Ensure test manifest declares `WRITE_EXTERNAL_STORAGE` for API <=32 to prevent permissions errors.
- **Gradle pipe buffering swallows progress lines**: Use `stdbuf -oL ./gradlew` to force line buffering on piped outputs.
- **RootViewWithoutFocusException on API 36 for AlertDialog**: Use `.inRoot(isDialog())` when interacting with dialog buttons under edge-to-edge enforcement.
- **Play Console Invite Error (64F4C82A)**: Invite service accounts with specific "App permissions" instead of "Admin (All)" as a workaround.
- **Hilt KSP import conflict**: Use fully-qualified references (`at.priv.graf.zazentimer.Constants.DEFAULT_BELL_VOLUME`) instead of importing Constants directly in files processed by Room KSP.
- **Android Auto Backup & demo sessions**: With backup enabled, check the actual database state rather than a SharedPreferences first-start flag which is restored on reinstall.
- **Room migration must match Entity annotations exactly**: Migration SQL must produce the EXACT schema Room expects, including index names.
- **Migration CREATE TABLE: PK-Spalte braucht explizites NOT NULL**: Primary key columns in migrations MUST include `NOT NULL` in SQL to satisfy Room's validator.
- **Migration CREATE TABLE: kein DEFAULT ohne Entity-Default**: SQL table definitions in migrations must not specify default values unless declared in entity annotations.
- **Migration tests via RoomMigrationTest**: Create baseline databases manually from JSON files to test migrations rather than using `room-testing` helper.
- **bellId = 0 silently breaks FK**: The FK constraint prevents this. `DbOperations.insertSection()` defaults `bellId` to the demo bell if unset.
- **New sections with bellId=0 fail FK in V2**: Ensure all manual DAO insertions (like tests) set a valid `bellId`.
- **Stale WAL/SHM files corrupt restored database**: Overwriting a DB file keeps `-wal` and `-shm` files; always delete them during restore before reopening the DB.
- **Bell URIs differ between debug and production**: `sanitizeBellUris()` runs at every startup in `ZazenTimerActivity.onCreate()` to heal package-name changes and backup-mismatch URIs.
- **Backup fixture must go to /sdcard/Download/**: Internal directories are inaccessible to SAF on API 30+; push to `/sdcard/Download/` and grant `MANAGE_EXTERNAL_STORAGE`.

## Prisma / Deno / Translation Pipeline Constraints

- **prismaCheckSchema fails after DB migration**: Device schema pull regenerates `current/schema.prisma`. It will fail until a human updates `desired/schema.prisma` (human-only).
- **-noaudio auto-detection via $DISPLAY fails with Xvfb**: Always pass `-noaudio` explicitly when launching emulators in virtual buffers.
- **Subshell redirect doesn't capture background process output**: Redirect process output inside the function using `>> "$logfile" 2>&1 &`.
- **AppCompat Theme & Material text appearances**: Use explicit sizing and styles for TextViews to prevent unstyled fallbacks on AppCompat base themes.
- **PRAGMA via $executeRawUnsafe schlägt fehl**: Use `$queryRawUnsafe` for PRAGMAs since they return rows.
- **JavaScript in <script>-Tags ist nicht self-closing**: Always close script tags with `</script>` explicitly.
- **Deno 2.7.14 has no native DOMParser**: Parse files with regex or pull `npm:xmldom`.
- **openai-whisper pip install is 106+ MB**: Use the static `whisper_languages.json` instead of importing the full python library.
- **SQLite ALTER TABLE ADD CHECK not supported in Prisma**: Define CHECK constraints in the original init migration.
- **Prisma SQLite enum sorts alphabetically**: Use `Int` with CHECK constraint for ordered numerical ranges.
- **prisma format doesn't validate raw SQL**: DB-level checks fail only at `push`/`migrate dev` time.
- **prisma generate with runtime = "deno"**: Output path must map to `deno.json` import map.
- **.env file location matters**: Active `.env` files must live relative to schema directories (e.g. `prisma/translations/.env`).
- **3 ISO 639-3 duplicates across locales**: `por`, `srp`, `zho` cover multiple regional variants — do not make `iso_639_3` unique.
- **32 locales lack Whisper support**: Set `whisper_response = null` and handle the null in code.
- **new URL("file.json", import.meta.url)**: Standard Deno path resolution rule.
- **Pre-push hook installed as symlink**: symlink `.git/hooks/pre-push` to `scripts/git-hooks/pre-push`.
- **Missing AVDs cause gradle to fail**: Check and skip missing AVDs with `avdmanager list avd` inside scripts.
- **PrismaClient + Deno.serve: module-level client loses connectivity**: Always instantiate a fresh client per request inside `Deno.serve` to avoid socket locks.
- **PrismaClient concurrent init in Deno**: Serialize client creation via a promise queue (`withPrisma`) to prevent concurrent `node:fs` imports.
- **Hono HTTPException requires app.onError handler**: Add global error handlers to return proper bodies.
- **Prisma P2002 returns empty response**: Global error handler must capture unique-constraint errors.
- **Basic Auth required for Opencode API**: Basic auth with `OPENCODE_SERVER_*` environment vars is required.
- **Parts-Format im Response**: Extract JSON from `type: "text"` parts, not parts[0].
- **Model-Eigenidentifikation**: Extract model names using `extractModelName()`.
- **System + Model per Message**: Send `system` with every message request.
- **zai (Zhipu AI) Provider**: Rerun token rotations to fix GLM model connection.
- **kimi-k2.6 / opencode-go is slow**: Limit locale loops or use DeepSeek.
- **Prisma v6 library engine intermittent blocking**: Re-instantiate client per query to bypass locks.
- **connectedDebugAndroidTest ignores ANDROID_SERIAL**: Gradle targets all connected devices; use `am instrument` and target serial directly.
- **am instrument has no -e excludeAnnotation**: Discover classes from source tree instead of relying on exclude annotations.

---

## Superseded & Relocated Pitfalls

The following fixed-bug pitfalls have been relocated to `docs/ai/HISTORY.md` for historical reference:

- **Espresso clearText() + typeText() race condition** → Relocated to `HISTORY.md` (Fixed in #254; use `replaceText()` convention).
- **Session drag-reorder lost after Add/Delete/Duplicate** → Relocated to `HISTORY.md` (Fixed in #244; save ranks before reload).
- **Dual-selection after removeItem/insertItem/moveItem** → Relocated to `HISTORY.md` (Fixed in #246; use full-list refresh).
- **Stale in-memory write in suspendUpdateSessionList()** → Relocated to `HISTORY.md` (Fixed in #253).
- **Emulator snapshot -no-snapshot-save prevents state persistence** → Relocated to `HISTORY.md` (Fixed in #254).
- **Stale WAL/SHM files corrupt restored database** → Relocated to `HISTORY.md` (Fixed in #255).
- **openOutputStream(uri) does NOT truncate** → Relocated to `HISTORY.md` (Fixed in #237; use `"wt"` mode).
- **fos.fd.sync() required before fos.close() on restore** → Relocated to `HISTORY.md` (Fixed in #255).
- **Room MIGRATION_1_2: PK must be NOT NULL explicitly** → Relocated to `HISTORY.md` (Baseline V2 migration complies).
- **Migration CREATE TABLE: PK-Spalte braucht explizites NOT NULL** → Relocated to `HISTORY.md`.
- **Migration CREATE TABLE: kein DEFAULT ohne Entity-Default** → Relocated to `HISTORY.md`.
- **Migration tests via RoomMigrationTest** → Relocated to `HISTORY.md`.
- **bellId = 0 silently breaks FK** → Relocated to `HISTORY.md` (FK bakes directly into V2).
- **New sections with bellId=0 fail FK in V10** → Relocated to `HISTORY.md` (V10 story is historical).
