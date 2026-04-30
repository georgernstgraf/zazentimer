# Decisions

Architectural and technical decisions made in this project.
Each entry documents WHAT was decided and WHY.

## 2026-04-04: UI Test Meta-Definition
- **Choice**: Adopted a Markdown-based UI Test Plan (`docs/ai/UI_TEST_PLAN.md`) instead of immediately relying on BDD/Cucumber.
- **Reason**: To quickly establish a single source of truth for UI test coverage that is easily readable by both human developers and AI agents without adding new framework dependencies immediately.
- **Considered**: Using BDD frameworks like Cucumber or purely relying on Java/Kotlin source code as documentation.
- **Tradeoff**: Requires manual updates to the Markdown file when tests are implemented or status changes.

## 2026-04-04: AndroidX Preferences Migration
- **Choice**: Replaced custom legacy `DialogPreference` implementations (`VolumePreference`, `BrightnessPreference`) with standard AndroidX `SeekBarPreference`, and flattened nested `PreferenceScreen` nodes into `PreferenceCategory`.
- **Reason**: To resolve `InflateException` crashes and broken navigation caused by incompatibilities between legacy preference frameworks and the new AndroidX `PreferenceFragmentCompat`.
- **Considered**: Manually wiring up fragment transactions for nested `PreferenceScreen`s.
- **Tradeoff**: Flattens the settings hierarchy slightly, placing all settings on one scrollable screen, but vastly improves stability and reduces maintenance burden.

## 2026-04-04: Full Automation of Start Meditation UI Test Scenario
- **Choice**: Implemented full automation for the "Start Meditation" UI test scenario following Android best practices with Page Object Model pattern.
- **Reason**: To achieve full test coverage for the core meditation functionality and update the UI_TEST_PLAN.md status from partially automated (🟡) to fully automated (🟢).
- **Considered**: Keeping the partial automation status and only documenting the enhancements.
- **Tradeoff**: Required enhancing existing page objects and updating documentation, but provides complete test coverage for this critical user flow.

## 2026-04-04: Background Timer — setAlarmClock() over setExactAndAllowWhileIdle
- **Choice**: Replaced `AlarmManager.setExactAndAllowWhileIdle()` with `AlarmManager.setAlarmClock()` for meditation section timing, and moved the `SectionEndReceiver` from runtime registration to static manifest registration.
- **Reason**: `setAlarmClock()` is the same API Android's built-in Clock app uses — it receives the highest scheduling priority, is immune to Doze suppression, and guarantees the alarm fires at the exact time. The previous approach could be deferred or suppressed by battery optimization on longer sessions.
- **Considered**: Adding WorkManager as a fallback mechanism; running a Handler/CountDownTimer inside the foreground service; keeping the existing AlarmManager approach with additional WakeLocks.
- **Tradeoff**: `setAlarmClock()` shows a small alarm icon in the status bar, which is actually appropriate for a meditation timer. Also required switching from `ELAPSED_REALTIME_WAKEUP` to RTC (wall clock) time. Static receiver registration means the receiver survives process death, but full process-death recovery (restoring meditation state) is not yet implemented.

## 2026-04-04: AndroidX Preferences Migration — Dead Code Note
- **Followup**: The legacy files `VolumePreference.java` and `BrightnessPreference.java` were left in the source tree after the migration to AndroidX `SeekBarPreference`. These classes extend the deprecated `android.preference.DialogPreference` (not AndroidX) and are not referenced by any active code. They were removed in #21.

## 2026-04-05: Gradle Wrapper Migration
- **Choice**: Replaced the bundled `gradle-7.5/` distribution (122 MB, 239 tracked files) with the standard Gradle Wrapper (`gradlew`).
- **Reason**: The bundled distribution inflated every clone by 122 MB, had no integrity verification, and made upgrades difficult. The wrapper auto-downloads the correct Gradle version, provides SHA-256 verification, and is the universal standard for Android projects.
- **Considered**: Keeping the bundled distribution; using a system-installed Gradle.
- **Tradeoff**: First build on a clean machine downloads Gradle (~60 MB) instead of having it pre-bundled, but this is a one-time cost. CI can cache `~/.gradle/wrapper` for subsequent runs.

## 2026-04-05: Developer Onboarding Documentation
- **Choice**: Created `docs/ai/ONBOARDING.md` with Linux-specific setup instructions and expanded `README.md` with project overview and quick-start.
- **Reason**: A new developer could not find where Android Studio was installed, where the SDK lived, or how to create an emulator. The README was 2 lines with no setup guidance.
- **Considered**: Using only the README for setup info; relying on Android Studio's new-project wizard.
- **Tradeoff**: Adds a documentation maintenance burden, but prevents repeated onboarding questions. Linux-specific paths may need adjustment for macOS/Windows developers.

