# Decisions

Architectural and technical decisions made in this project.
Each entry documents WHAT was decided and WHY.

## 2026-05-10: State Management via Singleton Repository
- **Choice**: Moved meditation state from `MeditationService` and UI-level polling in `MeditationViewModel` to a singleton `MeditationRepository`.
- **Reason**: Stabilize state across fragment lifecycles and eliminate UI-driven polling races that caused flakiness in Espresso tests.
- **Considered**: Keeping state in Service (suffered from binding races), using local ViewModel state (lost on navigation).
- **Tradeoff**: Repository must be carefully synchronized; state is global to the app process.

## 2026-05-10: Time Abstraction via ZazenClock
- **Choice**: Introduced `ZazenClock` interface and `SystemClock` implementation.
- **Reason**: Decouple business logic from `System.currentTimeMillis()` to allow deterministic time-travel in tests.
- **Considered**: Static mocks (brittle), manual offset injection.
- **Tradeoff**: Slight increase in boilerplate for dependency injection.

## 2026-05-10: Database Idling via CountingIdlingResource
- **Choice**: Wrapped all `DbOperations` methods with `CountingIdlingResource` increments/decrements.
- **Reason**: Enable Espresso to automatically wait for asynchronous database operations without manual `Thread.sleep()`.
- **Considered**: `IdlingThreadPoolExecutor` (doesn't catch all Room async work), manual waits in tests.
- **Tradeoff**: All DB operations now depend on the idling resource infrastructure.

## 2026-05-10: Script-Based Test Infrastructure over Gradle Managed Devices
- **Choice**: Use `scripts/run-instrumentation.sh` with dynamic AVD resolution instead of Gradle Managed Devices (GMD).
- **Reason**: Heterogeneous developer environments have varying AVD naming conventions (test_apiX, Medium_Phone_API_X, etc.). GMD enforces a single naming scheme, breaking local developer workflows.
- **Considered**: Pure GMD approach (inflexible for mixed environments), ad-hoc bash without Gradle integration (no source of truth).
- **Tradeoff**: Manual emulator lifecycle management in the script; less IDE integration for test launching.

## 2026-05-10: am instrument Fallback for API 31+
- **Choice**: Use `am instrument` CLI command for API 31+ instead of the Gradle UTP runner.
- **Reason**: AGP's Unified Test Platform runner has a known bug on API 35 (and intermittently on 31-34) reporting "0 tests found". `am instrument` bypasses this reliably.
- **Considered**: Staying with Gradle runner and accepting flakiness (unacceptable), using only Gradle runner on all APIs (fails on 35).
- **Tradeoff**: Slightly more complex APK installation flow; extra retry logic for focus errors.

## 2026-05-11: Full detekt cleanup — fix all violations, not suppress
- **Choice**: Fixed all 337 detekt violations across 22 source files instead of adjusting threshold config.
- **Reason**: User wanted proper code quality fixes, not suppressed warnings. Extracted 10 helper classes, ~160 MagicNumbers to named constants, and removed dead code.
- **Considered**: Adjusting `detekt.yml` thresholds (quick but hides problems), selective fixes (partial).
- **Tradeoff**: Large diff (~2400 insertions, ~1450 deletions); 9 new files; risk of regressions in test infra during parallel agent work.

## 2026-05-11: POST_NOTIFICATIONS runtime permission for targetSdk 36
- **Choice**: Added `POST_NOTIFICATIONS` manifest permission + runtime request in `ZazenTimerActivity.onStartPressed()` gated on API 33+.
- **Reason**: targetSdk=36 requires this permission for foreground service notifications; missing it causes guaranteed crash on API 33+.
- **Considered**: Auto-grant only (test-only hack), always-deny handling (blocks meditation start).
- **Tradeoff**: Permission request dialog appears on first meditation start; user can deny and still proceed.

## 2026-05-11: HOST execution mode for instrumented tests
- **Choice**: Changed `execution` from `"ANDROIDX_TEST_ORCHESTRATOR"` to `"HOST"` in `build.gradle.kts`.
- **Reason**: The `am instrument` path doesn't install the orchestrator APK; `HOST` mode matches this reality. Also avoids orchestrator instability (SIGKILL, `clearPackageData` races) on some API levels.
- **Considered**: Installing orchestrator manually in script (complex), keeping orchestrator and accepting flakiness.
- **Tradeoff**: No process-level test isolation; potential state leakage between tests (mitigated by explicit `resetDatabaseForTest()` in `@Before`).

## 2026-05-11: gradleMaxApi=30 — Gradle runner for 29-30, am instrument for 31+
- **Choice**: Set `zazentimer.test.gradleMaxApi=30` in `gradle.properties`.
- **Reason**: Gradle runner (`connectedDebugAndroidTest`) works reliably on API 29-30. API 31+ emulators have split APK install races and UTP bugs. `am instrument` with streamed install is more stable.
- **Considered**: gradleMaxApi=34 (API 34 agent recommended for orchestrator support), gradleMaxApi=29 (too aggressive, API 30 Gradle runner works).
- **Tradeoff**: API 31-36 tests use manual APK install + `am instrument` flow (slightly slower but more reliable).

## 2026-05-12: Translation cleanup — 129 locales, no regional duplicates
- **Choice**: Consolidated 136 locale directories to 129 by removing 7 regional duplicates (en-AU, en-GB, en-IN, es-US, fr-CA, ms-MY, zh-HK) and stripping regional qualifiers from 31 others.
- **Reason**: Regional variants exceeded requirements; base languages cover all target markets. Exception: kept zh-TW (different writing system), pt/pt-BR/pt-PT (distinct dialects), sr/sr-Latn (both scripts), fil/tl (both standards).
- **Tradeoff**: Users in regions like Canada (fr-CA) will see standard French; acceptable for meditation timer.

## 2026-05-12: 5 strings kept in English with translatable="false"
- **Choice**: `app_name`, `character_counter_pattern`, `theme_value_dark/light/system` marked non-translatable.
- **Reason**: Brand name, format patterns, and programmatic enum values must not be translated.
- **Tradeoff**: About text and bell names are now translated (was previously kept in English).

## 2026-05-12: Machine translation via Google Translate + MyMemory fallback
- **Choice**: Use `retranslate.py` with Google Translate primary, MyMemory for unsupported locales (bem, sr-Latn, nus, bo, ks, sat, fil).
- **Reason**: Free APIs; covers 128 of 129 locales. cgg (Chiga) unsupported by both.
- **Tradeoff**: Machine translation quality varies; all locales marked `reviewed: false` for future human review.

## 2026-05-12: Deleted 31 abc_* AndroidX duplicate strings
- **Choice**: Removed all `abc_*` string entries from app's strings.xml and all 129 locale files.
- **Reason**: These are AndroidX/AppCompat library strings provided by the library itself. Duplicating them was redundant and bloated translation scope.
- **Tradeoff**: None — AndroidX provides its own translations automatically.

## 2026-05-14: Zero-warning build — fix all, suppress none
- **Choice**: Fixed all 1,186 lint warnings across 39 categories instead of suppressing or disabling them.
- **Reason**: User explicitly requested strict approach — fix everything, no cosmetic suppression.
- **Tradeoff**: 188 files changed (+581/-1152); deleted 19 empty resource dirs; stripped Material Design private overrides.

## 2026-05-14: Bell volume per session, per unique bell type
- **Choice**: Moved volume from per-Section to per-Session with one volume per unique bell type (bell/bellUri) per session.
- **Reason**: Users with 12+ sections found per-section volume tedious and error-prone. Per-bell-type per-session is flexible (different volumes for different bells) without section-level overhead.
- **Considered**: Single volume per session (simpler but loses flexibility), keep per-section (rejected as UX problem).
- **Tradeoff**: More complex schema (new `session_bell_volumes` table); migration averages section volumes per bell type.

## 2026-05-14: DND uses INTERRUPTION_FILTER_PRIORITY with alarm-allowing policy
- **Choice**: ~~Changed "None" mute mode from `INTERRUPTION_FILTER_NONE` to `INTERRUPTION_FILTER_PRIORITY` with a custom `NotificationManager.Policy` that allows alarms (`PRIORITY_CATEGORY_ALARMS`). Refactored `AudioStateManager` to save `activeMuteMode` at mute time instead of re-reading preferences at unmute time. Simplified DND restore guard to compare only the filter (not the policy). Refactored `Meditation.finishMeditation()` into `stopImmediate()` (stop button) and `finishAfterLastBell()` (natural end) with shared `cleanup()`.~~ **SUPERSeded by #182 (2026-05-16): DND/mute functionality removed entirely. The app no longer modifies the phone's ringer or DND state.**
- **Reason**: ~~`INTERRUPTION_FILTER_NONE` suppressed all audio including alarms. DND restore was failing due to `NotificationManager.Policy.equals()` being unreliable across read cycles. `unmutePhone()` was re-reading preferences which could differ from what `mutePhone()` used. Single `finishMeditation()` had race conditions with `BellPlayer`'s `onDone` callback.~~ Phone has its own DND system; app should not modify it.
- **Considered**: ~~`INTERRUPTION_FILTER_ALARMS` (simpler but less flexible), comparing policy in guard (unreliable), keeping single `finishMeditation()` (race conditions).~~
- **Tradeoff**: ~~Filter-only guard means if user changes DND filter during meditation but keeps same filter value, settings still get restored. `finishAfterLastBell()` guard prevents double-invocation but relies on `stopping` volatile flag.~~ No tradeoff — app is simpler without DND code.

## 2026-05-14: Avg volume migration for bell volumes
- **Choice**: When multiple sections used the same bell with different volumes, the migration takes the average.
- **Reason**: Average preserves the intent of all sections rather than favoring one arbitrarily.
- **Considered**: First encountered volume (biased), maximum (favors loudest).
- **Tradeoff**: Averaged volume may differ from any individual section's original setting.

## 2026-05-16: Xvfb restart per API level in run-instrumentation.sh
- **Choice**: Refactored `run-instrumentation.sh` to restart Xvfb for each API level when running in virtual framebuffer mode (`IS_REAL_DISPLAY=false`).
- **Reason**: Xvfb dies mid-run (OOM, emulator GPU driver conflict) causing cascading failures — all subsequent APIs are lost. Fresh Xvfb per API isolates failures to the affected API only.
- **Considered**: Running each API as a separate script invocation (heavier), adding Xvfb watchdog (complex), accepting data loss on crash.
- **Tradeoff**: ~2s overhead per API for Xvfb startup; `xdpyinfo` polling adds up to 30s per restart (typically <5s).

## 2026-05-16: gradleMaxApi raised from 30 to 36 — AGP 9.1.1 fixes activity resolution
- **Choice**: Set `zazentimer.test.gradleMaxApi=36` in `gradle.properties`, enabling `connectedDebugAndroidTest` for all API levels.
- **Reason**: AGP 9.1.1 resolves the `Unable to resolve activity` bug that existed since AGP 7.2. Tested on API 31 (24/25 pass), 34 (25/25 pass), 36 (23/25 pass). All failures are Espresso UI issues (`NoMatchingViewException`), not AGP Manifest Merger errors.
- **Considered**: Keeping `gradleMaxApi=30` (unnecessary workaround), upgrading to AGP 10.0 (not yet released, expected H2 2026).
- **Tradeoff**: The `am instrument` fallback path remains in `run-instrumentation.sh` but is no longer invoked. Some tests may still fail due to Espresso UI timing issues under Xvfb, but these are test-specific, not AGP-specific.

## 2026-05-16: Per-API logfiles with logcat dumps in run-instrumentation.sh

## 2026-05-16: Modular self-contained API test runs with full logging
- **Choice**: Rewrote `run-instrumentation.sh` with per-run isolation: each API test (and each retry) gets its own Xvfb restart, zombie emulator kill, crash DB archival, logcat clear, and phase-annotated logging.
- **Reason**: Unstaged changes from a previous agent session were accidentally lost by another agent. The lost version had significant improvements visible only from log output: timestamps on every line, phase markers (`API X — Phase: <description>`), ADB command logging, emulator PID capture, crash DB preservation. The committed version had no timestamps, silent ADB commands (`2>/dev/null`), no phase markers, and Xvfb restart only in the outer loop (not on retry).
- **Considered**: Recreating from memory only (fragile), keeping the committed version (loses diagnostic value).
- **Tradeoff**: More verbose logging; crash DB archives under `logs/crashdb-api<level>-<date>/` consume disk space but are gitignored.

## 2026-05-16: Preserve crash DBs instead of deleting them
- **Choice**: `preserve_crash_dbs()` moves `/tmp/android-georg/emu-crash-*.db` to `logs/crashdb-api<level>-<date>/` instead of `rm -rf`.
- **Reason**: Crash DBs may contain important traces for diagnosing failures, especially with `--continue-on-error` where multiple APIs run sequentially. Deleting them destroys forensic evidence.
- **Tradeoff**: Additional disk usage (~few MB per API); mitigated by `logs/` being gitignored.

## 2026-05-16: Bash `&>>` does not background processes
- **Choice**: Changed emulator launch from `&>> "$API_LOG"` to `>> "$API_LOG" 2>&1 &` so `$!` correctly captures the background PID.
- **Reason**: In bash, `&>>` is the append redirect operator (`>>FILE 2>&1`), NOT `& >>` (background + redirect). The emulator must be explicitly backgrounded with `&` to capture its PID and allow the script to proceed to `wait_for_emulator`.

## 2026-05-16: Use `at` scheduler for long-running instrumentation tests
- **Choice**: Launch `run-instrumentation.sh` via the `at` scheduler (`echo "cmd" | at now`) instead of `nohup &` from the bash tool.
- **Reason**: The opencode bash tool kills its shell after the timeout (default 2 min, max 10s for background launches). `nohup` inside the tool's shell still dies with the shell. `at` submits the job to the system `atd` daemon, which runs completely independently of the launching shell.
- **Considered**: `nohup &` (killed by tool timeout), `systemd-run --user` (viable but more complex), `tmux`/`screen` (viable but interactive).
- **Tradeoff**: `at` captures stdout/stderr and tries to mail it — must redirect to `/dev/null` since the script already tees to log files. Requires `at` package installed and `atd` service running.

## 2026-05-16: Test report summarizer script
- **Choice**: Created `scripts/summarize-tests.sh` to parse instrumentation logs and JUnit XML into a markdown report with summary table + failure details.
- **Reason**: Long test runs (4-5 hours, 14 API levels) produce fragmented logs. A summarizer gives an at-a-glance view of what passed/failed and why.
- **Considered**: Manual log inspection (tedious), CI dashboard (not available for local runs).
- **Tradeoff**: Pure bash parsing of logs/XML — fragile if log format changes, but no Python dependency.

## 2026-05-16: Version display on About page
- **Choice**: Added `BuildConfig.VERSION_DISPLAY` field showing `3.0.1+19` (untagged) or `3.0.1` (tagged). No `v` prefix. Tags fetched from GitHub via `git fetch --tags` in `VersionTagSource`.
- **Reason**: Release tags now exist in the repo but were invisible to users. The `+N` suffix follows semantic versioning convention for build metadata.
- **Considered**: Showing `v3.0.1` (user preferred no `v`), omitting version when untagged (less informative).
- **Tradeoff**: `git fetch --tags` adds network dependency to build; fails gracefully if offline (uses local tags).
- **Choice**: All output from each API-level test run is written to `logs/api<level>-YYYY-MM-DD.log` via `tee -a`. On failure, `adb logcat -d` dumps to `logs/api<level>-YYYY-MM-DD-logcat.txt`. Overall run log at `logs/instrumentation-YYYY-MM-DD.log` via `exec > >(tee ...)`. Added `--debug` flag for logcat dumps on green runs too.
- **Reason**: Previous runs lost all diagnostic output to terminal-only — when terminal timed out or Xvfb crashed, stack traces, `am instrument` output, and crash details were irrecoverable.
- **Considered**: Single combined log file (hard to navigate), only logging failures (misses context for intermittent issues).
- **Tradeoff**: ~50-100MB logcat dump per failed API; `logs/` is gitignored so no repo bloat.

## 2026-05-17: API 36+ emulator service stabilization
- **Choice**: Added `service check activity` polling after `boot_completed`, 3x consecutive 10s stabilization checks, and skip package cleanup on API 36+.
- **Reason**: On API 36, `sys.boot_completed=1` fires before `activity`/`package` services are ready. The `clean_device_packages()` function's rapid-fire `adb uninstall` commands caused `Broken pipe (32)` on the `package` service, cascading to kill all system services (`cmd: Can't find service: activity`).
- **Considered**: Longer `sleep` after boot (wasteful), retry loop in Gradle (doesn't fix root cause).
- **Tradeoff**: ~40s added per API level for stabilization; package cleanup skipped on API 36+ means stale test APKs remain (harmless — Gradle replaces them).

## 2026-05-17: Replace SystemClock.sleep with Espresso idioms
- **Choice**: Replaced all 18 `SystemClock.sleep` calls in instrumented tests with `Espresso.onIdle()` and `UiAutomator Until.hasObject()`.
- **Reason**: `SystemClock.sleep()` blocks the main thread and cannot be interrupted by the JUnit `Timeout` rule, causing tests to hang indefinitely. `Espresso.onIdle()` lets the test framework synchronize with the app's message loop.
- **Considered**: `@Test(timeout=...)` per method (doesn't help with `SystemClock.sleep` on main thread), keeping sleeps (causes 25+ minute hangs).
- **Tradeoff**: `onIdle()` can cause `TestLooperManager already held` if called during `@Before` before ActivityScenarioRule is fully initialized — must NOT call `onIdle()` in `@Before` methods.

## 2026-05-17: AbstractZazenTest base class with global timeout
- **Choice**: Created `AbstractZazenTest` with `@Rule Timeout(2, MINUTES)`, `HiltAndroidRule`, and `ActivityScenarioRule`. All 8 test classes extend it.
- **Reason**: Centralizes common test setup and ensures no test runs longer than 2 minutes, preventing hung tests from blocking the entire suite.
- **Considered**: Per-test `@Test(timeout=...)` (verbose, easy to forget), no timeout (25+ minute hangs observed).
- **Tradeoff**: 2-minute timeout may be too short for some tests on slow emulators; can be increased per-class if needed.

## 2026-05-17: inRoot(isDialog()) for AlertDialog interactions on API 36+
- **Choice**: Use `onView(...).inRoot(isDialog())` for Espresso interactions with AlertDialog buttons on API 36+.
- **Reason**: API 36 enforces edge-to-edge (`EDGE_TO_EDGE_ENFORCED` flag on decor view). When an AlertDialog appears, the activity window loses focus, causing Espresso's default root matcher (which requires `hasWindowFocus()`) to throw `RootViewWithoutFocusException`. `isDialog()` matches windows with `TYPE_APPLICATION` (AlertDialog's type) without requiring focus.
- **Considered**: `Thread.sleep()` delays (already timed-out at 10s), catching exception with retry loop (band-aid).
- **Tradeoff**: Import of `RootMatchers.isDialog` adds one extra import; must be placed in correct lexicographic order for ktlint.
- **Module**: `app/src/androidTest/kotlin/at/priv/graf/zazentimer/MainScreenDeadStateTest.kt`

## 2026-05-18: Replace Add Section FAB with 3-dot menu
- **Choice**: Removed the `FloatingActionButton` (+ icon) from `fragment_edit_session.xml` and added "Add Section" to the overflow menu (`session_edit_menu.xml`).
- **Reason**: User wanted consistency with the main screen's 3-dot menu pattern and disliked the white plus symbol.
- **Tradeoff**: Two-tap to add a section instead of one-tap, but cleaner UI.
- **Choice**: Created `Constants.DEFAULT_BELL_VOLUME = 50` as single source of truth. All code constants (`AppDatabase.DEFAULT_VOLUME`, `EntityMapper.DEFAULT_VOLUME`, `Meditation.DEFAULT_BELL_VOLUME`, `SessionEditFragment.DEFAULT_BELL_VOLUME`, etc.) now reference this.
- **Reason**: Previously `100` was spread across 7+ files as separate constants. Changing the default required updating every file and risked inconsistency.
- **Tradeoff**: Cross-package references use fully-qualified names to avoid Hilt KSP compilation issues (see PITFALLS.md).

## 2026-05-18: Hide action bar back arrow during meditation
- **Choice**: In `MeditationFragment.showRunningState()`, call `supportActionBar?.setDisplayHomeAsUpEnabled(false)`.
- **Reason**: The back arrow was buggy during meditation; only the Stop button should allow exiting.
- **Tradeoff**: No change to `showIdleState()` needed since the fragment is destroyed when meditation stops.

## 2026-05-18: System alarm volume slider in Bell Volumes dialog
- **Choice**: Added a `SeekBar` for `AudioManager.STREAM_ALARM` at the top of the Bell Volume Config dialog, with section headers for "Meditation Volume" and "Bell Dimming".
- **Reason**: Users needed to adjust the phone's alarm volume from within the app's bell configuration dialog.
- **Considered**: Link to system settings (less integrated), percentage-based slider (raw device steps are more accurate).
- **Tradeoff**: Volume label shows raw device steps (e.g. "5/7") rather than percentage; UI now uses ScrollView.

## 2026-05-19: Foreign key bellId → bells._id with DB migration 7→8
- **Choice**: Added `FOREIGN KEY (bellId) REFERENCES bells(_id)` to both `sections` and `session_bell_volumes` tables via MIGRATION_7_8, plus `@ForeignKey` annotations on Room entities.
- **Reason**: No FK constraint existed — `bellId = 0` was silently allowed, causing sections with the same bell to collapse into a single entry in the Bell Volume dialog. The FK constraint prevents this at the database level, guaranteeing referential integrity.
- **Considered**: Fixing only the UI (deriveBellVolumesFromSections deduplication) — treats symptoms, not root cause. Adding FK without migration — would fail on existing data with bellId=0.
- **Tradeoff**: Migration must seed built-in bells with hardcoded package-name URIs (`android.resource://at.priv.graf.zazentimer/raw/…`). Debug builds produce different URIs; `ensureBellsTableConsistent()` at startup repairs the mismatch. Every test that creates sections now needs a bell row in the bells table first.

## 2026-05-19: BellId resolved at write time, not startup time
- **Choice**: `SectionEditFragment.onPause()` resolves `bellId` via `dbOperations.getBellByUri()` before saving. `DemoSessionCreator.createSection()` resolves `bellId` via `getBellByUri()` before insert. Removed `s.bellId = 0` in `onItemSelected()`.
- **Reason**: Previously, `bellId` was set to `0` when the user changed a bell and was resolved only at next app startup by `MigrationHelper.resolveUnresolvedBellIds()`. Between the change and the restart, all sections with `bellId=0` appeared as a single bell in the dialog.
- **Considered**: Resolving in `onItemSelected()` callback (async race with `onPause()` save), scheduling at startup only (leaves window of broken state).
- **Tradeoff**: One extra DB query per section save; negligible overhead.

## 2026-05-19: ensureBellsTableConsistent at every startup (SUPERSEDED)
- **Choice**: (Superseded by 2026-06-07 startup health check below) `ZazenTimerActivity` now calls `MigrationHelper.ensureBellsTableConsistent()` at every app startup (not just on backup restore), before demo session creation.
- **Reason**: The bells table may be stale after backup restore, manual DB modification, or upgrade from older versions. Running it every startup ensures consistency for the FK constraint.
- **Considered**: Running only on version upgrade (misses backup restore), running only on demo creation (misses stale data in existing sessions).
- **Tradeoff**: ~few dozen ms of DB operations at startup; idempotent (INSERT OR IGNORE for built-in bells).

## 2026-05-19: 3NF Normalization — remove bell/belluri/resourceName (#199)
- **Choice**: Dropped `sections.bell`, `sections.belluri`, `session_bell_volumes.bell`, `session_bell_volumes.belluri`, `bells.resourceName`. Made `sections.rank`/`bellcount`/`bellpause` NOT NULL. Changed `session_bell_volumes` unique constraint from `(fk_session, bell, belluri)` to `(fk_session, bellId)`.
- **Reason**: These columns were duplicate representations of bell identity — the canonical source is `bells._id` via FK constraint (MIGRATION_7_8). Maintaining duplicates violates 3NF. `resourceName` is redundant with `bells.uri` (e.g., `android.resource://.../raw/bell2` encodes the resource name).
- **Considered**: Keeping them as safety net (rejected — FK constraint already enforces integrity). Gradual deprecation via code-only removal (rejected — leaves cruft in migration chain).
- **Tradeoff**: Large migration (MIGRATION_9_10 drops 5 columns across 3 tables). All callers of `section.bell`/`section.bellUri` must be updated. `deriveBellVolumesFromSections()` simplified to bellId-only grouping.

## 2026-05-19: sessions.rank for persistent session ordering (#199)
- **Choice**: Added `rank INTEGER NOT NULL DEFAULT 0` to `sessions` table. `SessionDao` queries now `ORDER BY rank, name COLLATE NOCASE`.
- **Reason**: Session drag-and-drop reordering in the UI only modified the in-memory list — after app restart, sessions reverted to alphabetical order. Sections already had `rank` for this purpose; sessions needed parity.
- **Considered**: Storing order in SharedPreferences (fragile, breaks when sessions are added/deleted). Using natural alphabetical order only (already in place but users wanted custom order).
- **Tradeoff**: One extra column in sessions table. `DbOperations.insertSession()` now assigns `rank = MAX(rank) + 1`. Drag-and-drop in MainFragment needs to persist via `switchSessionPositions()`.

## 2026-05-19: BellPlayer accepts getBellById lambda
- **Choice**: `BellPlayer` constructor takes `getBellById: suspend (Int) -> BellEntity?` lambda instead of resolving bells internally.
- **Reason**: `BellCollection.getBellForSection()` accessed `section.bellUri` which no longer exists. Bell resolution now requires a DB query (`bellId → BellEntity.uri → BellCollection.getBellByUri`). BellPlayer is a service-layer class without Hilt injection; the lambda keeps it decoupled from DbOperations while enabling async resolution.
- **Considered**: Injecting DbOperations directly into BellPlayer (heavier DI, changes MeditationService construction). Passing Bell objects directly to playBells() (changes API, caller must pre-resolve for all sections).
- **Tradeoff**: BellPlayer callers (MeditationService) must provide a lambda wrapping `meditationRepository.getBellById()`.

## 2026-05-19: insertSection defaults bellId=0 to demo bell
- **Choice**: `DbOperations.insertSection()` resolves `bellId=0` to the demo bell via `BellCollection.getDemoBell() → getBellByUri() → bellId` before Room insert.
- **Reason**: New sections are created with `bellId=0` (no bell selected yet). The FK constraint `bellId → bells._id` rejects `bellId=0` at the SQLite level. Defaulting to the demo bell prevents the FK violation while keeping the user experience (bell can be changed immediately after creation in SectionEditFragment).
- **Considered**: Making `bellId` nullable in the FK (allows 0 as "no bell", but 0 is not NULL — FK constraint still fires). Requiring the UI to always set bellId before insert (SectionEditFragment doesn't set it until onPause).
- **Tradeoff**: Slightly magic behavior — user creates a section, it defaults to demo bell. The SectionEditFragment immediately shows the selected bell in the spinner, so the user sees the correct bell.

## 2026-05-20: Session rank persistence via onPause() recomputation (#199)
- **Choice**: Session drag-and-drop order persists via `MainFragment.onPause()` which recomputes ranks from in-memory list position and calls `dbOperations.updateSession()` for each session.
- **Reason**: Following the established pattern in `SessionEditFragment.onPause()` for sections. Simpler than calling `switchSessionPositions()` per `onMove` callback, and handles all drag scenarios correctly.
- **Considered**: Calling `switchSessionPositions()` in the `onMove` callback (complex — coroutine ordering issues with rapid successive drag events, and only handles adjacent swaps).
- **Tradeoff**: Writes all sessions to DB on every pause, even if no reorder occurred. Negligible for typical session counts.

## 2026-05-24: Deno runtime for Prisma schema generation
- **Choice**: Use Deno with `runtime = "deno"` in Prisma Client generator config, import map alias `"prismaclient": "./generatedprismaclient/client.ts"`. No Node.js/npm involved.
- **Reason**: The developer machine has Deno installed (not Node.js). `npm:prisma@^6.19.3` works natively via Deno's npm compatibility layer. Avoids maintaining a separate Node.js toolchain.
- **Considered**: Node.js + npx (requires Node install), Python SQLAlchemy (different ecosystem, no Prisma).
- **Tradeoff**: Prisma's Deno support is newer; some edge cases may differ from Node.js behavior.

## 2026-05-24: Confidence as Int with raw SQL CHECK constraint (not enum)
- **Choice**: `confidence` is `Int` with raw SQL `CHECK(confidence BETWEEN 1 AND 5)` in the `CREATE TABLE votes` statement. No Prisma `@validation` or enum type.
- **Reason**: SQLite sorts enums alphabetically by their string value, not by declaration order. `CONFIDENCE_LOW` (1) would sort before `CONFIDENCE_HIGH` (5) lexicographically, making ordered queries meaningless. A numeric Int with CHECK preserves natural ordering.
- **Considered**: Prisma enum (alphabetical sort issue), `@validation` annotation (only enforced by Prisma Client, not at SQLite level).
- **Tradeoff**: Raw SQL in migration file must be hand-maintained; Prisma's `prisma format` doesn't validate CHECK expressions.

## 2026-05-24: CHECK constraint in init migration CREATE TABLE, not separate migration
- **Choice**: Placed `CHECK(confidence BETWEEN 1 AND 5)` directly in the init migration's `CREATE TABLE votes` statement. Not as a separate migration with `ALTER TABLE ADD CHECK`.
- **Reason**: SQLite (via `prisma db push` or migration engine) does NOT support `ALTER TABLE ADD CHECK`. The CHECK must be present in the original `CREATE TABLE` or the migration fails with `SQLite does not support adding CHECK constraints to existing tables`.
- **Considered**: Separate migration (would fail), Prisma-level validation only (not enforced at DB level).
- **Tradeoff**: If the schema is re-generated from scratch, the raw SQL in the init migration must be preserved manually.

## 2026-05-24: whisper_response NOT unique — regional variants share same response
- **Choice**: Removed `@unique` from `whisper_response` in `locales` model. Multiple regions sharing the same language (e.g., `pt`, `pt-BR`, `pt-PT` → "portuguese"; `sr`, `sr-Latn` → "serbian"; `zh`, `zh-TW` → "chinese") legitimately have identical Whisper responses.
- **Reason**: Whisper returns one canonical name per language regardless of region. A unique constraint would prevent storing multiple BCP 47 variants that map to the same Whisper language.
- **Considered**: Keeping `@unique` with a separate `whisper_region` discriminator (over-engineered, Whisper doesn't distinguish regions).
- **Tradeoff**: Application code must handle the case where multiple locales resolve to the same Whisper transcription.

## 2026-05-24: Python pycountry for language seed generation (not openai-whisper)
- **Choice**: `scripts/generate_languages_seed.py` uses `pycountry` for ISO 639-3 codes and English names. A static `whisper_languages.json` (2 KB, extracted from Whisper `tokenizer.py`) provides the Whisper code → name mapping.
- **Reason**: `openai-whisper` has a massive dependency tree (Torch, CUDA, NumPy) — installing it just to extract the built-in language map is wasteful. `pycountry` is lightweight, pure Python, and provides authoritative ISO 639-3 lookups.
- **Considered**: `openai-whisper` pip install (106+ MB, pulls CUDA/Torch), manual ISO table (not authoritative), `langcodes` library (Pandas dependency).
- **Tradeoff**: The Whisper language map is static — if Whisper adds languages in a future release, the JSON must be manually updated.

## 2026-05-24: Static whisper_languages.json instead of openai-whisper import
- **Choice**: Extracted the 100-entry Whisper language map from `openai/whisper/tokenizer.py` into `prisma/translations/whisper_languages.json`. The Python seed script reads this JSON alongside the generated locale data.
- **Reason**: Avoids the 106+ MB `openai-whisper` dependency (Torch, CUDA, NumPy, tqdm, more). The language map is stable — Whisper hasn't changed its 100-language set since the Multilingual model release (late 2022).
- **Considered**: Dynamic import via `import whisper` then `whisper.tokenizer.LANGUAGES` (106 MB install), hardcoding in Python (maintenance burden).
- **Tradeoff**: If Whisper adds languages, the JSON must be manually regenerated. The Python seed script uses `from whisper_languages import LANGUAGES` — if the JSON format changes, the script breaks.

## 2026-05-24: ISO 639-3 is NOT unique in locales table
- **Choice**: `iso_639_3` column in `locales` has no `@unique` constraint. Portuguese (`por`: pt, pt-BR, pt-PT), Serbian (`srp`: sr, sr-Latn), and Chinese (`zho`: zh, zh-TW) each have multiple regional variants sharing the same ISO 639-3 code.
- **Reason**: ISO 639-3 identifies a language macrolanguage, not a specific region. A locale like `pt-BR` and `pt-PT` are both Portuguese (`por`). Enforcing uniqueness would prevent storing all regional variants.
- **Considered**: Unique on (iso_639_3, script) pair (over-engineered for current needs), splitting into separate language/region models (3NF normalization, too complex).
- **Tradeoff**: Queries grouping by ISO 639-3 must handle 1:N cardinality. The 3 affected codes are known and documented.

## 2026-05-24: Regex fallback for strings.xml parsing (no DOMParser in Deno 2.7.14)
- **Choice**: The Deno seed script parses `strings.xml` with a regex (`<string[^>]*name="([^"]*)"[^>]*>([^<]*)</string>`) instead of an XML parser.
- **Reason**: Deno 2.7.14 has no native `DOMParser` (requires `npm:xmldom` as polyfill). All 174 entries in the English `strings.xml` are single-line and well-formed — regex is sufficient and avoids an npm dependency.
- **Considered**: `npm:xmldom` polyfill (additional import, maintenance), `npm:fast-xml-parser` (heavier, configuration needed).
- **Tradeoff**: Regex breaks on multi-line string definitions or XML comments within the strings block. Must be verified if `strings.xml` format changes.

## 2026-05-25: Include null-vote strings in translate skip set (#219)
- **Choice**: Added `getNullExistingVotes()` to query `translation: ""` entries separately. `runOne()` now merges `nullVotes` into the skip set alongside `existing` (non-null) and `settled` (3+ model consensus).
- **Reason**: `getExistingVotes()` filters with `NOT_EMPTY`, so empty-string votes from `null` returns were never included in the skip set. On restart, every (model, locale) re-requested all strings where the model previously returned `null`, wasting 10-30 minutes per run.
- **Considered**: Changing `getExistingVotes()` to include empty strings (would break frontend coverage counts).
- **Tradeoff**: Separate query function keeps existing semantics intact.

## 2026-05-25: Pseudo-ISO timestamp format in translate logs
- **Choice**: Changed `ts()` from ISO format (`2026-05-25T12:38:40.123Z`) to filesystem-safe pseudo-ISO (`2026-05-25_12-38-40`). Console output now also includes the timestamp prefix.
- **Reason**: The ISO format with colons is problematic for filenames and log parsers. User wanted timestamps visible in console output too.
- **Tradeoff**: One-time format change, parsers must handle both formats during transition.

## 2026-05-25: Proficiency threshold skip bugfix (#222)
- **Choice**: Renamed `_minProficiency` to `minProficiency` and added the actual check: `if (proficiency < minProficiency) return`. Added `getProficiencyLevel()` helper in `db.ts`.
- **Reason**: The underscore-prefixed parameter was unused — models with proficiency below threshold were never skipped despite the `--min-proficiency` CLI flag.
- **Tradeoff**: Additional DB query for proficiency level on every (model, locale) run.

## 2026-05-25: WAL checkpoint after each translate batch (#229)
- **Choice**: Added `PRAGMA wal_checkpoint(TRUNCATE)` after each successful translate store cycle in `dispatchTranslate()`.
- **Reason**: The voting backend's PrismaClient blocked on `busy_timeout=5000` while translate wrote large batches, causing ~40s timeouts on `/strings/:sid/comparison`.
- **Considered**: Setting checkpoint at voting backend startup only (didn't help long-running translates).
- **Tradeoff**: ~2ms overhead per (model, locale) batch.

## 2026-05-25: Fresh PrismaClient per query for blocking endpoints
- **Choice**: `prisma.ts` changed from singleton to `new PrismaClient()` on every call. `getLanguageById()` and `getMasterStringById()` refresh the client on each invocation.
- **Reason**: Prisma v6's library engine (`.so.node` native addon) has intermittent internal blocking with a singleton client. A fresh client per query avoids accumulated engine state.
- **Considered**: PRAGMA-based workarounds (busy_timeout, WAL mode, CHECKPOINT) — none resolved the sporadic 15s delays.
- **Tradeoff**: ~6ms per-query overhead for engine startup.

## 2026-05-26: getEvaluation() now includes modelDetails[] (#230)
- **Choice**: `getEvaluation()` returns `modelDetails: {name, level}[]` per entry alongside the existing `modelNames: string`.
- **Reason**: The `/languages/:lid` page needed per-translation tooltips showing which models voted and at what proficiency level.
- **Tradeoff**: Slightly larger response; backwards compatible (modelNames remains).

## 2026-05-26: Voting backend /models page replaced nav tiles with table (#230)
- **Choice**: Replaced the model navigation button bar with a model overview table (Model | Languages | Avg. Proficiency | Total Votes). Clicking a model name loads the existing per-language proficiency table via htmx.
- **Reason**: User wanted a table instead of tiles, and per-model summary statistics (avg proficiency, non-empty vote count).
- **Tradeoff**: Additional `getModelsWithStats()` DB function; two-level navigation (overview → detail) instead of immediate tile selection.

## 2026-05-26: Explicit FK columns for language_proficiencies (#231)
- **Choice**: Replaced implicit M:N junction tables with explicit `modelId`/`languageId` FK columns on `language_proficiencies`, plus `@@unique([modelId, languageId])`.
- **Reason**: The implicit M:N pattern (`llm_models[]`, `languages[]`) was semantically wrong — a proficiency assessment belongs to exactly one (model, language) pair. Query patterns with `some: { id }` were awkward.
- **Considered**: Keeping implicit M:N (works but misleading schema).
- **Tradeoff**: Migration required deduplication (2 duplicates found) and table recreate; 6 query functions updated.

## 2026-05-26: 60min session timeout + 2 stall retries for translate (#232)
- **Choice**: Added `sendMessageWithTimeout()` using `AbortController`, 60-minute inactivity timeout, and 2 stall retries per (model, locale) in both `dispatchProficiency()` and `dispatchTranslate()`. Separate from the existing `MAX_RETRIES=3` verify-error retry mechanism. Added `scripts/analyze_translate_logs.sh`.
- **Reason**: opencode-go provider sessions sometimes hang indefinitely (observed: kimi-k2.6 took 39 min for Irish). The stall layer closes the hung session and creates a fresh one. User prefers generous timeout (60 min) to avoid false positives.
- **Considered**: Per-provider timeout (rejected — too complex), single timeout kills entire run (rejected — per-session granularity better).
- **Tradeoff**: 60-min timeout means the stall-retry will rarely fire; its primary value is as a safety net.

## 2026-05-24: Pre-push hook as symlink to scripts/git-hooks/
- **Choice**: `.git/hooks/pre-push` → `../../scripts/git-hooks/pre-push` (symlink). Not a copy.
- **Reason**: Keeps the pre-push hook always in sync with the template. Any edit to `scripts/git-hooks/pre-push` immediately applies to the live hook. The `--no-daemon` flag was removed from the hook to avoid unnecessary daemon-spawning behavior.
- **Considered**: Copy (drifts from template), inline in `.git/hooks/` (not version-controlled).
- **Tradeoff**: Requires symlink creation on fresh clones. Documented in ONBOARDING.md.

## 2026-05-24: PrismaClient per-request pattern with Deno.serve (#202)
- **Choice**: Create a fresh PrismaClient per request inside a serialized promise queue (`withPrisma()`). Never cache a module-level PrismaClient when using `Deno.serve`.
- **Reason**: PrismaClient created at module level loses engine connectivity once `Deno.serve` enters its event loop — subsequent requests hang indefinitely. Concurrent PrismaClient initialization causes `Unsupported scheme "node"` errors. Fresh client per request + serial queue avoids both issues.
- **Considered**: Module-level singleton (hangs), lazy singleton with cached client (still hangs on second request), concurrent clients (node:fs conflict).
- **Tradeoff**: Each request pays ~200ms for Prisma Engine process spawn + DB connection + disconnect. Acceptable for a voting backend with ~1 req/s load.

## 2026-05-24: Per-request PrismaClient with serialized queue (#202)
- **Choice**: Serialize PrismaClient creation and destruction via a promise chain (`_queue = _queue.then(...).catch(() => {})`). Only one PrismaClient exists at any time.
- **Reason**: Deno's npm compatibility layer has a race condition when multiple PrismaEngine binaries are spawned simultaneously — the runtime library (`library.mjs`) tries to import `node:fs` which Deno doesn't support in concurrent contexts.
- **Considered**: Mutex/lock (more complex), single client with connection pooling (Deno doesn't support it properly).
- **Tradeoff**: Sequential processing of concurrent requests — acceptable for this backend.

## 2026-05-24: Opencode HTTP API for translation orchestration (#202)
- **Choice**: The orchestrator (`prisma/translate.ts`) uses opencode's HTTP API (`POST /session`, `POST /session/{id}/message`, `DELETE /session/{id}`) instead of `opencode run --model X --continue`.
- **Reason**: opencode server läuft bereits (opencode serve). HTTP API vermeidet Prozess-Overhead pro Aufruf (1230× Deno.Process fork wäre zu langsam). Strukturierte Trennung von System-Prompt und Input-Daten via `system`-Feld.
- **Considered**: `opencode run --model X --continue` (Prozess-Overhead, Text-Parsing nötig), `Deno.Command` (blocking, parallel schwer handhabbar).
- **Tradeoff**: Orchestrator muss opencode-Server-URL kennen (konfigurierbar via Umgebungsvariable).

## 2026-05-24: Deno/TypeScript für Orchestrator, nicht Python (#202)
- **Choice**: Der Orchestrator wird in Deno/TypeScript geschrieben (`prisma/translate.ts`), nicht in Python.
- **Reason**: Direkter Import von `prismaclient` möglich (kein exec/umweg), konsistente Toolchain mit dem restlichen Prisma-Ökosystem, shared `prisma/lib/`-Module.
- **Considered**: Python mit `datasource`-Wrapper (extra Abhängigkeit, `prisma-client-py`), Shell-Scripting (zu komplex).
- **Tradeoff**: Kein Zugriff auf `pycountry` für ISO 639-3 Lookups — wird aber für den Orchestrator nicht benötigt.

## 2026-05-24: One opencode session per (model, locale) pair (#202)
- **Choice**: Jede (model, locale)-Kombination bekommt eine eigene opencode-Session. Keine Sessions, die mehrere Sprachen oder Modelle mischen.
- **Reason**: Der Agent sieht nur eine Sprache → null Verwechslungsgefahr. Kontextfenster bleibt minimal (~2k Tokens). Retry betrifft nur genau dieses (model, locale).
- **Considered**: Alle Locales in einer Session (Kontext wächst auf 250k+ Tokens, Agent vergisst Zielsprache, Retry in Sprache 42 riskiert 41 vorherige Ergebnisse).
- **Tradeoff**: Session-Churn — 1230 Sessions für einen Voll-Durchlauf. HTTP API verkraftet das problemlos.

## 2026-05-24: 10-minute timeout for --all runs (#202)
- **Choice**: `--all` runs haben ein hartes Timeout von 10 Minuten. `translate.ts` prüft vor jeder (model, locale)-Iteration `Date.now() - START_TIME >= 600_000`. Bei Überschreitung: `Deno.exit(0)` (sauberes Ende).
- **Reason**: LLM-API-Aufrufe dauern 3-20s pro Session → 10 Minuten reichen für ~30-200 Sessions. Der Rest wird beim nächsten Lauf nachgeholt (Idempotenz via `getExistingVotes`).
- **Considered**: Kein Timeout (Skript läuft Stunden), Timeout + Fehlercode (wäre kein Fehler, nur Abbruch).
- **Tradeoff**: Unvollständiger Durchlauf ist der Normalfall — muss dem Operator klar kommuniziert werden.

## 2026-05-24: null erlaubt im Output-JSON (#202)
- **Choice**: Das Output-JSON des LLM-Agents darf `"translation": null` enthalten. Der Agent signalisiert damit „diesen String kenne ich nicht". Solche Einträge werden nicht in der DB gespeichert.
- **Reason**: Bei ~154 Strings pro Locale kann ein Modell einzelne Strings nicht kennen (z.B. tiefe Meditationsterminologie in einer schwachen Sprache). Erzwungene Halluzination wäre schlimmer als `null`.
- **Considered**: Fehlende Keys im Output = unbehandelt (nicht von `translation: null` unterscheidbar), leere Strings (`""`) als Platzhalter (nicht von echten leeren Übersetzungen unterscheidbar).
- **Tradeoff**: Zusätzlicher Verify-Schritt. `null`-Einträge müssen beim Voting ignoriert werden.

## 2026-05-24: M:N language_proficiencies als Junction Table (#202)
- **Choice**: `language_proficiencies` ist eine implizite M:N-Junction zwischen `llm_models` und `languages` mit `level` als Payload. Ein Model kann mehrere Level-Einträge haben (pro Sprache), eine Sprache kann von mehreren Modellen bewertet werden.
- **Reason**: Flexibilität für zukünftige Anwendungsfälle (z.B. „Model hat Level 5 in Deutsch, aber nur Level 3 in österreichischem Deutsch"). `@@unique` auf der Junction wäre zu restriktiv gewesen.
- **Considered**: Explizite 1:N mit `@@unique([modelId, languageId])` (restriktiver, keine doppelten Einträge pro Model-Language-Paar).
- **Tradeoff**: Ohne `@@unique` können theoretisch doppelte (model, language, level)-Einträge entstehen. Der Orchestrator muss deduplizieren.

## 2026-05-22: Prisma-managed translation voting database (#202)
- **Choice**: Created a second Prisma schema at `prisma/translations/schema.prisma` with 4 models (locales, strings, translations, votes) to store multi-LLM translation candidates and voting results. Added `prismaValidateTranslationsSchema` Gradle task.
- **Reason**: Need a structured, version-controlled data store for the multi-model translation pipeline where 4 LLMs (Claude, Gemini, GPT, DeepSeek) each produce candidate translations that are then compared via a voting mechanism.
- **Considered**: JSON-only storage (no queryability), keeping in-memory only (no persistence between sessions).
- **Tradeoff**: Two Prisma schemas now coexist under `prisma/` — device DB at `prisma/desired/` + `prisma/current/`, translation DB at `prisma/translations/`. The translation DB is not auto-pulled from a device; schema evolves by hand.

## 2026-06-07: Bidirectional resilient backup restore with 1:1 bell sync + startup health check (#241)
- **Choice**: Three parts: (1) delete stale `-wal`/`-shm` files after database overwrite in restore; (2) add `DbOperations.sanitizeBellUris()` via Room DAOs (no raw SQL); (3) run `sanitizeBellUris()` at every app startup in `ZazenTimerActivity.onCreate()` lifecycleScope, superseding `ensureBellsTableConsistent()`.
- **Reason**: WAL/SHM stale files caused SQLite to corrupt the restored database on reopen. Debug↔production package name mismatches in bell URIs caused FK constraint violations when editing sessions after restore. Running at startup heals the app after direct file operations, app updates with changed bell sets, and any database drift.
- **Considered**: Fixing only via defensive code in `fillDataFromViews()` (masks root cause), raw SQL URI update in BackupManager (bypasses Room), running only after restore (misses edge cases).
- **Tradeoff**: `sanitizeBellUris()` is ~90 lines and requires `@Suppress("CyclomaticComplexMethod", "LongMethod")`; `onCreate()` also needs `@Suppress("LongMethod")`.
- **Deleted**: `MigrationHelper.kt` and its `seedBuiltinBells()` — fully redundant with `sanitizeBellUris()`.

## 2026-05-20: Emulator scripts as sourceable libraries (#200)
- **Choice**: Restructured `start-emulator.sh` and `stop-emulator.sh` to be both standalone executables AND sourceable libraries with a `[[ "${BASH_SOURCE[0]}" == "${0}" ]]` guard. `run-instrumentation.sh` and `create-emulator-snapshots.sh` source them instead of duplicating functions.
- **Reason**: 6 functions were duplicated 3-4x across scripts (`resolve_avd`, `wait_for_boot`, `configure_system`, `kill_emulator`, `kill_stale`, `setup_device`). Single source of truth eliminates drift.
- **Considered**: Extracting into a separate `lib/emulator-helpers.sh` file (additional file, no standalone mode).
- **Tradeoff**: Library functions must be generic (parameterized, not relying on global variables). Standalone mode duplicates a few lines of arg parsing.

## 2026-05-20: `-noaudio` passed explicitly by callers (#200)
- **Choice**: Removed auto-detection of `-noaudio` from `emulator_launch()` (which checked `$DISPLAY`). Callers now pass `-noaudio` explicitly based on their own display state (Xvfb vs real X11).
- **Reason**: After `emulator_x11_prepare` starts Xvfb, `$DISPLAY` is set to `:99`, causing `emulator_launch` to skip `-noaudio` — resulting in pulseaudio connection errors on headless VPS. The auto-detection was coupled to display state, but audio is independent.
- **Considered**: Adding a separate `EMULATOR_X11_IS_XVFB` flag checked by emulator_launch (adds coupling). Passing a boolean parameter (less flexible).
- **Tradeoff**: Every caller must remember to pass `-noaudio` when appropriate.

## 2026-05-20: Emulator logfile as mandatory parameter (#200)
- **Choice**: `emulator_launch(avd, serial, logfile, ...flags)` now requires a logfile parameter; emulator stdout/stderr redirected via `>> "$logfile" 2>&1 &`.
- **Reason**: Previously, callers tried to redirect emulator output via `$(emulator_launch ... 2>>logfile)`, but the background emulator process inherits the subshell's file descriptors only during the function call — its subsequent output was lost. Making logfile mandatory and redirecting inside the function ensures all emulator output is captured.
- **Tradeoff**: Every caller must provide a logfile path.

## 2026-05-24: Voting API + Frontend als Hono JSX + htmx (#212)
- **Choice**: Rewrote `voting_api.ts` as `voting_api.tsx` mit Hono JSX (SSR) + htmx + Pico CSS. Lazy PrismaClient Singleton mit WAL Mode in `lib/prisma.ts`.
- **Reason**: `voting_api.ts` wurde upstream gelöscht (war Teil von #211). Neubau als `.tsx` für JSX-Support. htmx erlaubt interaktive Filter/Suche ohne eigenes JavaScript. Pico CSS ist classless und macht rohes HTML sofort presentabel.
- **Considered**: SPA mit React/Vue (zu schwer, Build-Schritt nötig), reines Vanilla JS (htmx spart ~100 Zeilen fetch/event-code).
- **Tradeoff**: htmx-Abwesenheit = Full Page Reloads bei Filter-Änderungen. Pico CSS CDN = keine Offline-Funktionalität.
- **DB-Pattern**: Lazy Singleton (`lib/prisma.ts`) statt per-request `withPrisma()` — SQLite profitiert von Single-Connection. WAL-Mode für gleichzeitiges Lesen/Schreiben. PRAGMAs via `$queryRawUnsafe` (nicht `$executeRawUnsafe`, da PRAGMA Result-Zeilen zurückgibt).

## 2026-05-20: Hostname-based test matrix (#200)
- **Choice**: `run-instrumentation.sh` selects API list and display strategy based on `hostname -s`:
  - `claw`: Xvfb forced, APIs from `zazentimer.test.apis.claw` (currently API 34 only)
  - `think`: uses X11 if available, or `--force-xvfb` flag, otherwise skips instrumented
  - other: skips instrumented tests entirely (unit tests only)
- **Reason**: VPS (claw) and local dev machine have different AVD sets and display capabilities. Avoids running instrumented tests in inappropriate environments.
- **Considered**: Hardcoding in script (less flexible), environment variable (works but not repo-tracked).
- **Tradeoff**: Hostname is hardcoded in `case` statement; adding a new host requires editing the script.
- **SUPERSEDED 2026-05-24**: claw no longer uses `zazentimer.test.apis.claw` (property removed). All hosts use `zazentimer.test.apis`. Missing AVDs are silently skipped instead of failing. Gradle exit code is no longer fatal — per-API pass/fail is tracked independently, and overall exit code reflects test results only.

## 2026-05-27: Verbose translate logs — stringCount/emptyCount/skippedMasterString
- **Choice**: Replaced `stored`/`skipped` counters with `stringCount` (non-empty votes), `emptyCount` (`""` votes including null-LLM-responses), and `skippedMasterString` (no master_string match, only logged when >0).
- **Reason**: User wanted to distinguish real translation votes from empty-string sentinel entries in the log. Previously both were lumped under `stored`.
- **Tradeoff**: Additional counters, but no behavioral change — same DB writes.

## 2026-05-27: Proficiency start log line
- **Choice**: Added `requesting proficiency for ${modelName} ${langEnglishName} (${langBcp47})` before each proficiency assessment call.
- **Reason**: Provide visibility into which (model, locale) pair is being assessed, matching the existing "submitting translation request" log.
- **Tradeoff**: One extra log line per (model, locale) with proficiency.

## 2026-05-27: Always-on verbose language-start stats
- **Choice**: The `runOne()` language stat log fires on every translate attempt (not only when `settled.size > 0`), showing settled/existing/null/missing counts with the active provider label.
- **Reason**: User wanted to see full context for every (model, locale) run, including "0 out of N strings are settled" cases.
- **Tradeoff**: Redundant log line when settled=0 (but informative).

## 2026-05-27: Verify error enrichment with raw output snippet
- **Choice**: `asObject()` now preserves the `JSON.parse` error message. `verifyTranslationFile()` and `verifyProficiencyFile()` wrap their body in try/catch and append the raw output (first 200 chars) to every `VerifyError`.
- **Reason**: Models on retry got only "Response is not valid JSON" with no clue what they wrote. Since skills previously said "no access to files", they couldn't read their own output.
- **Tradeoff**: Error messages are longer; raw snippet may be truncated at 200 chars.

## 2026-05-27: Skill "no access to files" relaxed for output files
- **Choice**: Translate Skill now says "You may read `translate-input.json` and `translate-output.json`." Proficiency Skill now says "You may read `proficiency-output.json`." Both skills: "You have no access to other files, tools, scripts, or APIs."
- **Reason**: Models couldn't self-diagnose on retry because the skill forbade file reading. With the output file readable, the model can inspect what it wrote.
- **Considered**: Keeping "no access" and embedding full output in error message (belt-and-suspenders approach — both are now implemented).
- **Tradeoff**: Slightly relaxed security — models can read two specific files in the project dir.

## 2026-05-27: System prompt always sent on retry
- **Choice**: `dispatchTranslate()` and `dispatchProficiency()` now send `system: SKILL_*` on every retry (not only `retry === 0`). Retry user message also mentions "You may read 'INPUT' and 'OUTPUT' to inspect."
- **Reason**: On retry, the model had no system prompt (previous one was lost as conversation context). Without it, the model didn't know the locale, input keys, or output format.
- **Tradeoff**: Extra tokens on retry (the SKILL is ~600 chars).

## 2026-05-27: PROVIDER_RANKING → MODEL_PROVIDERS per-model mapping
- **Choice**: Replaced the global `PROVIDER_RANKING` array with `MODEL_PROVIDERS`, a per-model `Record<string, string | string[]>` that maps each model to its preferred provider(s) in priority order. `buildFallbackChain()` uses this instead of the global ranking. `fetchAvailableModels()` no longer stores rank or sorts by it.
- **Reason**: Some models are only available from specific providers; a global ranking didn't capture that. For example, `mistral-large` is only on `opencode-go`, but the old system would try other providers first (which don't have it) before falling back. `ModelEntry` no longer needs a `rank` field.
- **Considered**: Keeping both systems (messy), making MODEL_PROVIDERS override PROVIDER_RANKING (unnecessary indirection).
- **Tradeoff**: Changes to `MODEL_PROVIDERS` require code edits (not env config). Models not in `MODEL_PROVIDERS` cannot be used by the pipeline.

## 2026-05-27: llmmodels_seed.json → llmmodels_master.json + deleteMany in seed
- **Choice**: Renamed `llmmodels_seed.json` to `llmmodels_master.json` (single source of truth). Added `mistral-large`. The seed now runs `deleteMany` for LLM models not in the master list (cascade-deletes votes + proficiencies).
- **Reason**: Previously, models removed from the seed file were never cleaned up from the DB. The seed already did this for languages and master_strings; models should be consistent.
- **Tradeoff**: Running the seed (or any pipeline run via `--all`) may cascade-delete votes for retired models.

## 2026-05-27: Model-DB validation at pipeline start
- **Choice**: `--all` validates that every `MODEL_PROVIDERS` entry exists in DB (error + exit(1)), warns for DB models not in `MODEL_PROVIDERS`. `--model --all` validates the specific model exists in `MODEL_PROVIDERS` and DB. Errors suggest: "Ensure it's in llmmodels_master.json and run deno task prismatranslationsseed."
- **Reason**: Prevent silent pipeline failures when a model is defined in config but hasn't been seeded yet, and alert when stale models linger in DB.
- **Tradeoff**: Extra startup validation (~ms per model).

## 2026-05-27: Remove 35 unused English string resources (#223)
- **Choice**: Deleted 35 `<string>` entries from `values/strings.xml` that had zero references in `.kt`/`.xml` source files (no `R.string.` / `@string/`).
- **Reason**: These were dead strings from removed features (ACRA crash reporting, server news, SD card status, bell dimming, legacy bottom navigation). Translation files untouched.
- **Tradeoff**: Language-pair files (`values-*/strings.xml`) still contain these keys — they become orphans until the translation pipeline is rerun against the cleaned source.

## 2026-05-27: seed.ts deletes obsolete master_strings for idempotency (#224)
- **Choice**: After upserting master_strings from `values/strings.xml`, added `deleteMany({ text: { notIn: currentTexts } })`. Orphaned votes cascade-deleted via FK constraint.
- **Reason**: Previously, removing strings from the XML left orphan master_strings (and their votes) in the DB permanently. The seed was not truly idempotent.
- **Tradeoff**: Running seed may cascade-delete votes if strings were removed since last run.

## 2026-05-27: Inline language seeding into seed.ts, remove Python script (#225)
- **Choice**: `seed.ts` now scans `app/src/main/res/` for `values-*` dirs directly. `iso639.json` (committed, 8K entries, generated once from pycountry) replaces runtime Python dependency. Deleted `generate_languages_seed.py` and `languages_seed.json`.
- **Reason**: Filesystem is the single source of truth for available locales. Seed now auto-detects new dirs and deletes orphan languages. Eliminates intermediate Python+JSON build step.
- **Tradeoff**: Adding a new locale requires adding the `values-xx` directory and potentially updating `iso639.json` if the language code is not in pycountry (unlikely for ISO 639-1 codes).

## 2026-05-27: Remove custom types from db.ts, use Prisma-generated types (#226)
- **Choice**: Replaced `PLanguage`, `PModel`, `PMasterString` with `import type { languages, master_strings, llm_models } from "prismaclient"`.
- **Reason**: Hand-rolled types duplicated Prisma schema and were always out of date. Prisma-generated types are always correct after `prisma generate`.
- **Tradeoff**: Type change may cause false-positive lint errors if imported types differ slightly from custom subset.

## 2026-05-27: Remove direct Prisma dependency from voting_api.tsx (#226)
- **Choice**: Moved all 13 `prisma.xxx` calls from `voting_api.tsx` into 8 new functions in `lib/db.ts`. Removed `import { getPrisma }` and `const prisma = await getPrisma()` from voting_api.tsx.
- **Reason**: Clean layering — route handlers should never touch Prisma directly. All DB access via `db.ts` makes the interface explicit and testable.
- **Tradeoff**: Slightly more function definitions in db.ts; some are thin wrappers (`getModelById`, `getLanguageById`).

## 2026-05-27: Performance: groupBy for string vote counts, filter proficiencies (#227)
- **Choice**: `getStrings()` uses `prisma.votes.groupBy` (single query) instead of N+1 `votes.count`. `getStringSettlement()` filters `language_proficiencies` to only models+languages present in the string's votes.
- **Reason**: strings page did 1+N queries (130+ queries). Settlement page loaded all ~1230 proficiencies for a single string query.
- **Tradeoff**: groupBy may not use indexes ideally on SQLite; acceptable for current data volume.

## 2026-05-27: Fix stale bell list in Adjust Bell Volumes dialog (#234)
- **Choice**: `showBellVolumeDialog()` reads sections fresh from DB via `dbOperations.readSections(s.id)` inside a coroutine instead of using the cached `this.sections` field. Removed `deriveBellVolumesFromSections()`.
- **Reason**: Deleting sections or swapping bells within a section left `this.sections` stale. When the dialog opened, it included bells from deleted sections.
- **Tradeoff**: Dialog open now requires a coroutine launch (non-blocking, already async pattern).

## 2026-06-02: Backup filter — only `bell_*` files (#237)
- **Choice**: `BackupManager.addFilesDirToZip()` now includes only files starting with `bell_` from `filesDir`. Previously it included everything except `InstantRun`.
- **Reason**: The old filter swept stale artifacts (`zentimer`, `profileInstalled`, `profileinstaller_*`) into every backup. Custom bell audio files (the only meaningful data in `filesDir` besides the DB) all start with `bell_`.
- **Considered**: Excluding specific known-stale filenames (brittle, misses future artifacts).
- **Tradeoff**: Any non-`bell_*` file in `filesDir` is silently excluded. Currently no such files carry useful data.

## 2026-06-02: `BellCollection` refresh after backup restore (#237)
- **Choice**: Added `BellCollection.initialize(requireContext())` in `SettingsFragment.doRestore()` after successful restore, alongside the existing `seedBuiltinBells()` call.
- **Reason**: `BellCollection` is a singleton that caches available bells in memory. Restoring a backup writes audio files to `filesDir` but doesn't refresh `BellCollection`, so the Section Edit bell spinner (which reads from `BellCollection`) is stale. Manage Bells always worked because it reads directly from the DB.
- **Tradeoff**: Adds ~ms of filesystem scanning per restore; idempotent.

## 2026-06-02: selectBell URI fallback for cross-device restore (#237)
- **Choice**: `SectionEditFragment.selectBell()` now falls back to filename-suffix matching when exact URI comparison fails.
- **Reason**: The database stores absolute file paths from the backup device (e.g., `file:///data/.../OldDevice/files/...`). `BellCollection` builds URIs dynamically from the current device's `filesDir`. Exact URI matching fails across devices. Comparing only the filename part (`bell_*`) works regardless of `filesDir` path.
- **Tradeoff**: If two custom bells happen to have files ending in the same suffix, the wrong bell could be selected. Currently impossible since `bell_` prefix ensures uniqueness.

## 2026-06-02: `openOutputStream("wt")` to prevent ZIP tail corruption (#237)

## 2026-06-02: Close #202 — Python scripts obsolete
- **Choice**: Closed issue #202 without implementing the Python scripts (`translation_votes.py`, `dispatch_translations.py`).
- **Reason**: The Deno/TypeScript voting pipeline (`prisma/translations/`, `voting_api.tsx`, `export.ts`) was built instead and fully replaces the originally-planned Python approach. No Python scripts needed.
- **Tradeoff**: None — the existing Deno pipeline is superior (type-safe, same toolchain as Prisma, no Python dependency).

## 2026-06-02: Close #234 — already implemented
- **Choice**: Closed issue #234 as already fixed in commit history (2026-05-27).
- **Reason**: `showBellVolumeDialog()` reads sections fresh from DB. `deriveBellVolumesFromSections()` was removed. Verified against source code.
- **Tradeoff**: None — fix is confirmed working.

## 2026-06-02: Update #64 — Play Store checklist finalized

## 2026-06-02: Room DB version on About screen reads PRAGMA user_version (#238)
- **Choice**: `DbOperations.getActualDatabaseVersion()` reads `appDb?.openHelper?.readableDatabase?.version` (maps to SQLite `PRAGMA user_version`) instead of the compile-time constant `AppDatabase.CURRENT_VERSION`.
- **Reason**: The compile-time constant says what version the code declares; `PRAGMA user_version` says what version the database actually is on disk. They can diverge after `fallbackToDestructiveMigration` or backup restore. The issue explicitly requested "reading it from migration, not Gradle."
- **Considered**: Using `AppDatabase.CURRENT_VERSION` directly (simpler but potentially inaccurate), embedding in `BuildConfig` via Gradle (explicitly rejected by issue).
- **Tradeoff**: A null-safe fallback to `AppDatabase.CURRENT_VERSION` is needed since `openHelper` can be null when the DB is closed (during backup operations). The About screen is never reachable during those operations.

## 2026-06-02: Update #64 — Play Store checklist finalized
- **Choice**: Replaced the tutorial-style issue body with a concise checklist reflecting current progress.
- **Reason**: CI/release pipeline is complete; most Play Console setup is done. The original issue body was a generic guide, not a project-specific checklist.
- **Tradeoff**: None — the issue is now actionable.

## 2026-06-02: `openOutputStream("wt")` to prevent ZIP tail corruption (#237)
- **Choice**: Changed `contentResolver.openOutputStream(uri)` → `contentResolver.openOutputStream(uri, "wt")` in `SettingsFragment.doBackup()`.
- **Reason**: Without `"wt"` (write-truncate) mode, the content provider opens the existing file for writing at offset 0 but does NOT truncate it. If the new backup is smaller than the old file, the old central directory and EOCD remain at the tail. `unzip` finds the stale EOCD and reads garbage entry listings (CRC mismatch, filename mismatch, bad offsets).
- **Considered**: Deleting the file before writing (not possible with content URIs), manually truncating via `FileChannel.truncate(0)` (requires `ParcelFileDescriptor`).
- **Tradeoff**: `"wt"` mode is supported by all standard Android DocumentsProvider implementations. A non-standard provider might ignore it — but the app would fail with the old behavior anyway.

## 2026-06-02: New session pre-fills defaults + auto-creates first section (#236)
- **Choice**: `MainFragment.addNewSession()` now sets `name = getString(R.string.new_session_name)` ("New Session") and `description = getString(R.string.new_session_description)` ("New Session Description"). After inserting the session, it also creates a default section via `Section(resources.getString(R.string.default_section_name), Constants.DEFAULT_SECTION_DURATION_SECONDS)` and `dbOperations.insertSection(session, section)`.
- **Reason**: Previously, new sessions opened with blank name/description and zero sections, leaving users with an empty screen and no guidance. Pre-filling defaults and auto-creating a section eliminates the empty state and matches the mental model of "a session has at least one section."
- **Considered**: Adding an empty-state UI (TextView placeholder when sections list is empty) — rejected as more complex and the session would still appear empty. Auto-creating the section is simpler and makes the session immediately usable.
- **Tradeoff**: Every new session starts with one "Unnamed" section (60s, demo bell). Users who want zero sections must delete it. This matches the most common workflow (create session → add section → edit section).

## 2026-05-27: Bell sliders: direct volume, normal direction, matched steps (#235)
- **Choice**: Replaced the inverted "dimming" seekbar (left=loud, right=quiet, `vol = 100 - progress*10`) with standard direct volume seekbar (left=quiet, right=loud, `max=same as system slider`). Section title changed from "Bell Dimming" to "Configure Bell Volume". Removed `VOLUME_STEP_SIZE`/`VOLUME_MAX_STEP` constants.
- **Reason**: User wanted normal seekbar behavior and same step count as system alarm slider. Dimming concept was confusing (reduced from 100% = dimmed).
- **Tradeoff**: Bell slider now maps 10-100% volume to system slider steps (e.g., 0-7). Linear mapping gives ~12% per step on 7-step devices.

## 2026-06-06: Global crash handler with separate-process CrashActivity (#240)
- **Choice**: Implemented `Thread.setDefaultUncaughtExceptionHandler` in `ZazenTimerApplication` that launches a new `CrashActivity` (running in `:crash` process) to display exception details in an AlertDialog.
- **Reason**: Previously, uncaught exceptions silently crashed the app with no user-visible feedback. A global handler ensures all exceptions are caught and displayed as a pop-up with the exception type, message, and stack trace. The separate process ensures the dialog can launch even if the main process is corrupted.
- **Considered**: Try-catch wrapping individual entry points (brittle, misses background thread crashes), in-process crash activity (vulnerable to corrupted main process), ACRA integration (heavy dependency for a simple dialog).
- **Tradeoff**: CrashActivity runs in a separate process, meaning a second app instance is briefly spawned. The `ZazenTimerApplication`'s `onCreate` skips setting the handler in the crash process to avoid recursion.

