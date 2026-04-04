# Architecture

Living structural map of the system as of 2026-04-04.
Overwritten when structural changes occur during a session.

## Overview
Reconstructed legacy Android meditation timer (ZazenTimer) targeting API 29-34.
Uses AndroidX libraries, AlarmManager for timing, Foreground Service for session lifecycle.

## Commands (`commands/`)
| Command | Purpose | Delegates to |
|---------|---------|-------------|
| `./gradle-7.5/bin/gradle build` | Build the project | Gradle build system |
| `adb install -r ...` | Install the APK to device | ADB |

## Knowledge Files (`docs/ai/`)
| File | Purpose | Update mode |
|------|---------|------------|
| UI_TEST_PLAN.md | Meta-definition for UI test scenarios | Append/Update |
| HANDOFF.md | Open tasks for next session | Overwrite |
| DECISIONS.md | Chronological record of choices | Append |
| ARCHITECTURE.md | Living structural map | Overwrite |
| CONVENTIONS.md | Ongoing rules to follow | Append |
| PITFALLS.md | Hard-won failure knowledge | Append |
| DOMAIN.md | Business/domain rules | Append |
| STATE.md | Current project status | Overwrite |

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
| `SectionEndReceiver` | `service/` | Static BroadcastReceiver for alarm-fired section-end events |
| `Audio` | `audio/` | MediaPlayer-based bell playback with volume management |
| `ServCon` | `service/` | ServiceConnection proxy for activity ↔ service communication |

## Data Flows
- APK source -> decompilation output -> project source/resources -> gradle build -> APK.
- AlarmManager.setAlarmClock() → SectionEndReceiver → MeditationService → Meditation.onSectionEnd() → Audio.playBell()
