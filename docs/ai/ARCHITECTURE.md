# Architecture

Living structural map of the system as of 2026-04-07.
Overwritten when structural changes occur during a session.

## Overview
Android meditation timer (ZazenTimer) targeting API 29-35.
100% Java, AndroidX libraries, Hilt DI, Room database, Navigation Component, Material Motion transitions, Foreground Service for session lifecycle.

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
| `MeditationFragment` | Meditation | Dual-state: idle (paused at 00:00) or running meditation |
| `SessionEditFragment` | Edit Session | Edit session name/description + section list |
| `SectionEditFragment` | Edit Section | Edit section duration, bell, volume, gap |
| `SettingsFragment` | Settings | Preferences via PreferenceFragmentCompat |

**BottomNavigationView**: 3 tabs — Sessions, Meditation, Settings. Always visible except during session/section editing.
**AppBarConfiguration**: 3 tab destinations (mainFragment, meditationFragment, settingsFragment) are top-level (no up button). Wired via `NavigationUI.setupActionBarWithNavController()`.
**About**: Shown as `AlertDialog` from overflow menu (not a fragment destination). Matches Privacy dialog pattern.

Navigation flow:
```
Bottom Nav: Sessions ↔ Meditation ↔ Settings
Sessions tab --[Start]--> Meditation tab (auto-starts)
Sessions tab --[FAB]--> SessionEditFragment (new session)
Sessions tab --[Card Edit]--> SessionEditFragment
SessionEdit --[Edit Section]--> SectionEditFragment
Overflow menu --[Privacy]--> AlertDialog
Overflow menu --[About]--> AlertDialog
```

**MeditationFragment states:**
- **Idle** (no running service): TimerView shows 00:00 with selected session's total duration and name. Play button starts meditation.
- **Running** (service active): Live timer with Pause/Stop. Back press shows stop confirmation.
- Session selection persists via `PREF_KEY_LAST_SESSION` in SharedPreferences. Idle state re-reads on every `onResume()`.

## Transitions
- **MaterialFadeThrough**: top-level tab switches (Sessions ↔ Meditation ↔ Settings)
- **MaterialSharedAxis X**: drill-down navigation (session edit, section edit)
- **MaterialSharedAxis Y**: meditation screen entry/exit

## Business Objects (`bo/`)
| Class | Table | Fields |
|-------|-------|--------|
| `Session` | `sessions` | id, name, description |
| `Section` | `sections` | id, name, duration (seconds), bell, bellUri, bellcount (1-5), bellpause (1-15), volume (0-100), rank, fkSession |

## Database Layer
- **Room** — `AppDatabase` (version 5), entities: `SessionEntity`, `SectionEntity`
- **`DbOperations`** — `@Singleton` via Hilt, wraps Room DAOs for CRUD operations
- **`SessionDao` / `SectionDao`** — Room DAOs with `@Query`, `@Insert`, `@Update`, `@Delete`
- **Migrations**: 1→2 (settings table), 2→3 (no-op), 3→4 (volume column), 4→5 (recreate tables with explicit NOT NULL)
- Tables: `sessions`, `sections`, `settings` (legacy, migrated to SharedPreferences)

## Build Config
- `BuildConfig.GIT_HASH` — 7-character Git commit hash, injected at build time via `git rev-parse --short=7 HEAD` in `build.gradle`. Used in About dialog.

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
| `Audio` | `audio/` | MediaPlayer-based bell playback with volume management |
| `BellCollection` | `audio/` | Singleton: 8 built-in bells + custom bell scanning |
| `VolumeCalc` | `audio/` | Calculates system stream vs. MediaPlayer volume split |
| `TimerView` | `views/` | Custom circular arc timer widget with section visualization |
| `SessionListAdapter` | `fragments/` | RecyclerView adapter for session cards with selection tracking |
| `HiltTestRunner` | androidTest | Custom AndroidJUnitRunner that injects HiltTestApplication |

## Data Flows
- **AlarmManager.setAlarmClock()** → SectionEndReceiver → MeditationService → Meditation.onSectionEnd() → Audio.playBell()
- **UI updates:** Handler.postDelayed (300ms polling) reads Meditation state → TimerView
- **Preferences:** SharedPreferences via PreferenceManager → read in Activity/Service/Fragments
- **Database:** DbOperations → Room DAOs → SessionEntity/SectionEntity → Session/Section BOs
- **Navigation:** BottomNavigationView.setSelectedItemId() for tab switches; NavController for drill-down
- **Tab switching:** All tab switches use `BottomNavigationView.setSelectedItemId()`. Navigation actions only for drill-down screens (session/section editing).

## Commands
| Command | Purpose |
|---------|---------|
| `./gradlew build` | Build + unit tests + lint |
| `./gradlew assembleDebugAndroidTest` | Build test APK (compile androidTest sources) |
| `./gradlew connectedDebugAndroidTest` | Instrumented tests on device/emulator |
| `adb install -r app/build/outputs/apk/debug/app-debug.apk` | Install debug APK |

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
