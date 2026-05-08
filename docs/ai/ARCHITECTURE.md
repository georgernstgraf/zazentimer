# Architecture

Living structural map of the system as of 2026-05-08.
Overwritten when structural changes occur during a session.

## Overview
Android meditation timer (ZazenTimer) targeting API 29-35.
100% Kotlin, AndroidX libraries, Hilt DI (KSP), Room database (KSP), Navigation Component, Material Motion transitions, Foreground Service for session lifecycle.
Build: AGP 9.1.1, Gradle 9.x, Kotlin DSL, viewBinding.
Source: `src/main/kotlin/` (app), `src/test/kotlin/` (unit tests), `src/androidTest/kotlin/` (instrumented tests).

## App Components
| Component | Class | Role |
|-----------|-------|------|
| Activity | `ZazenTimerActivity` | Single host for NavHostFragment, manages service binding, menu, WakeLocks |
| Service | `MeditationService` | Foreground service, hosts Meditation, handles section-end intents |
| Receiver | `SectionEndReceiver` | Static BroadcastReceiver for alarm-fired section-end events |

## Navigation Architecture
**Navigation Component** with `nav_graph.xml`. `NavHostFragment` in `main.xml` using `FragmentContainerView`.

| Fragment | Nav Label | Role |
|----------|-----------|------|
| `MainFragment` | Sessions | Session list (RecyclerView cards) + Start button |
| `MeditationFragment` | Meditation | Three-state: idle, running, paused |
| `SessionEditFragment` | Edit Session | Edit session name/description + section list |
| `SectionEditFragment` | Edit Section | Edit section duration, bell, volume, gap |
| `SettingsFragment` | Settings | Preferences via PreferenceFragmentCompat |

**Toolbar overflow menu**: 4 items — Add Session, Settings, Privacy, About. Only visible on `mainFragment`.
**AppBarConfiguration**: 1 top-level destination (`mainFragment`, shows no up button). Wired via `NavigationUI.setupActionBarWithNavController()`.
**About**: Shown as `AlertDialog` from overflow menu (not a fragment destination). Uses `Html.fromHtml()` + `LinkMovementMethod` for clickable links in about text. Matches Privacy dialog pattern.

Navigation flow:
```
Sessions screen --[Start]--> MeditationFragment (auto-starts)
Sessions screen --[Overflow > Add Session]--> SessionEditFragment (new session)
Sessions screen --[Card Edit]--> SessionEditFragment
Sessions screen --[Overflow > Settings]--> SettingsFragment
SessionEdit --[Edit Section]--> SectionEditFragment
Overflow menu --[Privacy]--> AlertDialog
Overflow menu --[About]--> AlertDialog
```

