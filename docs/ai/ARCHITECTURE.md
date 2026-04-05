# Architecture

Living structural map of the system as of 2026-04-05.
Overwritten when structural changes occur during a session.

## Overview
Android meditation timer (ZazenTimer) targeting API 29-34.
100% Java, AndroidX libraries, AlarmManager for timing, Foreground Service for session lifecycle.

## App Components
| Component | Class | Role |
|-----------|-------|------|
| Activity | `ZazenTimerActivity` | Single host for all fragments, manages service binding, menu, WakeLocks |
| Service | `MeditationService` | Foreground service, hosts Meditation, handles section-end intents |
| Receiver | `SectionEndReceiver` | Static BroadcastReceiver for alarm-fired section-end events |

## Fragment Architecture
All navigation is manual `FragmentTransaction.replace()` into `FrameLayout @id/content`.

| Fragment | Location | Role |
|----------|----------|------|
| `MainFragment` | `fragments/` | Landing screen: session Spinner + Start button |
| `MeditationFragment` | `fragments/` | Active meditation: Pause/Stop buttons |
| `SessionEditFragment` | `fragments/` | Edit session name/description + section list |
| `SectionEditFragment` | `fragments/` | Edit section duration, bell, volume, gap |
| `SettingsFragment` | `fragments/` | Preferences via PreferenceFragmentCompat |
| `AboutFragment` | `fragments/` | Version info |
| `TimePickerFragment` | `fragments/` | Dialog: minutes/seconds NumberPickers |

Navigation flow:
```
MainFragment --[Start]--> MeditationFragment
MainFragment --[Edit]--> SessionEditFragment --[Edit Section]--> SectionEditFragment
MainFragment --[Settings]--> SettingsFragment
MainFragment --[About]--> AboutFragment
```

## Business Objects (`bo/`)
| Class | Table | Fields |
|-------|-------|--------|
| `Session` | `sessions` | id, name, description |
| `Section` | `sections` | id, name, duration (seconds), bell, bellUri, bellcount (1-5), bellpause (1-15), volume (0-100), rank, fkSession |

## Database Layer
- **`ZenTimerDatabase`** extends `SQLiteOpenHelper` — database `"zentimer"`, version 4
- **`DbOperations`** — static utility wrapping CRUD for sessions and sections
- **`DbMapper`** — reflection-based ORM using `@DbTable`, `@DbColumn`, `@DbPrimaryKey` annotations
- Tables: `sessions`, `sections`, `settings` (legacy, migrated to SharedPreferences)

## Timer Architecture (Meditation Flow)
```
User presses Start
  → ZazenTimerActivity binds to MeditationService
  → MeditationService.startMeditation() creates Meditation, calls start()
  → Meditation.start() mutes phone, schedules first section via AlarmManager.setAlarmClock()
  → When alarm fires:
      → SectionEndReceiver (static, in AndroidManifest.xml)
      → forwards intent to MeditationService.onStartCommand()
      → Meditation.onSectionEnd() plays bells, schedules next section (or finishes)
  → On finish: MeditationService.stopForeground(), sends broadcast, stopSelf()
```

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
| `BetterListView<T>` | `betterlist/` | Custom ListView with drag-to-reorder and swipe-to-delete |
| `JwtCallCredentials` | `grpc/` | JWT auth for gRPC (prepared, not yet invoked) |

## Data Flows
- **AlarmManager.setAlarmClock()** → SectionEndReceiver → MeditationService → Meditation.onSectionEnd() → Audio.playBell()
- **UI updates:** Handler.postDelayed (300ms polling) reads Meditation state → TimerView
- **Preferences:** SharedPreferences via PreferenceManager → read in Activity/Service/Fragments
- **Database:** DbOperations (static SQLiteDatabase) → DbMapper → Session/Section BOs

## Commands
| Command | Purpose |
|---------|---------|
| `./gradlew build` | Build + unit tests + lint |
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
