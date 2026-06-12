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
- **Jede Room-Migration braucht einen Test:** Zu jeder neuen `Migration(X, Y)` muss ein `RoomMigrationTest`-Fall existieren, der die Migration auf einer temporären V1-Datenbank mit realistischen Daten ausführt und Daten-Integrität + Schema-Korrektheit prüft. Die Migration muss direkt via `.migrate(db)` aufgerufen werden (nicht über Room Builder), um das exakte Laufzeitverhalten zu testen. Die Tests müssen zumindest umfassen: Daten-Erhalt, Schema-Korrektheit (PK NOT NULL, Indices, Default-Werte), und Indices-Existenz.

## Test Infrastructure
- API levels for instrumentation tests are defined in `gradle.properties` (`zazentimer.test.apis`).
- `scripts/run-instrumentation.sh` reads API levels dynamically — never hardcode them.
- Hostname-specific API lists use `zazentimer.test.apis.<hostname>` keys in `gradle.properties` (e.g., `zazentimer.test.apis.claw=34`).
- Shared test utilities (ScreenRobot, IdlingResource, PreFlightRule) live in `src/testFixtures/`.
- `DevicePreFlightRule` is applied in `HiltTestRunner.onStart()` to ensure screen is awake and animations disabled.
- Android Test utilities use `java-test-fixtures` via `testFixtures { enable = true }` in AGP, NOT the standalone plugin.
- **Launching long-running scripts**: Always use `echo "cd <dir> && <cmd>" | at now` to schedule via `atd`. Never use `nohup &` from the bash tool — the tool's shell timeout kills the process. Redirect stdout/stderr to `/dev/null` since the scripts already tee to log files.
- **Monitoring test runs**: Use `scripts/summarize-tests.sh --date YYYY-MM-DD` to get an at-a-glance report. Check process liveness with `ps aux | grep -E "(gradle|emulator|run-instrument)" | grep -v grep`.

## Database
- All asynchronous DB operations in `DbOperations` must be wrapped with `withIdling { ... }` to ensure Espresso synchronization.
- Bell references use the `bells` table (`_id`, `name`, `uri`, `is_builtin`). Sections and session_bell_volumes reference bells via `bellId` FK exclusively — no `bell`/`bellUri` duplicate columns.
- New sections must have a valid `bellId` before insert. `DbOperations.insertSection()` defaults `bellId=0` to the demo bell via `getBellByUri()` to satisfy FK constraint.
- Sessions use `rank` column for persistent ordering. Query: `ORDER BY rank, name COLLATE NOCASE`. New sessions get `rank = MAX(rank) + 1`.
- `sanitizeBellUris()` runs at every startup in `ZazenTimerActivity.onCreate()` (and after manual restores). It is the sole bell-sync function — replaces both `ensureBellsTableConsistent()` and `seedBuiltinBells()`. Syncs builtin bells with `BellCollection`, removes orphaned bells (reassigning sections to demo), and enforces strict 1:1 mapping for custom bells with disk.
- **FK constraint testing**: Any unit test that creates sections must first insert a bell row and set `bellId` to the valid ID.
- **Migration snapshots**: Every Room migration that modifies table schemas must include index creation statements for all indices in `@Entity`. Room validates full schema after migration.
- Column order in SQLite is cosmetic — Room maps by column name, not position. Reordering columns in a migration is harmless.
- **Session rank persistence**: Session ranks are saved in TWO places: (1) `MainFragment.onPause()` via `runBlocking` — the lifecycle safety net that fires on navigation/pause; (2) `MainFragment.suspendUpdateSessionList()` at the TOP (before clearing and reloading from DB) — prevents the in-memory drag order from being overwritten by stale DB data when actions like Add/Delete/Duplicate call `updateSessionList()`. Both assign `sessions[i].rank = i` and call `dbOperations.updateSession()`.
- **Session list selection**: Use full-list refresh (`for (i in items.indices) notifyItemChanged(i)`) instead of tracking `previous`/`selectedPosition` for targeted `notifyItemChanged`. This avoids stale-index bugs when `removeItem`/`insertItem`/`moveItem` shift the backing list without updating selection state. The cost (N rebinds) is negligible for typical session counts (~10). Applies to both `setSelectedPosition()` and the in-adapter click listener.
- **After backup restore, call `sanitizeBellUris()`**: `SettingsFragment.doRestore()` calls `dbOperations.sanitizeBellUris()` after a successful backup restore. This ensures bell URIs are normalized for the current app's package name, builtin bells match `BellCollection`, and custom bells have a strict 1:1 mapping with files on disk. Never skip this step — the database may have stale URIs from a different build type or app version.

