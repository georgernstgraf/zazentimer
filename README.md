# Zazen Meditation Timer

A meditation timer for Android with customizable multi-section sessions, authentic bell sounds, and reliable background timing.

Originally created by Stefan Gaffga. Currently maintained by Georg Graf.

## Features

- **Custom sessions** — Build meditation sessions with multiple timed sections (sitting, walking, or any practice). Demo sessions are created on first launch to get you started.
- **8 authentic bell sounds** — Recorded from real Japanese rin bowls and a Tibetan singing bowl. Provided by Winfried of [meditationsuhren.de](http://www.meditationsuhren.de).
- **Per-section bell control** — Choose which bell rings, how many times, and at what interval for each section.
- **Per-session bell volume** — Adjust individual bell loudness per session through the bell dimming settings.
- **Custom bell import** — Import your own audio files as bell tones from the Manage Bells screen.
- **Drag-and-drop editing** — Long-press and drag to reorder sections; swipe to delete.
- **Animated arc timer** — A custom ring visualization shows section progress, elapsed/remaining time, and smooth section transitions.
- **Multiple time displays** — Tap the timer during meditation to cycle through: section elapsed, section remaining, session elapsed, session remaining.
- **Background timer** — Runs as a foreground service with a persistent notification. The timer keeps reliable time even when the screen is locked or you switch apps.
- **Pause and resume** — Pause your meditation and resume later from the notification or in-app controls.
- **Keep screen on** — Optionally keep the display on during meditation with adjustable brightness.
- **Light, dark, and system themes**.
- **Backup and restore** — Save your sessions and custom sounds to a ZIP file; restore them later.
- **No ads, no data collection, no internet required**.

## Usage

1. **Create a session** — Go to the Sessions tab and tap the + button. Give it a name and optional description.
2. **Add sections** — Tap a session to edit it. Add sections with a name, duration, bell sound, bell count, and bell interval.
3. **Reorder sections** — Long-press a section handle and drag it to reorder. Swipe right to delete.
4. **Start meditating** — Switch to the Meditation tab, select your session, and tap Start.
5. **During meditation** — Tap the timer to cycle time display modes. Use the Pause/Stop buttons or the notification to control the session.
6. **Manage bells** — Go to Settings → Manage Bells to import custom sound files or remove imported bells.
7. **Backup** — Use Settings → Create Backup to save your data, and Restore from Backup to reload it.

## Build & Run

**Package:** `at.priv.graf.zazentimer` | **Min SDK:** 23 | **Target SDK:** 36 | **Kotlin 2.3**

```bash
./gradlew build                              # Build debug APK
./gradlew test                               # Unit tests
./gradlew connectedDebugAndroidTest          # Instrumented tests (requires emulator/device)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

For detailed developer setup, see [docs/ai/ONBOARDING.md](docs/ai/ONBOARDING.md).

## Architecture

- **Single Activity** (`ZazenTimerActivity`) with multiple Fragments, navigated via Jetpack Navigation (NavHostFragment).
- **Foreground Service** (`MeditationService`) manages meditation lifecycle with a persistent notification.
- **ViewModel** (`MeditationViewModel`) holds meditation state and bridges the service with the UI.
- **AlarmManager** (`setAlarmClock`) triggers exact section-end timing via `SectionEndReceiver`.
- **Room database** (SQLite) with DAO pattern for sessions, sections, bells, and bell volumes.
- **Hilt** for dependency injection.
- **Custom `TimerView`** — An animated arc/ring drawn on Canvas with morphing section labels and a progress marker.
- **BackupManager** — Zips the database and custom bell files for backup/restore.
- **100% Kotlin**, targeting SDK 36 with full edge-to-edge support.

## Important Notes

### Silencing Your Phone

ZazenTimer does **not** change your phone's ringer mode or Do Not Disturb setting. Use your phone's built-in Do Not Disturb or Flight Mode before starting a session.

### Bell Volume

Bells play through the **alarm audio stream**. Adjust your phone's alarm volume to control overall loudness, then fine-tune individual bells within a session using the "Adjust Bell Volumes" option.

## License

See [LICENSE](LICENSE).
