# Zazen Meditation Timer

Android meditation timer with configurable sessions, multiple bell sounds, and background timer reliability.

**Package:** `de.gaffga.android.zazentimer` | **Min SDK:** 29 | **Target SDK:** 34 | **Java 17**

## Quick Start

```bash
# Build
./gradlew build

# Run unit tests
./gradlew test

# Run instrumented tests (requires emulator)
./gradlew connectedDebugAndroidTest

# Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

For full setup instructions (JDK, Android Studio, SDK, emulator), see [docs/ai/ONBOARDING.md](docs/ai/ONBOARDING.md).

## Project Overview

A session-based meditation timer where each session consists of one or more timed sections. Each section has configurable duration, bell sound, volume, and bell count. The app plays bells at section boundaries and keeps the timer running reliably in the background via a foreground service and `AlarmManager.setAlarmClock()`.

### Key Features
- Multiple configurable sessions with timed sections
- 8 built-in bell/gong sounds plus custom bell import
- Drag-and-drop section reordering with swipe-to-delete
- Light and dark themes
- Backup/restore via Storage Access Framework
- Screen-on and brightness control during meditation

### Architecture
- **1 Activity** + **7 Fragments** (manual Fragment transactions)
- **Foreground Service** for meditation lifecycle
- **AlarmManager** for exact section-end timing
- **Raw SQLite** with reflection-based ORM
- 100% Java, no DI framework

## Documentation

Project documentation for AI agents and developers lives in `docs/ai/`:

| File | Purpose |
|------|---------|
| [ONBOARDING.md](docs/ai/ONBOARDING.md) | Developer setup guide |
| [ARCHITECTURE.md](docs/ai/ARCHITECTURE.md) | Structural map of the system |
| [CONVENTIONS.md](docs/ai/CONVENTIONS.md) | Coding rules and patterns |
| [DOMAIN.md](docs/ai/DOMAIN.md) | Business/domain knowledge |
| [PITFALLS.md](docs/ai/PITFALLS.md) | Known gotchas |

## Git Workflow

Trunk-based development. Commit directly to `main`. No branches, no PRs.

```
fix: resolve startup crash (#8)
feat: use setAlarmClock() for reliable background timer (#11)
docs: update onboarding documentation (#21)
```
