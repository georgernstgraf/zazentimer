# Conventions

Coding patterns, naming rules, and style agreements for this project.
Follow these without question. Do not deviate unless explicitly told.

## Naming
- Use `ZMT_` prefix for Log tags in ViewModels and Services.

## State Management
- Use `MeditationRepository` for all timer-related state.
- UI components should observe `MeditationRepository.meditationState` via `StateFlow` (collected in ViewModels).
- Never use UI-level polling (e.g. `Handler.postDelayed` or `delay()` loops) to update timer views.

## Testing
- Use `ZazenClock` for all time-related logic.
- Instrumented tests must register `IdlingResourceManager.countingIdlingResource`.
- Prefer `StateFlow` over `LiveData` for new state streams to better support coroutine-based testing.

## Test Infrastructure
- API levels for instrumentation tests are defined in `gradle.properties` (`zazentimer.test.apis`).
- `scripts/run-instrumentation.sh` reads API levels dynamically — never hardcode them.
- Shared test utilities (ScreenRobot, IdlingResource, PreFlightRule) live in `src/testFixtures/`.
- `DevicePreFlightRule` is applied in `HiltTestRunner.onStart()` to ensure screen is awake and animations disabled.
- Android Test utilities use `java-test-fixtures` via `testFixtures { enable = true }` in AGP, NOT the standalone plugin.
- **Launching long-running scripts**: Always use `echo "cd <dir> && <cmd>" | at now` to schedule via `atd`. Never use `nohup &` from the bash tool — the tool's shell timeout kills the process. Redirect stdout/stderr to `/dev/null` since the scripts already tee to log files.
- **Monitoring test runs**: Use `scripts/summarize-tests.sh --date YYYY-MM-DD` to get an at-a-glance report. Check process liveness with `ps aux | grep -E "(gradle|emulator|run-instrument)" | grep -v grep`.

## Database
- All asynchronous DB operations in `DbOperations` must be wrapped with `withIdling { ... }` to ensure Espresso synchronization.
- Bell references use the `bells` table (`_id`, `name`, `uri`, `is_builtin`, `resource_name`). Sections and session_bell_volumes reference bells via `bell_id` FK.
- New sections must be created with `bell = -2` (BELL_INDEX_NONE) and a valid `bellUri` from `BellCollection`. `bellId` is resolved at startup by `ensureBellsTableConsistent()`.
- Built-in bells always have `is_builtin = true` and `resource_name` set to the R.raw resource name (e.g., "bell1", "bell2", "dharma107").

## Detekt
- `./gradlew detekt` must exit 0 before any commit. Zero violations policy.
- Use `@Suppress("TooManyFunctions")` for classes that are cohesive but exceed function limits (e.g., main Activity, core business logic).
- Extract helper classes when extraction improves readability; don't extract purely to satisfy detekt thresholds.
- Use `@Suppress("MagicNumber")` sparingly — prefer extracting to named constants even for color/dimension values.
- Use `@Suppress("ConstructorParameterNaming")` for Room Entity classes where `_id` naming is conventional.

## Instrumented Test Reliability
- Grant `POST_NOTIFICATIONS` via `adb shell pm grant` in test `@Before` for APIs 33+.
- Never use `Dispatchers.Main` inside `runBlocking` on the main thread — use `withContext(Dispatchers.Main) { }` inside a non-dispatched `runBlocking` instead.
- Call `DevicePreFlightRule.execute()` in `HiltTestRunner.onStart()` wrapped in try-catch for resilience.
- Use `execution = "HOST"` in `build.gradle.kts` when the `am instrument` path is used (no orchestrator APK).
- Keep emulator memory at `-memory 2048` to avoid `systemd-oomd` kills.
- **Never use `SystemClock.sleep()` in tests**: Use `Espresso.onIdle()` after UI actions, `UiAutomator Until.hasObject()` for polling. `SystemClock.sleep` blocks the main thread and prevents JUnit `Timeout` rule from working.
- **Never call `Espresso.onIdle()` in `@Before`**: Causes `TestLooperManager already held`. DB ops in `onActivity {}` are synchronous — no idle wait needed.
- **All test classes must extend `AbstractZazenTest`**: Provides `Timeout(2, MINUTES)`, `hiltRule`, and `activityRule`. Never duplicate these rules.
- **`@HiltAndroidTest` annotation is NOT inherited**: Every test class needs its own `@HiltAndroidTest` annotation AND the `import dagger.hilt.android.testing.HiltAndroidTest`. ktlintFormat removes the import as "unused" — always verify with `compileDebugAndroidTestKotlin` after `ktlintFormat`.
- **Use `inRoot(isDialog())` for AlertDialog interactions**: On API 36+, the system enforces edge-to-edge (`EDGE_TO_EDGE_ENFORCED`), which can cause activity windows to lose focus when `AlertDialog` appears. Espresso's default root matcher requires window focus, causing `RootViewWithoutFocusException`. Use `.inRoot(isDialog())` to target the dialog root directly. Import: `import androidx.test.espresso.matcher.RootMatchers.isDialog`.


## Play Store Automation
- Service Account key is located at `~/.config/iron-country-322716-8ab0815de79f.json` (Local) or provided via GitHub Secrets for CI.
- The Python environment is managed in the project root under `.venv/`.
- Automation scripts are located in `scripts/play_store/`:
    - `setup.sh`: Bootstraps the local `.venv`.
    - `check_status.py`: Lists current tracks and releases.
    - `update_notes.py`: Updates release notes for a specific track. Usage: `.venv/bin/python3 scripts/play_store/update_notes.py <track> <notes> [language]`
    - `activate_alpha_bundle.py`: Re-activates a specific version code (e.g. 3000300) in the alpha track if it was deactivated.

- **Extremely Strict LLM Instructions**: When using LLMs for translation, you **MUST** provide extremely precise instructions regarding XML tags and placeholders (`%s`, `%1$d`, `&lt;`, `&gt;`). LLMs often corrupt these in low-resource languages, leading to runtime formatting crashes.
- **Explicit Fallback Rule**: Explicitly prompt any translation sub-agent: *"If you do not have high confidence in this specific language, or if you cannot guarantee that EVERY placeholder will be preserved exactly, you MUST leave the string in English. Guessing or hallucinating will cause the application to crash."*
- Always use `R.string` — never hardcode user-facing text in Kotlin, XML, or navigation graphs
- New strings go to `values/strings.xml` first, then run `retranslate.py --diff`
- Mark programmatic strings as `translatable="false"` in XML
- Never add `abc_*` strings — those come from AndroidX automatically
- Use `@string/` references in layout XML and navigation graphs
- Run `retranslate.py --locales X,Y` for targeted locale fixes
- Verify placeholder counts match after translation
