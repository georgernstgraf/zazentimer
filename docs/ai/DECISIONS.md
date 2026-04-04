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
