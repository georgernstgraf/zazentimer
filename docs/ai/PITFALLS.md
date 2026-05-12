# Pitfalls

Things that do not work, subtle bugs, and non-obvious constraints.
Read this file carefully before making changes in affected areas.

- **Espresso & 300ms Polling**: Legacy polling in ViewModels prevented Espresso from reaching an idle state, causing `AppNotIdleException`.
- **Service Binding Race**: Fragments attempting to interact with `MeditationService` before `onServiceConnected` caused NPEs or lost commands; use `MeditationRepository` as the stable intermediary.
- **UTP / API 35 Bug**: AGP 9.1.1 UTP runner may report "0 tests found" on API 35; use manual `am instrument` fallback in scripts.
- **Emulator Hardware**: Never use `-target google_apis` with newer emulators (36.5.10+); use `-target android`.
- **Database Race**: DB operations are async; without `IdlingResource`, tests may read old data before a write finishes.
- **java-test-fixtures Plugin Conflict**: Adding `id("java-test-fixtures")` to the plugins block conflicts with AGP's built-in `testFixtures { enable = true }`. Only use the AGP block — do not add the standalone plugin.
- **Kotlin Init Order NPE**: In `MeditationViewModel`, the `init` block accessed `meditationState` LiveData which was declared AFTER the init block, causing NPE on API 36. Always declare LiveData/StateFlow fields before the `init` block that uses them.
- **runBlocking(Dispatchers.Main) Deadlock**: Calling `runBlocking(Dispatchers.Main)` from the Main thread deadlocks because `runBlocking` blocks the thread while `Dispatchers.Main` tries to dispatch to it. Use `runBlocking { withContext(Dispatchers.Main) { } }` or avoid blocking calls on Main.
- **Captured Null ServiceConnection**: `MeditationViewModel.startMeditation()` captured `serviceConnection` into a local val before `bindToService()` set it, causing stale null reference. Always read mutable fields directly inside coroutine closures.
- **systemd-oomd Kills Emulators**: On hosts with `systemd-oomd` active, emulator memory of 2048+MB triggers OOM kills. Reduce emulator memory (`-memory 2048`) and kernel cache clearing (`echo 3 > /proc/sys/vm/drop_caches`) help.
- **No orchestrator with am instrument**: The `am instrument` path does NOT install the orchestrator APK, but `execution = "ANDROIDX_TEST_ORCHESTRATOR"` requires it. Set `execution = "HOST"` in build.gradle.kts to match the `am instrument` path, or install orchestrator manually.
- **Emulator Not Advertising ADB**: If `adb wait-for-device` hangs, check that the emulator process is actually running (not killed by oomd) and that ADB server is started (`adb start-server`). Use `tmux` to isolate long-running emulator processes from script timeout.
- **Hilt @TestInstallIn requires `replaces`**: Omitting `replaces` from `@TestInstallIn` causes KSP processor failure: `@TestInstallIn, 'replaces' class is invalid or missing`. Must always specify at least one module.
- **@TestInstallIn replaces drops ALL bindings**: When `replaces = [DatabaseModule::class]` is used, ALL `@Provides` methods from that module are unavailable in tests. Every binding needed from the replaced module must be re-provided in the test module (e.g., both `ZazenClock` and `CoroutineDispatchers`).
- **kotlinx-coroutines-test for instrumented tests**: `kotlinx-coroutines-test` added as `testImplementation` is NOT available in `androidTest/`. Must add `androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")` for instrumented tests using `UnconfinedTestDispatcher`.
- **Auto-tag FAILED_APIS leak**: `run-instrumentation.sh` mutates `FAILED_APIS` inside `run_gradle_test()`/`run_am_instrument_test()` on failure but never clears it on retry success. If an API fails on attempt 1 but passes on attempt 2, `FAILED_APIS` still contains that API, blocking auto-tag. Must clean FAILED_APIS after retry success.
- **LLM Translation Hallucinations**: In extremely low-resource languages (e.g., Kashmiri `ks`, Manipuri `mni`, Bemba `bem`), LLMs will hallucinate completely unrelated strings (sometimes NSFW) or silently destroy `%s` and `%1$d` placeholders, causing `UnknownFormatConversionException` at runtime. You MUST guardrail sub-agents to fall back to English if they lack high confidence or if they alter formatting.