## Release Workflow
- Use `scripts/release.sh <version>` (e.g. `./scripts/release.sh 3.0.8`) to create releases. The script updates `.fdroid.yml` with the new version, commits, and tags.
- Push with: `git push origin main v<version>`
- The tag triggers the GitHub Actions Play Store pipeline automatically.
- F-Droid updates are handled separately via the MR in fdroiddata (see #242).

## F-Droid Metadata
- `.fdroid.yml` in project root contains the F-Droid build recipe.
- `prebuild:` must be a single line (not a YAML list).
- `versionName:` and `CurrentVersion:` must NOT be quoted.
- Use `gradle: - yes` (not explicit task names) — F-Droid handles task selection.
- `AutoUpdateMode: None` + `UpdateCheckMode: None` for initial submission; switch to `Tags` + `Version` for auto-updates later.
- `SOUND_LICENSES.md` documents audio asset licensing for F-Droid compliance.
- Backup test ZIP files live in `app/src/test/resources/backups/` with `_old` suffix. Do NOT place ZIP files in `databases/` — F-Droid scanner flags them as binary blobs.

## Prisma — Device DB
- `prisma/desired/schema.prisma` is **human-only** — hand-crafted SOLL schema. Never auto-generate or edit by agent.
- `prisma/current/schema.prisma` is auto-generated by `prisma db pull --force` from the device DB (IST schema).
- `prismaCheckSchema` diffs the two — a non-zero exit signals that the device DB has drifted from the desired schema. This is expected after migrations until the human updates `desired/`.

## Voting API + Frontend
- `prisma/voting_api.tsx` enthält alles: API-Routes + JSX-Komponenten + htmx-Fragmente. Eine Datei, solange es kompakt bleibt.
- JSX-Import: `compilerOptions: { jsx: "precompile", jsxImportSource: "hono/jsx" }` in `deno.json`.
- CDN-Dependencies: Pico CSS (`@picocss/pico@2`) + htmx (`htmx.org@2.0.4`). Keine npm-Installation.
- DB-Pattern: Lazy Singleton aus `lib/prisma.ts` — `getPrisma()` cached den Client, WAL Mode aktiviert.
- htmx-Fragment-Routes erben denselben Host/Port, unterscheiden sich nur im Pfad.
- Alle Frontend-Seiten nutzen `<Layout>` als Wrapper (Navigation + CDN-Imports).
- `LevelBadge`: 1=red, 2=orange, 3=yellow, 4=light-green, 5=green (Pico CSS Farb-Variablen).

## Prisma — Translation DB
- `prisma/translations/schema.prisma` uses Prisma Client generator with `runtime = "deno"` and `output = "../generatedprismaclient"`. Never use Node.js/npm.
- Deno import map alias: `"prismaclient": "./generatedprismaclient/client.ts"` in `prisma/deno.json`.
- **Type-checking**: Always run `deno task check` from within `prisma/` (uses `--config=deno.json`). Never use standalone `deno check` from the project root — it cannot resolve the `prismaclient` alias.
- `prisma generate --schema=translations/schema.prisma` outputs client to `prisma/generatedprismaclient/`.
- All Prisma CLI operations use `deno run -A npm:prisma@^6.19.3` (via `prisma/deno.json` tasks).
- Translation DB is NOT auto-pulled from device — schema evolves by hand (like `desired/`).
- Confidence `CHECK(confidence BETWEEN 1 AND 5)` is raw SQL in init migration's `CREATE TABLE`, not Prisma-level constraint.
- Seed scripts must be idempotent: use `upsert` (unique fields only), never `create`. Run multiple times safely.
- `supported_languages.json` is the single source of truth for supported languages — manually curated, shape `{ bcp_47, posix_code, iso_639_3, directory, english_name, whisper_response }[]`. Add/remove languages by editing this file and running `deno task seed`.
- `llmmodels_master.json` is the single source of truth for LLM models and their providers — manually curated, shape `{ name: string, providers: string[] }[]`. Both `seed.ts` and `translate.ts` import it via `import masterModels from "..." with { type: "json" }` (static import, no runtime file reading). The seed reads model names and `deleteMany`s DB models not in the list (cascade-deletes votes + proficiencies). `translate.ts` builds `MODEL_PROVIDERS` from the `providers` arrays.
- `whisper_languages.json` is a static 100-entry map from Whisper `tokenizer.py` — update only if Whisper adds languages.
- Three non-unique columns by design: `iso_639_3` (por/srp/zho have multiple regions), `whisper_response` (regional variants share same response), and confidence `Int` with CHECK (not enum, avoids alphabetical sort).

## Database — SQLite CHECK constraints
- CHECK constraints on `CREATE TABLE` only. `ALTER TABLE ADD CHECK` is NOT supported by SQLite in Prisma's migration engine.
- Put CHECK constraints directly in the init migration's SQL, not in separate migration files.
- Confidence `Int` with CHECK — never use Prisma enum for numeric ranges (SQLite sorts enums alphabetically).

## Translation Pipeline (#202)
- Shared Prisma DB queries in `prisma/lib/db.ts` — verwendet von `translate.ts`.
- Orchestrator at `prisma/translate.ts` — nistete Loop `for model × for locale → dispatch → verify → store`.
- `prisma/lib/opencode_client.ts` — HTTP Client für opencode Server (`createSession`, `sendMessage`, `closeSession`).
- `prisma/lib/verify.ts` — Output-Verifikation: JSON-Struktur, Keys, null erlaubt, Placeholder-Check.
- One opencode session per (model, locale) pair. `terminateSession()` (abort + delete) is called via `try/finally` on ALL exit paths (success, timeout, quota, retry exhaustion). `abortSession()` sends `POST /session/{id}/abort` to interrupt the running fiber; `closeSession()` sends `DELETE` to remove session storage.
- `--all` runs haben 10 Minuten Timeout. Erreichte Sessions sind dauerhaft in DB (Idempotenz via `getExistingVotes`-Check).
- Output-JSON erlaubt `"translation": null` für unbekannte Strings → kein Vote, kein Fehler.
- Proficiency (1-5) ist Pflicht. Fehlt sie → `Deno.exit(1)`. Keine halben Sachen in der DB.
- Translate-Ergebnis-Log: `stringCount` (Votes mit nicht-leerem Text) / `emptyCount` (Votes mit `""`) / `skippedMasterString` (nur geloggt wenn >0). Null-Responses vom LLM zählen auf `emptyCount`.
- Provider-Label-Format im Language-Start-Log: `"to only provider (opencode-go)"` wenn das Model nur einen Provider hat, sonst `"to opencode (rank 1/4)/gemini-3.5-flash"`.
- `MODEL_PROVIDERS` in `prisma/translate.ts` is built from `llmmodels_master.json` at import time — no hardcoded model-provider mapping in code. `buildFallbackChain()` durchläuft diese Liste und filtert gegen `fetchAvailableModels()`.
- Verify-Fehler bei Translate/Proficiency enthalten den Roh-Output (erste 200 Zeichen) + den eigentlichen Fehlertext, damit das Model auf Retry seinen Fehler selbst diagnostizieren kann.
- Translate/Proficiency Skills erlauben jetzt das Lesen der Output-Dateien (`translate-output.json`, `proficiency-output.json`) für Retry-Diagnose.
- `validateDatabaseConsistency()` runs unconditionally at translate startup: checks models, languages, and master strings against DB. Aborts only if source files have items MISSING from DB (not for extras in DB). Shared consistency functions (`checkModelConsistency`, `checkLanguageConsistency`, `checkMasterStringConsistency`) live in `db.ts`, used by both `translate.ts` and `seed.ts`.

## Translation Scoring Constants (in `db.ts`)
- `SETTLED_SCORE_THRESHOLD = 7`: string is "settled" (no more votes needed) when best translation score >= 7. Override via `--settled-threshold <N>` CLI flag.
- `TRANSLATION_SCORE_THRESHOLD = 3`: minimum score to export a translation or return it from `getTranslation()`.
- `MIN_VOTE_PROFICIENCY = 2`: default for `--min-proficiency` — models below this are never asked to translate.
- `byScoreThenCount`: shared comparator (score primary, modelCount tiebreak) used in all ranking sorts. Never duplicate sort logic.
- Proficiency filter removed from scoring: all models with a proficiency record contribute their level to the score. The `--min-proficiency` gate at request time is the single control.

## Translation Export
- `prisma/export.ts` generiert ALLE `values-*`-Verzeichnisse im Target komplett neu aus der Voting-DB.
  Vorherige `values-*`-Verzeichnisse werden gelöscht — der Export ist die autoritative Quelle.
- `--target=<pfad>` ist Pflicht (z.B. `../app/src/main/res`).
- Languages are exported if they have at least one translation scoring >= `TRANSLATION_SCORE_THRESHOLD`. Languages with no qualifying translations are skipped (no directory created).
- A language NOT in `supported_languages.json` CAN still be exported if its translations meet the threshold.
- Ausgabe ist alphabetisch nach Key sortiert für deterministische, diff-freundliche Deltas.

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
- **Bell DB tests**: Always seed at least one builtin bell via `TestBellHelper.seedBell()`. Insert custom bells with `isBuiltin=false` and `file://` URIs. For `deleteCustomBell` tests, `BellCollection.getDemoBell()` returns null in Robolectric — the fallback `bellDao?.getBuiltinBells()?.firstOrNull()` is used instead.
- **ManageBellsPage pattern**: Use `RecyclerViewActions.actionOnItem<ViewHolder>(withBellName(name), clickChildViewWithId(R.id.deleteButton))` to find and click delete for a specific bell. Custom matcher `withBellName()` checks `R.id.bellName` text on each item. `hasSibling()` from Hamcrest is NOT available — use custom TypeSafeMatcher instead.
- **Fresh PrismaClient for blocking endpoints**: In `prisma/lib/db.ts`, `getLanguageById()` and `getMasterStringById()` call `prisma = await getPrisma()` on every invocation (not just module-level init). This avoids Prisma v6 library engine's intermittent internal blocking with a singleton.
- **getEvaluation() return includes modelDetails**: Each entry contains `modelDetails: {name, level}[]` for tooltips, plus `modelNames: string` (comma-joined, backwards compatible).
- **getVotesGrouped() returns master_stringsId**: Enables linking to `/strings/{id}` detail pages.
- **WAL checkpoint after translate batches**: `dispatchTranslate()` runs `PRAGMA wal_checkpoint(TRUNCATE)` after each successful store cycle to prevent WAL bloat and reader blocking.


## Play Store Automation
- Service Account key is located at `~/.config/iron-country-322716-8ab0815de79f.json` (Local) or provided via GitHub Secrets for CI.
- The Python environment is managed in the project root under `.venv/`.
- Automation scripts are located in `scripts/play_store/`:
    - `setup.sh`: Bootstraps the local `.venv`.
    - `check_status.py`: Lists current tracks and releases.
    - `update_notes.py`: Updates release notes for a specific track. Usage: `.venv/bin/python3 scripts/play_store/update_notes.py <track> <notes> [language]`
    - `activate_alpha_bundle.py`: Re-activates a specific version code (e.g. 3000300) in the alpha track if it was deactivated.

- **No Automated Blind Scripts**: The LLM performs translations interactively with explicit Zen meditation context. Script-based, context-free automated translation of meditation-specific strings (e.g. via Google Translate API, MyMemory, or any batch script) is **strictly forbidden**. Every locale must be translated by an LLM sub-agent that understands Zazen, Kinhin, mindfulness, and singing-bowl terminology.
- **Extremely Strict LLM Instructions**: When using LLMs for translation, you **MUST** provide extremely precise instructions regarding XML tags and placeholders (`%s`, `%1$d`, `&lt;`, `&gt;`). LLMs often corrupt these in low-resource languages, leading to runtime formatting crashes.
- **Explicit Fallback Rule**: Explicitly prompt any translation sub-agent: *"If you do not have high confidence in this specific language, or if you cannot guarantee that EVERY placeholder will be preserved exactly, you MUST leave the string in English. Guessing or hallucinating will cause the application to crash."*
- **Translation Workflow**: `prisma/translate.ts` liest den englischen Master `strings.xml`, seeded die Strings in die Voting-DB und führt autonom das LLM-Voting durch (per-skill Sub-Agents). Die DB ist die zentrale Autorität für alle Übersetzungen. Nach einem Translate-Lauf wird die DB via `deno task savetranslationstogit` als SQL-Dump versioniert. `prisma/export.ts` regeneriert die `values-*`-Verzeichnisse komplett aus der DB.
- Always use `R.string` — never hardcode user-facing text in Kotlin, XML, or navigation graphs
- Mark programmatic strings as `translatable="false"` in XML
- Never add `abc_*` strings — those come from AndroidX automatically
- Use `@string/` references in layout XML and navigation graphs
- Verify placeholder counts match after translation

## Opencode HTTP API
- System prompt und Model werden **per Message** gesendet (`POST /session/{id}/message`), nicht bei Session-Erstellung (`POST /session {}`).
- Model-Ref Format: `{ providerID: "opencode", modelID: "gpt-5.5" }` — getrennt von Provider, nicht der Gesamt-Slug.
- `sendMessage()` response: aus `parts[]` den Eintrag mit `type: "text"` extrahieren (nicht `parts[0]`).
- Bei Retry in derselben Session: `system` wird **immer** mitgesendet (SKILL_TRANSLATE/SKILL_PROFICIENCY), nicht nur beim ersten Request. Das Model behält so auch auf Retry vollen Kontext.
- Auth: Basic Auth mit `OPENCODE_SERVER_USERNAME`/`OPENCODE_SERVER_PASSWORD`.

## Emulator Scripts
- `start-emulator.sh` and `stop-emulator.sh` are sourceable libraries. Use `[[ "${BASH_SOURCE[0]}" == "${0}" ]]` guard for standalone mode.
- `emulator_launch(avd, serial, logfile, ...flags)` requires a logfile parameter. Callers pass `-noaudio`, `-no-snapshot-save`, `-wipe-data` etc. explicitly.
- `-noaudio` must be passed by callers based on display state (Xvfb vs real X11); it is NOT auto-detected in emulator_launch.
- New emulator-management scripts (`run-instrumentation.sh`, `create-emulator-snapshots.sh`) source `start-emulator.sh` and `stop-emulator.sh` — do NOT duplicate functions.
