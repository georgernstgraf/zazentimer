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
