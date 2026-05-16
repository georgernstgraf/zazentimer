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
- **Choice**: All output from each API-level test run is written to `logs/api<level>-YYYY-MM-DD.log` via `tee -a`. On failure, `adb logcat -d` dumps to `logs/api<level>-YYYY-MM-DD-logcat.txt`. Overall run log at `logs/instrumentation-YYYY-MM-DD.log` via `exec > >(tee ...)`. Added `--debug` flag for logcat dumps on green runs too.
- **Reason**: Previous runs lost all diagnostic output to terminal-only — when terminal timed out or Xvfb crashed, stack traces, `am instrument` output, and crash details were irrecoverable.
- **Considered**: Single combined log file (hard to navigate), only logging failures (misses context for intermittent issues).
- **Tradeoff**: ~50-100MB logcat dump per failed API; `logs/` is gitignored so no repo bloat.
