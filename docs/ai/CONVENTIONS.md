# Conventions

Coding patterns, naming rules, and style agreements for this project.
Follow these without question. Do not deviate unless explicitly told.

## Naming
- Java package structure: `de.gaffga.android.*`
- Page Object classes for instrumented tests: `*Page.java` in `screens/` package (e.g., `MainPage.java`, `MeditationPage.java`)

## File Layout
- Standard Android project structure: `app/src/main/java`, `app/src/main/res`, `app/src/main/assets`.

## API Patterns
- RecyclerView adapter overflow menu: Use a callback interface (e.g. `OnSectionActionListener`), inflate a `PopupMenu` from an XML menu resource in `onBindViewHolder`, and wire menu item IDs to interface callbacks. Follow the pattern established in `SessionListAdapter.java`.
- Use AndroidX libraries (`androidx.*`). The legacy `android.support` migration is complete.
- Use `Context.*_SERVICE` constants instead of raw string service names.
- Use `startForegroundService()` (API 26+) instead of `startService()` for foreground services.
- Use `registerForActivityResult()` (Activity Result API) instead of `onActivityResult()`.

## Dependency Injection
- Hilt is the DI framework. Use `@AndroidEntryPoint` on Activities/Fragments, `@HiltViewModel` on ViewModels.
- Use `@Inject` constructor injection for injectable classes. `DbOperations` is `@Singleton`.
- Tests use `@HiltAndroidTest` + `HiltAndroidRule`. The test runner is `HiltTestRunner` (not `AndroidJUnitRunner`).

## Translation
- Use `deep_translator` (GoogleTranslator) in `.venv/bin/python` for string translations.
- Mask format specifiers (`%1$d`, `%1$s`, `%2$d`) and `\n` with Unicode bracket placeholders (`⦅0⦆`, `⦅1⦆`...) before sending to Google Translate. Restore after translation.
- Do not translate: `about1`, `about2`, `about3`, `app_description`, `app_name`, `bell_name_1` through `bell_name_8`, `theme_value_dark`, `theme_value_light`.
- Skip all `abc_*` prefixed strings.
- Escape raw apostrophes in output (`'` → `\'`) for Android XML compatibility. Use regex `(?<!\\)'` to avoid double-escaping already-escaped `\'`.
- Always run `./gradlew assembleDebug` after translation to verify the build is green.
- Translation scripts stored in `scripts/translate_batch*.py`.

## Testing
- Ensure standard `lint` and `./gradlew build` commands pass.
- After deleting or renaming resource files (layouts, strings, drawables, IDs in `public.xml`), always run `./gradlew clean` before building and testing. Incremental builds can produce stale R.class entries that cause instrumented tests to fail with incorrect resource IDs.
- **Always verify GitHub Actions passes after every push.** Run `gh run list --limit 3` and `gh run view <id>` to check. Do not assume CI is green.

## Workflow
- **Issue management:** Use the `issue-workflow` skill for all GitHub issue operations (start, commit, finish). Every commit must reference a GitHub issue number.
- **Knowledge persistence:** Use the `knowledge-persistence` skill to update `docs/ai/` files after meaningful changes or when wrapping up a session.

## Git Workflow
- **Trunk-based development.** Commit directly to `main`. No branches, no PRs.
- Use descriptive commit messages referencing issue numbers (e.g. `fix: backup fails on Android 11+ (#18)`).

## Knowledge Persistence
- Project documentation lives in `docs/ai/`. See `AGENTS.md` for the bootstrap reading order.
- Use the `knowledge-persistence` skill to update docs/ai/ files after meaningful changes.

## CI
- Build command: `./gradlew build`
- JDK version: 17 (AGP 7.4+ requirement)
- Keep GitHub Actions versions up to date (`actions/checkout@v4`, `actions/setup-java@v4`) to avoid Node.js deprecation warnings.
- Release APK signing uses GitHub Secrets (`RELEASE_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Keystore must be decoded to `$RUNNER_TEMP/` using an absolute path (Gradle resolves relative paths against daemon working dir, not project dir).
- The keystore and the private key are stored pgp-encrypted in georgs svn under private/
- Three CI artifacts: `app-debug`, `app-release`, `test-results`.
