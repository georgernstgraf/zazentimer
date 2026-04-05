# Developer Onboarding

Setup guide for new developers joining the Zazen Meditation Timer project.

## Prerequisites

### JDK 17 (Temurin)

Install Eclipse Temurin JDK 17:

```bash
# Ubuntu/Debian
sudo apt install -y wget
wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo apt-key add -
echo "deb https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update
sudo apt install -y temurin-17-jdk

# Verify
java -version
# openjdk version "17.x.x"
```

Set `JAVA_HOME` if not set automatically:
```bash
export JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64
```

### Android Studio

Download from [developer.android.com/studio](https://developer.android.com/studio).

Typical install locations on Linux:
- **Binary:** `/opt/android-studio/bin/studio.sh`
- **Desktop entry:** Created automatically on first launch
- **Config:** `~/.config/Google/AndroidStudio*`

Launch:
```bash
/opt/android-studio/bin/studio.sh
```

### Android SDK

Android Studio installs the SDK on first launch. Default location:

```
~/Android/Sdk
```

If installing without Android Studio, set `ANDROID_HOME`:
```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

**Required SDK components:**
- Android 14 (API 34) — Platform, SDK Platform Tools
- Android 10 (API 29) — System image `x86_64` (for emulator tests)
- Android 15 (API 35) — System image `x86_64` (optional, for local testing)
- Build Tools (latest)
- Android Emulator
- Android SDK Platform-Tools

Install via SDK Manager (`~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager`):
```bash
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
sdkmanager "system-images;android-29;default;x86_64"
sdkmanager "system-images;android-35;default;x86_64"
```

### Emulator (AVD)

Create an AVD for instrumented testing:

```bash
# Create AVD named "test_device" with API 29
avdmanager create avd -n test_device -k "system-images;android-29;default;x86_64" -d "Nexus 6"
```

Or via Android Studio: **Tools → Device Manager → Create Device → Nexus 6 → API 29 x86_64**.

For local testing with a newer image:
```bash
avdmanager create avd -n medium_phone_api35 -k "system-images;android-35;default;x86_64" -d "Medium Phone"
```

Start the emulator:
```bash
emulator -avd test_device &
# or
emulator -avd medium_phone_api35 &
```

## Clone and Build

```bash
git clone https://github.com/georgernstgraf/puregg.git
cd puregg
```

### `local.properties`

Create `local.properties` in the project root (gitignored, auto-created by Android Studio):
```properties
sdk.dir=/home/YOUR_USER/Android/Sdk
```

### Build

```bash
./gradlew build
```

This runs lint, compiles debug + release, and executes unit tests for both variants.

### Run Tests

Unit tests:
```bash
./gradlew test
```

Instrumented tests (requires running emulator or device):
```bash
./gradlew connectedDebugAndroidTest
```

### Install on Device

```bash
# Via ADB (device or emulator must be visible in `adb devices`)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Or run directly from Android Studio
```

## Project Structure

```
puregg/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/de/gaffga/android/           # Java source (60 files)
│   │   │   │   ├── fragments/                     # 7 Fragments + adapters
│   │   │   │   ├── zazentimer/                    # Core: Activity, DB, Service
│   │   │   │   │   ├── bo/                        # Business objects (Session, Section)
│   │   │   │   │   ├── audio/                     # Bell playback & volume
│   │   │   │   │   ├── service/                   # Foreground service & timer
│   │   │   │   │   ├── grpc/                      # JWT auth (prepared, unused)
│   │   │   │   │   └── views/                     # Custom TimerView
│   │   │   │   ├── base/                          # Utilities, settings
│   │   │   │   ├── mapping/                       # Reflection-based ORM
│   │   │   │   └── betterlist/                    # Custom drag-and-drop ListView
│   │   │   ├── res/                               # Layouts, drawables, values
│   │   │   └── AndroidManifest.xml
│   │   └── androidTest/                           # Espresso instrumented tests
│   └── build.gradle
├── docs/ai/                                       # AI agent documentation
├── gradle/wrapper/                                # Gradle wrapper
├── build.gradle                                  # Root build file (AGP 7.4.2)
├── settings.gradle                               # Single module: :app
└── gradle.properties                             # AndroidX, Jetifier, JDK 17
```

## Key Architecture Facts

- **1 Activity** (`ZazenTimerActivity`) hosts **7 Fragments** via manual `FragmentTransaction`
- **Foreground Service** (`MeditationService`) keeps meditation alive across screen-off
- **AlarmManager** (`setAlarmClock()`) triggers section transitions
- **Database:** Raw SQLite via custom `SQLiteOpenHelper` + reflection-based `DbMapper`
- **Language:** 100% Java, compiled with Java 17
- **No DI framework** — manual instantiation
- **No ViewModel/LiveData** — state held in Activity fields + Handler polling loop

## Git Workflow

**Trunk-based development.** All work commits directly to `main`. No branches, no PRs.

Commit message format references issue numbers:
```
feat: add backup/restore instrumented tests (#18)
fix: resolve startup crash (#8)
docs: update architecture documentation (#21)
```

## CI

GitHub Actions runs on every push:
- **Build job:** `./gradlew assembleDebug --no-daemon` on Ubuntu + JDK 17
- **Test job:** `./gradlew connectedDebugAndroidTest --no-daemon` on API 29 emulator

Check CI status after pushing:
```bash
gh run list --limit 3
gh run view <run-id>
```

## Documentation

Agent-oriented documentation lives in `docs/ai/`:
- `ONBOARDING.md` — this file
- `ARCHITECTURE.md` — structural map of the system
- `CONVENTIONS.md` — coding rules and patterns
- `DECISIONS.md` — chronological record of design choices
- `PITFALLS.md` — known gotchas and failure knowledge
- `STATE.md` — current project status
- `DOMAIN.md` — business rules and domain knowledge
- `HANDOFF.md` — open tasks for next session
- `UI_TEST_PLAN.md` — UI test coverage tracking

## Useful Commands

| Command | Purpose |
|---------|---------|
| `./gradlew build` | Full build + unit tests |
| `./gradlew assembleDebug` | Build debug APK only |
| `./gradlew test` | Run unit tests |
| `./gradlew connectedDebugAndroidTest` | Run instrumented tests |
| `./gradlew lint` | Run Android lint |
| `adb devices` | List connected devices/emulators |
| `adb install -r app/build/outputs/apk/debug/app-debug.apk` | Install debug APK |
| `gh run list --limit 3` | Check latest CI runs |