**MeditationFragment states:**
- **Idle** (`running=false`): TimerView shows colored section arcs (from ViewModel's `emitIdleState()`). First section name in ring, session name in dedicated `TextView`. Greyed stop button. Play button starts meditation. No back-press interception.
- **Running** (`running=true, paused=false`): Live timer with Pause/Stop. Full color stop button. Back press shows stop confirmation.
- **Paused** (`running=true, paused=true`): Frozen timer with Play/Stop. Full color stop button. Back press shows stop confirmation.
- Session name displayed in dedicated `TextView` (`sessionNameText`) below TimerView in all states.
- Zen circle indicator (`zenIndicator`) in toolbar shows when `MeditationService` is running.
- Session selection persists via `PREF_KEY_LAST_SESSION` in SharedPreferences.
- **Sessions screen during meditation**: All interactions blocked (card selection, Start, Edit/Copy/Delete, FAB).

## Transitions
- **MaterialFadeThrough**: MainFragment enter/re-enter, SettingsFragment enter
- **MaterialSharedAxis X**: drill-down navigation (SessionEditFragment enter/return, SectionEditFragment)
- **MaterialSharedAxis Y**: MeditationFragment enter/return
- Note: Fragment back-navigation plays MaterialFadeThrough re-enter transition on MainFragment. `onResume()` fires mid-transition before layout completes, requiring timing-independent approaches for any layout-sensitive logic.

## Business Objects (`bo/`)
| Class | Table | Fields |
|-------|-------|--------|
| `Session` | `sessions` | id, name, description |
| `Section` | `sections` | id, name, duration (seconds), bell, bellUri, bellcount (1-5), bellpause (1-15), volume (0-100, 100=full), rank, fkSession |

## Database Layer
- **Room** — `AppDatabase` (version 5), entities: `SessionEntity`, `SectionEntity`
- **`DbOperations`** — `@Singleton` via Hilt, wraps Room DAOs for CRUD operations
- **`SessionDao` / `SectionDao`** — Room DAOs with `@Query`, `@Insert`, `@Update`, `@Delete`
- **Migrations**: 1→2 (settings table), 2→3 (no-op), 3→4 (volume column), 4→5 (recreate tables with explicit NOT NULL)

## Build Config
- `BuildConfig.GIT_HASH` — 7-character Git commit hash, injected at build time via `git rev-parse --short=7 HEAD` in `build.gradle.kts`. Used in About dialog.
- AGP 9.1.1, Gradle 9.x, Kotlin DSL, KSP (Room + Hilt), compileSdk 36, minSdk 29, Java 21.
- ktlint 14.2.0 + detekt 1.23.8 (visibility-only in CI, enforcement deferred to #108).
- No `kotlinOptions` block — AGP 9.x derives JVM target from `compileOptions`.

## Timer Architecture (Meditation Flow)
```
User presses Start (Sessions tab or Meditation tab)
  → ZazenTimerActivity.startMeditation()
  → MeditationService.startMeditation() creates Meditation, calls start()
  → Meditation.start() mutes phone, schedules first section via AlarmManager.setAlarmClock()
  → When alarm fires:
      → SectionEndReceiver (static, in AndroidManifest.xml)
      → forwards intent to MeditationService.onStartCommand()
      → Meditation.onSectionEnd() plays bells, schedules next section (or finishes)
  → On finish: MeditationService.stopForeground(), sends broadcast, stopSelf()
```

## Dependency Injection
- **Hilt** — all Activities use `@AndroidEntryPoint`, ViewModels use `@HiltViewModel`
- `DbOperations` is `@Singleton` via Hilt, injected where needed
- Tests use `HiltTestRunner` → `HiltTestApplication`
- Test rules: `@Rule(order = 0)` HiltAndroidRule, `@Rule(order = 1)` ActivityScenarioRule

## Key Classes
| Class | Location | Role |
|-------|----------|------|
| `MeditationService` | `service/` | Foreground service, hosts Meditation, handles section-end intents |
| `Meditation` | `service/` | Core timer logic: AlarmManager scheduling, bell playback, pause/resume |
| `MeditationServiceBinder` | `service/` | Binder exposing MeditationService to Activity |
| `ServCon` | `service/` | ServiceConnection proxy for Activity ↔ Service communication |
| `SectionEndReceiver` | `service/` | Static BroadcastReceiver for alarm-fired section-end events |
| `Audio` | `audio/` | MediaPlayer-based bell playback on STREAM_ALARM with per-section dimming |
| `BellCollection` | `audio/` | Singleton: 8 built-in bells + custom bell scanning |
| `TimerView` | `views/` | Custom circular arc timer widget with section visualization |
| `SessionListAdapter` | `fragments/` | RecyclerView adapter for session cards with selection tracking |
| `SessionTouchHelperCallback` | `fragments/` | ItemTouchHelper.Callback for long-press drag reorder (no swipe, no DB persistence) |
| `MaxHeightRecyclerView` | `fragments/` | RecyclerView subclass that caps height in `onMeasure()` — used to enforce 60% maximum for session list, leaving 40% minimum for zen circle image |
| `HiltTestRunner` | androidTest | Custom AndroidJUnitRunner that injects HiltTestApplication + filters `@RequiresDisplay` tests when `headless=true` (legacy, kept for safety) |
| `RequiresDisplay` | androidTest | Annotation for tests requiring a real display surface. All currently annotated tests pass under Xvfb. Kept as safety marker |

## Data Flows
- **AlarmManager.setAlarmClock()** → SectionEndReceiver → MeditationService → Meditation.onSectionEnd() → Audio.playBell()
- **Bell playback volume:** System STREAM_ALARM volume (set by user) × section dimming (`MediaPlayer.setVolume(volume/100f)`)
- **UI updates:** Handler.postDelayed (300ms polling) reads Meditation state → TimerView. Idle state computed from `Section[]` in `MeditationViewModel.emitIdleState()`.
- **Preferences:** SharedPreferences via PreferenceManager → read in Activity/Service/Fragments
- **Database:** DbOperations → Room DAOs → SessionEntity/SectionEntity → Session/Section BOs
- **Navigation:** NavController.navigate() and popBackStack() from ZazenTimerActivity helper methods (showMeditationScreen, showSettingsScreen, showMainScreen, showSessionEditFragment).

## Commands
| Command | Purpose | Pipeline Stage |
|---------|---------|----------------|
| `./gradlew assembleDebug assembleRelease` | Build APKs (no tests) | Stage 1 (local iteration) |
| `./gradlew testDebugUnitTest` | JVM unit tests only | Stage 1 |
| `scripts/run-stage2.sh` | Instrumented tests on min+max API, detects `$DISPLAY` for Xvfb | Stage 2 |
| `scripts/run-nightly.sh` | Full matrix all APIs, VPS cron | Stage 3 |
| `./gradlew assembleDebugAndroidTest` | Build test APK (compile androidTest sources) | — |
| `./gradlew ktlintCheck` | Kotlin lint (visibility-only, not enforced) | CI |
| `./gradlew detekt` | Static code analysis (visibility-only, not enforced) | CI |
| `adb install -r app/build/outputs/apk/debug/app-debug.apk` | Install debug APK | — |
| `adb shell am instrument -w at.priv.graf.zazentimer.test/at.priv.graf.zazentimer.HiltTestRunner` | All instrumented tests via am instrument (API 35+) | Stage 2/3 |

## Translation Architecture

### Source of Truth
`app/src/main/res/values/strings.xml` is the canonical English source.
All 136 locale files (`values-*/strings.xml`) are derivatives generated
from it via automated translation.

### Per-App Language (Android 13+)
`res/xml/locales_config.xml` declares all 136 locales via BCP 47 tags.
`AndroidManifest.xml` references it via `android:localeConfig`.
No in-app language picker — users select language via system settings on Android 13+.
Pre-Android 13 devices use automatic Android resource resolution.

### Directory Structure
```
app/src/main/res/
├── values/strings.xml          ← Canonical English source
├── values-af/strings.xml       ← Afrikaans (136 languages)
├── values-zu/strings.xml       ← Zulu
└── ...
```

### Translation Tooling
| File | Purpose |
|------|---------|
| `scripts/retranslate.py` | Incremental locale update from English source (Google Translate + MyMemory fallback) |
| `scripts/locales.json` | Mapping of 136 Android resource dirs → translator codes (`gt_code` or `mymemory_code`) |
| `scripts/keep_english.json` | List of string keys to copy verbatim (never translate) |
| `.venv/` (gitignored) | Python venv with `deep-translator` dependency |

### Workflow
```
English source changed → python scripts/retranslate.py --diff → 127 locales updated → assembleDebug
```

## Knowledge Files (`docs/ai/`)
| File | Purpose | Update mode |
|------|---------|------------|
| ONBOARDING.md | Developer setup guide | Overwrite |
| UI_TEST_PLAN.md | Meta-definition for UI test scenarios | Append/Update |
| HANDOFF.md | Open tasks for next session | Overwrite |
| DECISIONS.md | Chronological record of choices | Append |
| ARCHITECTURE.md | Living structural map | Overwrite |
| CONVENTIONS.md | Ongoing rules to follow | Append |
| PITFALLS.md | Hard-won failure knowledge | Append |
| DOMAIN.md | Business/domain rules | Append |
| STATE.md | Current project status | Overwrite |
