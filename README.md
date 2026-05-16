# Zazen Meditation Timer

A meditation timer for Android with authentic bell sounds, customizable sessions, and reliable background timing.

Originally created by Stefan Gaffga. Currently maintained by Georg Graf. Open source on [GitHub](https://github.com/georgernstgraf/zazentimer).

---

## Features

- **Custom sessions** — Build meditation sessions with multiple timed sections (e.g. Zazen, Kinhin, or any sitting practice)
- **8 authentic bell sounds** — Real Japanese rin bowls and a Tibetan singing bowl, recorded from actual instruments
- **Per-bell volume control** — Adjust the loudness of each bell sound individually
- **Custom bell import** — Add your own sound files as bell tones
- **Drag-and-drop editing** — Reorder sections by long-pressing and dragging; swipe to delete
- **Background timer** — The timer keeps running reliably even when you lock your screen or switch apps
- **Keep screen on** — Optionally keep the display on during meditation, with adjustable brightness
- **Light, dark, and system themes**
- **Backup and restore** — Save your sessions and custom sounds to a file, and restore them later
- **No ads, no data collection, no internet required**

## Important Notes

### Silencing Your Phone

ZazenTimer does **not** change your phone's ringer mode or Do Not Disturb setting. To silence incoming calls and notifications during meditation, use your phone's built-in **Do Not Disturb** or **Flight Mode** before starting a session.

### Bell Volume

Bell volume is set **per gong type** (session-wide), not per individual section. To make bells louder or quieter overall, adjust your phone's **alarm volume** — ZazenTimer plays bells through the alarm audio stream.

You can also fine-tune individual bell loudness within the app using the "Adjust Bell Volumes" option in section settings.

## How to Use

1. **Create a session** — Go to the Sessions tab and tap the + button
2. **Add sections** — Name each section (e.g. "Zazen 1", "Kinhin"), set its duration, choose a bell sound, and configure how many times the bell rings
3. **Reorder sections** — Long-press a section and drag it to the desired position
4. **Start meditating** — Switch to the Meditation tab, select your session, and tap "Start Meditation"
5. **Pause or stop** — Use the controls on screen or the notification to pause or end your session early

## Bell Sounds

| Bell | Description |
|------|-------------|
| High Tone | High-pitched ring |
| Low Tone | Deep, resonant ring |
| Jap. Rhinbowl Dharma, 107mm | Medium Japanese rin bowl |
| Jap. Rhinbowl Dharma, 88mm (black, glazed) | Smaller Japanese rin bowl |
| Jap. Rhinbowl Shomyo, 90mm | Japanese rin bowl |
| Jap. Rhinbowl Tang, 164mm | Large Japanese rin bowl |
| Tibetan Bowl, 230mm | Tibetan singing bowl |
| Jap. Rhinbowl, 97mm | Compact Japanese rin bowl |

Bell sounds are provided by Winfried of [meditationsuhren.de](http://www.meditationsuhren.de).

## For Developers

**Package:** `at.priv.graf.zazentimer` | **Min SDK:** 29 | **Target SDK:** 34 | **Java 17**

```bash
./gradlew build                        # Build
./gradlew test                         # Unit tests
./gradlew connectedDebugAndroidTest    # Instrumented tests (requires emulator)
adb install -r app/build/outputs/apk/debug/app-debug.apk  # Install
```

For full setup instructions, see [docs/ai/ONBOARDING.md](docs/ai/ONBOARDING.md).

### Architecture

- 1 Activity + 7 Fragments (manual Fragment transactions)
- Foreground Service for meditation lifecycle
- AlarmManager (`setAlarmClock`) for exact section-end timing
- Raw SQLite with reflection-based ORM
- 100% Java, no DI framework

### Git Workflow

Trunk-based development. Commit directly to `main`. No branches, no PRs. Push a `v*` tag to trigger release.

### Documentation

Project documentation lives in `docs/ai/`:

| File | Purpose |
|------|---------|
| [ONBOARDING.md](docs/ai/ONBOARDING.md) | Developer setup guide |
| [ARCHITECTURE.md](docs/ai/ARCHITECTURE.md) | Structural map of the system |
| [CONVENTIONS.md](docs/ai/CONVENTIONS.md) | Coding rules and patterns |
| [DOMAIN.md](docs/ai/DOMAIN.md) | Business/domain knowledge |
| [PITFALLS.md](docs/ai/PITFALLS.md) | Known gotchas |