## 2026-04-06: Navigation and Information Architecture (#20)
- **Choice**: Replaced Spinner with RecyclerView session cards, added BottomNavigationView (Sessions/Settings/About), FAB for new sessions, per-card popup menus for Edit/Copy/Delete, back-press confirmation during meditation, and Material Motion transitions.
- **Reason**: All navigation actions were buried in the overflow menu; Spinner didn't scale; no visual hierarchy. Bottom nav makes Settings and About discoverable. Session cards show name + description + duration.
- **Considered**: ViewPager2 for section editing; inline accordion expansion for sections; keeping overflow menu approach.
- **Tradeoff**: Bottom nav adds a persistent UI element but eliminates menu hunting. Kept separate-screen section editing as-is (simple, already works). Section editing deferred to future if needed.

## 2026-04-06: Room Schema Alignment — Migration 4→5
- **Choice**: Bumped Room database version to 5 with MIGRATION_4_5 that recreates sessions and sections tables with explicit `NOT NULL` on `_id`, `name`, and `description` columns.
- **Reason**: The old SQLiteOpenHelper database had `name TEXT NOT NULL` and `_id integer` (nullable), but Room entities expected `_id INTEGER NOT NULL` and `String name` (nullable). Room's schema validation failed on the physical phone's existing database.
- **Considered**: Using `fallbackToDestructiveMigration()` (rejected — would destroy user data).
- **Tradeoff**: Adds a migration step but preserves all user data. The table-recreation approach (CREATE new → INSERT SELECT → DROP old → RENAME) is standard for fixing NOT NULL mismatches.

## 2026-04-06: Instrumented Test Rule Ordering
- **Choice**: Added `@Rule(order = 0)` to `HiltAndroidRule` and `@Rule(order = 1)` to `ActivityScenarioRule` in all test classes.
- **Reason**: JUnit 4 applies rules in non-deterministic order via reflection. Without explicit ordering, `ActivityScenarioRule` could fire before Hilt initialized the test component on API 31 devices, causing `NoActivityResumedException`.
- **Considered**: Using `RuleChain` instead of `order` parameter.
- **Tradeoff**: Minimal change, works on both API 31 and API 35 devices.

## 2026-04-07: App Documentation — Markdown with Embedded Screenshots (#51)
- **Choice**: Single `docs/app-docs/APP_DOCUMENTATION.md` file with embedded screenshot images using relative paths, organized by screen with TOC.
- **Reason**: Markdown is universally viewable, easy to commit alongside code, and supports embedded images. A single file keeps all documentation cohesive. The screenshots folder lives alongside for easy reference.
- **Considered**: Multiple Markdown files per screen; HTML for layout flexibility; AsciiDoc for richer formatting.
- **Tradeoff**: Markdown has limited layout control (no side-by-side images), but is sufficient for per-screen documentation and works natively on GitHub.

## 2026-04-07: Screen Capture via UI Automation (#51)
- **Choice**: Used `adb shell uiautomator dump` for element hierarchy + `adb shell screencap` for visual capture + vision sub-agents for analysis, navigating between screens via `adb shell input tap` with coordinates from the UI dump.
- **Reason**: uiautomator provides exact element bounds, resource IDs, and text content. Vision analysis adds visual description that the XML dump can't provide (colors, icons, overall layout feel). Combining both gives complete screen documentation.
- **Considered**: Using Espresso/UI Automator test scripts for navigation; manual screenshot capture.
- **Tradeoff**: Coordinate-based tapping is fragile and screen-resolution-dependent. Popup menus may close unexpectedly. But it works well enough for one-time documentation capture.
- **Choice**: Replaced all deprecated API usages: `startService()` → `startForegroundService()` for foreground services, raw string `getSystemService("...")` → `Context.*_SERVICE` constants, and `onActivityResult()` → Activity Result API (`registerForActivityResult`).
- **Reason**: The app targets API 29-34 but used APIs deprecated/removed in the target range. `startForegroundService()` is required since API 26 for foreground services. Raw string service names are fragile. `onActivityResult` is deprecated since API 30.
- **Considered**: Keeping `onActivityResult` with `@SuppressWarnings`; using `ContextCompat.startForegroundService()`.
- **Tradeoff**: Activity Result API requires registering launchers before `onCreate` completes (field initializers work fine). Removes 3 `onActivityResult` overrides across 3 files. No functional changes.

## 2026-04-07: Duplicate Session Fix (#52)
- **Choice**: Added `source.id = 0;` before `insertSession(source)` in `DbOperations.duplicateSession()`, plus an Espresso instrumented test (`DuplicateSessionTest.java`) that exercises the overflow-menu duplicate action.
- **Reason**: The method reused the original session's `_id` (primary key), causing `SQLiteConstraintException` on `@Insert`. Room's `@PrimaryKey(autoGenerate = true)` only auto-generates when the value is 0.
- **Considered**: Using `@Insert(onConflict = REPLACE)` instead of resetting the ID.
- **Tradeoff**: Resetting to 0 is more correct (creates a truly new row vs. silently overwriting). Test covers both crash regression and "Copy of" prefix verification.

