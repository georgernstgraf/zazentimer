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

## Database
- All asynchronous DB operations in `DbOperations` must be wrapped with `withIdling { ... }` to ensure Espresso synchronization.

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

## Internationalization (i18n) & LLM Translations
- **Extremely Strict LLM Instructions**: When using LLMs for translation, you **MUST** provide extremely precise instructions regarding XML tags and placeholders (`%s`, `%1$d`, `&lt;`, `&gt;`). LLMs often corrupt these in low-resource languages, leading to runtime formatting crashes.
- **Explicit Fallback Rule**: Explicitly prompt any translation sub-agent: *"If you do not have high confidence in this specific language, or if you cannot guarantee that EVERY placeholder will be preserved exactly, you MUST leave the string in English. Guessing or hallucinating will cause the application to crash."*
- Always use `R.string` — never hardcode user-facing text in Kotlin, XML, or navigation graphs
- New strings go to `values/strings.xml` first, then run `retranslate.py --diff`
- Mark programmatic strings as `translatable="false"` in XML
- Never add `abc_*` strings — those come from AndroidX automatically
- Use `@string/` references in layout XML and navigation graphs
- Run `retranslate.py --locales X,Y` for targeted locale fixes
- Verify placeholder counts match after translation
