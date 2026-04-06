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

## 2026-04-06: Phase 2 — Modernize Deprecated APIs (#23)
- **Choice**: Replaced all deprecated API usages: `startService()` → `startForegroundService()` for foreground services, raw string `getSystemService("...")` → `Context.*_SERVICE` constants, and `onActivityResult()` → Activity Result API (`registerForActivityResult`).
- **Reason**: The app targets API 29-34 but used APIs deprecated/removed in the target range. `startForegroundService()` is required since API 26 for foreground services. Raw string service names are fragile. `onActivityResult` is deprecated since API 30.
- **Considered**: Keeping `onActivityResult` with `@SuppressWarnings`; using `ContextCompat.startForegroundService()`.
- **Tradeoff**: Activity Result API requires registering launchers before `onCreate` completes (field initializers work fine). Removes 3 `onActivityResult` overrides across 3 files. No functional changes.