## 2026-04-07: Consistent Release APK Signing via GitHub Secrets (#53)
- **Choice**: Store a single release keystore in GitHub Secrets (base64-encoded) and decode it in CI for signing. Keystore generated once with `keytool`, stored in 4 secrets: `RELEASE_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- **Reason**: Per-run keystores produce differently-signed APKs, preventing in-place upgrades. Android refuses to install an APK signed with a different key over an existing installation. A single persistent keystore ensures all release APKs are signed identically.
- **Considered**: Generating keystore per CI run; using the debug signing key for release; storing keystore in the repo (rejected — security risk).
- **Tradeoff**: Requires keystore backup outside GitHub (if lost, no future release can upgrade over previously installed ones). Keystore is valid for ~27 years (10000 days).

## 2026-04-07: Navigation Restructure — Meditation as Bottom Nav Tab (#54)
- **Choice**: Moved Meditation to bottom nav as a persistent tab (Sessions, Meditation, Settings). Moved About from bottom nav to overflow menu as an AlertDialog (matching Privacy pattern). Removed `AboutFragment` entirely.
- **Reason**: User requested Meditation be a first-class navigation destination accessible at any time, with the idle state showing a paused meditation at 00:00 for the selected session. About and Privacy are both informational dialogs that don't need dedicated tabs.
- **Considered**: Keeping About as a fragment; making Meditation a modal; keeping the old 3-tab layout.
- **Tradeoff**: Meditation tab adds complexity (dual-state fragment: idle vs running). Bottom nav visible during meditation means users can switch tabs mid-session (service continues). Removed dedicated About screen in favor of dialog — less real estate but simpler architecture.

## 2026-04-10: Volume System Simplification (#56)
- **Choice**: Removed the 3-layer volume system (system stream + master volume + per-section volume) and replaced it with a 2-layer system: system STREAM_ALARM volume (user-controlled) + per-section dimming via `MediaPlayer.setVolume()`. Deleted `VolumeCalc` and `VolumeInfo`. Removed output channel selection (always STREAM_ALARM). Removed channel muting preferences (mute_alarm, mute_music). Inverted the per-section volume UI to "Dim bell" semantics (0 = full loudness, 100 = silent).
- **Reason**: The 3-layer volume system was overengineered. VolumeCalc split volume between system stream steps and MediaPlayer volume using a logarithmic curve, requiring save/restore of system stream volume around each bell playback. This was confusing for users who had 3 volume controls that interacted non-obviously. The simplified system lets the user control the system alarm stream volume directly (via Settings slider), and per-section "dimming" attenuates individual bells that are too loud.
- **Considered**: Removing master volume only (keeping VolumeCalc); removing per-section volume only; keeping 3 layers with clearer UI; keeping both output channels.
- **Tradeoff**: Users lose the Music stream option (no earphone support for bells via STREAM_MUSIC). Very few apps use STREAM_ALARM, so alarm-stream muting during meditation was removed without significant impact. The dimming slider is inverted from the DB storage (volume 0-100 stored as-is, displayed as 100-volume dimming percentage) — no DB migration needed but the mapping must be maintained in SectionEditFragment.

## 2026-04-10: Fix Corrupted Meditation State After Natural Finish (#55)
- **Choice**: Two single-line fixes: (1) clear `meditationState` LiveData to null in `MeditationViewModel.stopUpdateThread()`, (2) set `meditationRunning = false` in `MeditationFragment.showIdleState()`.
- **Reason**: When meditation finishes while the user is on another tab, the activity-scoped ViewModel's `meditationState` LiveData retained its last `MeditationUiState(running=true)`. New `MeditationFragment` observers received this stale value, setting `meditationRunning = true` while no service was running. The `showIdleState()` method corrected the UI but never reset the `meditationRunning` flag, so the play button's click handler called the no-op `pauseMeditation()` instead of `startMeditationFromIdle()`.
- **Considered**: Adding a dedicated `MeditationUiState.stopped()` factory method; resetting state in `onResume()` only; posting a `running=false` state instead of null.
- **Tradeoff**: Setting LiveData to null is the simplest fix and is already handled by the existing observer (`if (state == null || !state.running)`). No new classes or methods needed.

## 2026-04-11: Idle State as Proper MeditationUiState (#57)
- **Choice**: Idle is modeled as `MeditationUiState(running=false)` with full section arc data computed from `Section[]` in the ViewModel, instead of a null LiveData or a separate code path. The ViewModel never emits null. The fragment has a three-branch observer (idle/running/paused).
- **Reason**: Making idle a proper state with section arc data lets the TimerView show colored section arcs in idle (matching paused-at-0 appearance). The user requested that idle look the same as paused, except for a greyed stop button and no back-press interception. Storing session name in `MeditationUiState` makes it available in all states.
- **Considered**: Creating a `Meditation` object for idle (rejected — requires `MeditationService` instance); keeping null LiveData for idle (rejected — can't carry section data); computing idle state in the Fragment (rejected — ViewModel owns state).
- **Tradeoff**: `emitIdleState()` duplicates section boundary computation from `Meditation.java` (lines 206-236) with `currentSectionIdx=0`. If the boundary logic changes, both places must be updated. But the duplication is small (~10 lines) and the alternative (decoupling Meditation from MeditationService) would be a much larger refactor.

## 2026-04-13: Bell Audio Normalization
- **Choice**: Normalize all bell sound files to a consistent -16.0 LUFS.
- **Reason**: To eliminate significant loudness discrepancies between different bell sound files (a range of 9.4 LU was found).
- **Considered**: Manual normalization; leaving as-is.
- **Tradeoff**: Requires batch processing script; build-time overhead; user-imported files will not be automatically normalized (user advised of target LUFS).

## 2026-04-15: Volume Control and Audio Routing Refactor (#60)
- **Choice**: Removed the orphaned "Master Volume" setting from `preferences.xml` and refactored `Audio.java` to use modern `AudioAttributes` (USAGE_ALARM).
- **Reason**: The "Master Volume" slider was redundant and non-functional after issue #56 simplified the volume system. `AudioAttributes` is the recommended way to handle audio routing in modern Android, ensuring better compatibility with Bluetooth devices.
- **Considered**: Linking the "Master Volume" slider to the system alarm volume (rejected — redundant since system volume is globally accessible).
- **Tradeoff**: Simplifies UI and codebase but requires users to use system volume controls for global volume adjustment.

## 2026-04-29: OOBE Language Translation (#67)
- **Choice**: Translated the app into all 127 languages supported by Google
  Translate that also appear in the Android OOBE language picker. Used
  `deep-translator` (GoogleTranslator) over `googletrans` (4.0.0rc1 returned
  empty JSON, was unreliable). Used incremental `--diff` approach instead of
  full re-translation to minimize API calls (~127 per change vs ~16,000).
- **Reason**: The app needed to match the language list Android shows during
  initial device setup. Machine translation via Google is the only feasible
  approach for 127 languages (~16,000+ strings). The `--diff` approach lets
  developers change one string and propagate it to all locales in seconds.
- **Considered**: Using a dedicated translation service (Crowdin, Lokalise) —
  rejected for cost/complexity. Using GPT for higher quality — rejected for
  rate limits at 127-language scale.
- **Tradeoff**: Google Translate quality varies by language (poor for Bemba,
  Lao, Nuer). Some short strings may be left untranslated. Product names,
  URLs, and emails are kept in English across all locales.

## 2026-04-29: Zero Deprecation Warnings Policy
- **Choice**: Enabled `-Xlint:deprecation` in `app/build.gradle` and fixed all 14 deprecation warnings across 10 files to achieve a zero-warning build.
- **Reason**: The compiler was silently hiding deprecation warnings. Without `-Xlint:deprecation`, only a generic "Some input files use or override a deprecated API" message appeared. Enabling the flag and fixing everything ensures the codebase stays forward-compatible and makes future deprecations immediately visible.
- **Considered**: Suppressing warnings with `@SuppressWarnings("deprecation")`; only fixing the most critical deprecations.
- **Tradeoff**: Some replacements are more verbose (e.g., `addMenuProvider()` vs. `setHasOptionsMenu()`), but all follow the officially recommended migration paths. Dead-code branches (like the pre-API-26 `Notification.Builder` else) were removed entirely since `minSdk=29`.

## 2026-04-30: Test-Max Job Uses Direct `am instrument` on API 36 (#80)
- **Choice**: The `test-max` CI job uses `adb shell am instrument` directly instead of `./gradlew connectedDebugAndroidTest` for API 36 testing.
- **Reason**: `./gradlew connectedDebugAndroidTest` uses the Gradle Unified Test Platform (UTP) which discovers 0 tests on API 35+ emulators. This is a UTP bug/limitation — the same test APK and `HiltTestRunner` discover all 6 tests when run via raw `am instrument`. The `target: google_apis` system image is also required (the `default` image doesn't include necessary components for test discovery).
- **Considered**: Upgrading AGP to a version with UTP fixes for newer APIs; using `host` execution mode instead of orchestrator; staying on API 34.
- **Tradeoff**: No HTML test report is generated by `am instrument`, but the raw instrumentation output is visible in CI logs with proper exit code checking.
