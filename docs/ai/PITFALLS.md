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
- **Zombie qemu processes survive adb emu kill**: The reliable match is `pgrep -f "qemu-system-x86_64"` — NOT `"qemu.*android"` (case mismatch: qemu args contain `Android`, capital A, so that pattern never matches and is dead code in older scripts). Teardown goes through `emulator_graceful_kill` (stop-emulator.sh): polls CPU/IO/D-state, no time cap while progressing, SIGTERM→SIGKILL only after 60s sustained idleness, and purges the AVD snapshot on SIGKILL.
- **Unstaged changes are fragile**: Commit important work promptly to prevent multi-agent overwrites.
- **ktlintFormat removes annotation imports in derived test classes**: Always verify compilation with `./gradlew compileDebugAndroidTestKotlin` after running `ktlintFormat`.
- **Background-Jobs im bash tool**: `nohup cmd > logs/...log 2>&1 &` ist der zuverlässige Weg (Prompt kehrt sofort zurück, Prozess erbt `$DISPLAY`; poll via `tail`/`fuser`). Eine volle API-Matrix-Instrumentierung läuft ca. 2,5 h — alle ~7 min in die Logdatei schauen. **`at now` NICHT verwenden**: `at` startet in einer sauberen Umgebung und verliert `$DISPLAY` (sowie andere ENV-Vars), wodurch `run-instrumentation.sh` die instrumentierten Tests überspringt.
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
- **App freezer freezes the instrumented test process (API ≥ 31)**: Disable with `adb shell settings put global cached_apps_freezer disabled` + `adb shell device_config put activity_manager_native_boot use_freezer false` (the latter is a boot flag → needs a guest reboot to take effect). The earlier `activity_manager native_with_freezer` flag is the WRONG namespace and does nothing. `run-instrumentation.sh` provisions this marker-gated (one reboot per AVD); `create-emulator-snapshots.sh` bakes it into the baseline for API ≥ 31.
- **adb ignores SIGTERM when blocked on the device socket**: Plain `timeout N adb shell …` hangs forever (timeout sends SIGTERM, adb survives). Use `timeout -s KILL N adb …`. Exit-code rule: command killed by SIGTERM → 124; by SIGKILL → 137. Detect both: `[ $? -eq 124 ] || [ $? -eq 137 ]`.
- **Matrix runs save snapshots on exit for faster subsequent runs**: the emulator saves a `default_boot` snapshot on shutdown so the next run boots in seconds rather than a cold boot. If a post-test snapshot is non-resumable (qemu hangs on next boot), run with `--cold-boot` once or regenerate the baseline via `create-emulator-snapshots.sh`. (The old policy used `-no-snapshot-save` to avoid self-poisoning baselines — removed in #282 in favor of speed, accepting the occasional reconstruction cost.)
- **Post-test snapshots can be non-resumable**: Even a graceful save after a PASSING run can capture guest state that hangs on resume. This is rare but known. The mitigation: `emulator_graceful_kill` already purges snapshots on SIGKILL (truncated in-flight saves). For non-SIGKILL hangs, the remedy is `--cold-boot` once or regenerate via `create-emulator-snapshots.sh`.
- **Guard every best-effort adb call under `set -euo pipefail`**: A bare `adb …` returning non-zero (e.g. `adb shell mkdir -p /sdcard/…` when `/sdcard` isn't ready post-boot) aborts the whole script via the EXIT trap with no summary. Append `|| true` to best-effort adb commands.
- **Never edit a running bash script**: Rewriting a script while `bash script.sh` is mid-execution misaligns bash's byte-offset → it reads garbage (a `# ───` comment separator got executed as a command: `line 845: $'\224\200──': command not found`). Commit git changes only when no instrumentation run is in flight.
- **`declare -A arr` without `=()` is unbound under `set -u`**: `${#arr[@]}` / `${arr[@]}` on a declared-but-empty associative array aborts with "arr: unbound variable". Always `declare -A arr=()`. (Subscript access `${arr[k]:-}` is safe regardless.)
- **UI-test failures may not reproduce under Xvfb (virtual framebuffer)**: claw forces Xvfb (no real `$DISPLAY`). Focus-sensitive Espresso matches (e.g. `AlertDialog` + `.inRoot(isDialog())`) and touch-event-sequencing flows (`ItemTouchHelper` drag/drop) can behave differently under a virtual framebuffer than on a real X11 display. A "passes on claw/Xvfb" result does **not** prove "passes on real X11". Always validate suspected UI-test failures on a real `$DISPLAY` host (`think`) before concluding not-reproducible. #272's 3 API-23 tests pass on claw/Xvfb on **both** x86_64 and x86 32-bit (ABI ruled out), but need real-X11 confirmation.
- **A poisoned baseline snapshot produces false test failures**: `run-instrumentation.sh` boots each API from a saved `default_boot` baseline. A baseline captured after a prior test run can carry bad guest state (leftover windows, odd input-subsystem state) that makes specific tests fail deterministically — even though the code is correct and a fresh cold boot passes. This is why matrix runs use `-no-snapshot-save` and baselines are regenerated only by `create-emulator-snapshots.sh`. If tests fail "deterministically" on one host but pass on a fresh cold boot of another, suspect the baseline snapshot, not the code. (See #272.)
- **Espresso drag tests must drive the drag handle + a full DOWN→MOVE→UP**: with `isLongPressDragEnabled() = false`, `ItemTouchHelper` only starts a drag when the drag-handle view (`R.id.dragHandle`) receives `ACTION_DOWN` (its `OnTouchListener` calls `itemTouchHelper.startDrag(holder)`). A `ViewAction` that injects `ACTION_MOVE` only — or on the row center instead of the handle — never engages `ItemTouchHelper`, so `onMove`/`clearView` never fire and the "drag" is a no-op. Inject `ACTION_DOWN` on the handle's screen center (use `getLocationOnScreen`), then interpolated `ACTION_MOVE`s to the target, then `ACTION_UP`. Otherwise the test silently validates nothing. (See #273 — the old test's `atPosition(0, hasDescendant(withId(sessionName)))` was true for any row and hid the regression.)
- **Persist ItemTouchHelper reorder state at the drop signal (`clearView`), not deferred to `onPause`**: a drag that only mutates the in-memory list and relies on `onPause()` to flush is unreliable on in-app navigation — `runBlocking` inside `onPause` during a fragment transaction doesn't reliably land writes before the returning `onResume` rebuild. Write the reordered ranks in `clearView`/`onDragEnd` (async) so the DB is correct the moment the drag ends. (See #273.)
- **`nohup &` does NOT survive the opencode bash tool's shell exit** (refines #33): `nohup` only handles `SIGHUP`, but the bash tool sends `SIGTERM` to the entire process group when its shell terminates (default 120s timeout). Use `setsid bash -c 'cmd > log 2>&1' </dev/null >/dev/null 2>&1 & disown` — `setsid` creates a new session ID, fully detached from the tool's process group. Verified on `think` during the 4-API matrix run (nohup'd child died mid-`am instrument`, setsid'd child survived). **Never use `at now`** (PITFALLS #33 already covers this — `at` loses `$DISPLAY`).
- **Android Toast windows are invisible to UiAutomator on API 31+**: `UiDevice.findObject(UiSelector().text(...))`, `UiAutomation.windows`, and `findAccessibilityNodeInfosByText()` cannot see system-managed Toast windows. There is NO reliable way to assert Toast text from an instrumented test on API 31+. Workaround: (1) the production catch block must `Log.w(TAG, "...", e)` so logcat proves the catch ran; (2) the test asserts side effects (state NOT mutated + logcat contains the `Log.w` line); (3) reaching the test's assertions without process crash IS proof the catch block executed — an uncaught exception would crash the test process. (See #291 / commit `071c264`.)
- **`run-instrumentation.sh` auto-retry masks attempt-1 crashes**: the script retries Phase 1 once on failure. The final summary shows the LAST attempt's result. A flaky crash on attempt 1 (e.g. `RootViewWithoutFocusException`, FK violation, dialog-focus race) can be invisible if attempt 2 passes. When verifying a fix, ALWAYS grep the per-API logcat (`rg "FATAL EXCEPTION|TestRunner: failed:" logs/api<level>-<date>-logcat.txt`) — do not rely on the final PASS summary alone.
- **Instrumented-test fixtures must live in `app/src/androidTest/res/raw/`, NOT `app/src/test/resources/`**: the `test/resources/` classpath is JVM-only (Robolectric). Instrumented tests run on-device and cannot read those files. Use `app/src/androidTest/res/raw/` (test APK packaging) and access via `InstrumentationRegistry.getInstrumentation().context.resources.openRawResource(R.raw.X)` — note **test context** (`.context`), not target context (`.targetContext`). Raw resource names must be lowercase + underscores, no dots: `goodbell.mp3` → `goodbell_mp3`. The test R class is `at.priv.graf.zazentimer.test.R` (separate from main `R`); alias the import if both are needed: `import at.priv.graf.zazentimer.test.R as TestR`. (See #291.)
- **`e.message ?: getString(R.string.fallback)` is an unreachable-fallback anti-pattern**: when an exception type always populates `message` (e.g., `BellImportException(msg, cause)` always sets it from the cause), the Elvis fallback never fires. Users see raw Java exception text (`"Prepare failed.: status=0x1"`) instead of the friendly string. Always use `getString(R.string.fallback)` for UI display, and `Log.w(TAG, "...", e)` for diagnostics. (See #291 / commit `071c264` — `ManageBellsFragment` import-failure toast.)
- **`BellValidator.validate()` throws `BellImportException` — all background callers MUST catch**: `BellSanitizer.importOrphanedBellFiles` and any other path that validates user-provided/orphaned bell files must wrap each per-file `validate()` call in try-catch. An uncaught `BellImportException` in a `lifecycleScope.launch` (e.g., `ZazenTimerActivity.onCreate`) crashes the app process — and on next launch the same corrupt file is still there, so it crashes again. **A single bad `bell_*.mp3` in `filesDir/` bricks the app on every launch.** Catch per file, `Log.w(TAG, "invalid audio, deleting", e)`, delete the corrupt file, continue. (See #291 / commit `071c264`.)

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
- **`const val` referencing `R.x` triggers Kotlin IR interpreter error**: In an `object`, `const val FOO: Int = R.string.bar` fails with `InterpreterMethodNotFoundError` during `compileDebugKotlin` because the Kotlin compiler tries to fold the constant at compile time but `R` fields aren't available to the IR interpreter. Use `val` (non-const) instead — the field is still effectively final.
- **`prisma/lib/db.ts` connects to the DB at module load**: Line 3 `let prisma = await getPrisma()` is a top-level await that runs `prisma.$connect()` on import. Any `deno test` that imports from `db.ts` — even just `SETTLED_SCORE_THRESHOLD` — pulls in Prisma and needs a live database. Pure, testable modules under `prisma/lib/` (e.g. `settlement.ts`) must NOT import `db.ts`; keep them dependency-free.

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

## Hilt / DbOperations Dissolution (#268)

- **Reopen-safe DAO access for Hilt-singleton repositories**: when a `@Singleton @Inject` repository needs a DAO, it must fetch it **dynamically** from `DatabaseOwner` on each call (`databaseOwner.sessionDao()`), never cache it in a constructor field. Reason: Hilt singletons are built once and can't be rebuilt, but `close()`/`reopen()` (backup-restore flow) recycle the `AppDatabase` connection — a cached DAO ref would point at the closed/stale DB after `reopen()`. `DatabaseOwner` holds the mutable `AppDatabase` and re-derives DAOs from the current instance.
- **API 34 freezer skip-check fooled by `setting=disabled` vs flag-not-applied**: `run-instrumentation.sh` skips freezer re-provisioning when `settings get global cached_apps_freezer` returns `disabled`. But on **fast-boot resume** from a snapshot, the boot-time flag `activity_manager_native_boot use_freezer=false` may NOT take effect (snapshot resume ≠ fresh boot for a boot flag) even though the setting reads `disabled`. Result: the script skips, the freezer is actually active, the test process gets cgroup-frozen → 900s `am instrument` timeout. Remedy: run with `--cold-boot` (full cold boot applies the boot flag), OR re-baseline the AVD such that `cached_apps_freezer` is `null` (so the script re-provisions + reboots). Seen on `claw`/`test_api34`. (See #268 Phase 0 gate, #272.)
- **API 36 `system_server` crash on `claw` after freezer-provisioning reboot**: after `run-instrumentation.sh` provisions the freezer (`cached_apps_freezer=disabled` + `use_freezer=false` + guest reboot), the API 36 emulator's `system_server` can crash (`Lost network stack` / `DeadSystemException: The system died`) — taking the test process down with it. Phase 1 (main tests) passes before the crash; Phase 2 (backup restore) fails. This is `claw`/Xvfb emulator instability, not a code regression (zero app-side FATAL/DI errors in logcat). Not reliably fixable on `claw`; may need `think`/real-X11. (See #268 Phase 0 gate.)
