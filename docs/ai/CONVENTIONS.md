# Conventions

Coding patterns, naming rules, and style agreements for this project.
Follow these without question. Do not deviate unless explicitly told.

## Naming
- Java package structure: `de.gaffga.android.*`

## File Layout
- Standard Android project structure: `app/src/main/java`, `app/src/main/res`, `app/src/main/assets`.

## API Patterns
- Support legacy `android.support` libraries for now.

## Testing
- Ensure standard `lint` and `gradle build` commands pass.
- After deleting or renaming resource files (layouts, strings, drawables, IDs in `public.xml`), always run `gradle clean` before building and testing. Incremental builds can produce stale R.class entries that cause instrumented tests to fail with incorrect resource IDs.
