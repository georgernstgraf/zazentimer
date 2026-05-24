# Domain Knowledge

Business rules and domain relationships not obvious from code.

## Entities

### Session
- A named meditation configuration with a description.
- Contains one or more ordered Sections.
- Stored in `sessions` database table.
- Can be duplicated (deep copy of all sections).
- Default sessions are created on first app launch.

### Section
- A timed segment within a Session.
- Fields: name, duration (seconds), bell sound, bell count (1-5), bell pause/gap (1-15), volume (0-100), rank (ordering).
- Stored in `sections` database table with foreign key to session.
- Bell sound: either a built-in resource URI or a custom file URI (`bell_*` prefix in app filesDir).
- Duration displayed as MM:SS.
- Ordered by `rank` field within a session.
- **Default volume**: 50% (since 2026-05-18).

### Bell
- A sound played at section boundaries during meditation.
- 8 built-in bells loaded from `R.raw.*` (High Tone, Low Tone, 6 singing bowls).
- Custom bells: user-imported audio files stored in app's `filesDir` with `"bell_"` prefix.
- `BellCollection` singleton manages available bells.

## Meditation Lifecycle
1. User selects a session and presses Start.
2. `MeditationService` starts as a foreground service (notification visible).
3. `Meditation.start()` schedules first section via `AlarmManager.setAlarmClock()`.
4. When a section timer expires, `SectionEndReceiver` fires → `Meditation.onSectionEnd()`:
   - Plays bells for the completed section (count × pause gap via `PlayBellsAsync`).
   - Schedules the next section's alarm (or finishes if last section).
5. User can pause/resume (saves elapsed time, cancels/re-schedules alarm).
6. User can stop manually (calls `finishMeditation()`).
7. On finish: release WakeLock, stop foreground, send `ZAZENTIMER_SESSION_ENDED` broadcast, stopSelf.

## Volume System
- Bell audio always uses `AudioManager.STREAM_ALARM` (hard-coded, no channel selection).
- **System volume**: User controls alarm stream volume via Settings slider (or Android volume buttons). The app does not modify system stream volume during bell playback.
- **Per-session bell volume**: Each session defines volume per bell type (0-100 in DB, where 100=full loudness). The UI presents this as "Adjust Bell Volumes". Default is 50% for new sessions.
- **Legacy per-section dimming**: Older versions had a `volume` field (0-100) per section. These are migrated to per-session volumes.
- No master volume multiplier — the Settings "Bell Volume" slider directly controls the system alarm stream volume.

## Timer Display (TimerView)
- Custom circular arc showing session progress.
- 4 time display modes (toggled by tapping): section elapsed, section remaining, session elapsed, session remaining.
- Section transitions animate via morph (fade out old name, grow in new name).
- Ring segments colored: previous (dark), current (purple), next (teal), remaining (gray).

## Backup/Restore
- Backup: Uses SAF (`ACTION_CREATE_DOCUMENT`) to save a ZIP containing the SQLite database and app files (custom bells).
- Restore: Uses SAF (`ACTION_OPEN_DOCUMENT`) to read a ZIP, with confirmation dialog.
- Backup/restore runs via `AsyncTask` (deprecated, pending migration).

## Theme
- Light and dark themes available via `ListPreference` in settings.
- Changing theme restarts the Activity.

## Translation Voting Pipeline
- **Locale**: A language variant identified by BCP 47 tag (e.g., `pt-BR`), POSIX code (`pt_BR`), ISO 639-3 code (`por`), and optional Whisper response name. 123 locales in the app. ISO 639-3 is NOT unique per locale — por/srp/zho each cover multiple regional variants.
- **POSIX code format**: `language[_territory][@modifier]` (e.g., `sr@latin`, `pt_BR`). No uppercase rules — follows what `locale -a` on Linux produces.
- **BCP 47 vs POSIX**: BCP 47 uses hyphens (`pt-BR`), POSIX uses underscores (`pt_BR`). Some locales use POSIX modifier (`sr@latin` → BCP 47 `sr-Latn`). Legacy Android locale dirs (e.g., `values-in`, `values-iw`, `values-ji`) have been renamed to modern BCP 47 codes.
- **Master String**: An English string from `values/strings.xml` with a unique key. Some are `translatable="false"`. Multiple keys may share the same English text — each key is a separate master string. 154 unique strings from 174 total.
- **LLM Model**: A registered model from `llm_models` table (10 models from 6 providers: OpenAI, Anthropic, Google, Meta, Mistral, DeepSeek). Each model can produce translation candidates.
- **Vote**: A translation candidate submitted by an LLM, with a confidence score (1-5) enforced by SQL CHECK constraint. One vote per (master_string, locale, llm_model). Confidence is never "per-model quality" — it's per-translation-per-model.
- **Whisper**: Used for audio transcription during meditation, not for TTS. Whisper language map is static (100 languages). 32 of 123 locales have no Whisper support (whisper_response = null).
- **Confidence scale**: 1-5, Int, raw SQL CHECK. Not an enum — SQLite sorts enums alphabetically, making ordered queries meaningless.

## Screen Behavior During Meditation
- **Keep screen on** (`keep_screen_on` preference): Prevents screen timeout during meditation.
- **Brightness** (`brightness` preference): Sets screen brightness (0-100) during meditation, only when keep_screen_on is enabled.
