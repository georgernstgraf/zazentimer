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

### Bell
- A sound played at section boundaries during meditation.
- 8 built-in bells loaded from `R.raw.*` (High Tone, Low Tone, 6 singing bowls).
- Custom bells: user-imported audio files stored in app's `filesDir` with `"bell_"` prefix.
- `BellCollection` singleton manages available bells.

## Meditation Lifecycle
1. User selects a session and presses Start.
2. `MeditationService` starts as a foreground service (notification visible).
3. `Meditation.start()` mutes the phone per preferences, schedules first section via `AlarmManager.setAlarmClock()`.
4. When a section timer expires, `SectionEndReceiver` fires → `Meditation.onSectionEnd()`:
   - Plays bells for the completed section (count × pause gap via `PlayBellsAsync`).
   - Schedules the next section's alarm (or finishes if last section).
5. User can pause/resume (saves elapsed time, cancels/re-schedules alarm).
6. User can stop manually (calls `finishMeditation()`).
7. On finish: unmute phone, release WakeLock, stop foreground, send `ZAZENTIMER_SESSION_ENDED` broadcast, stopSelf.

## Mute Modes (mutually exclusive)
- **Vibrate + Sound** (`mute_mode_vibrate_sound`): Don't mute the phone during meditation.
- **Vibrate Only** (`mute_mode_vibrate`): Mute ringer, keep vibrate.
- **Silent** (`mute_mode_none`): Mute ringer and vibrate (default).

## Audio Output Channels (mutually exclusive)
- **Alarm Stream** (`pref_output_channel_alarm`, default): Uses `STREAM_ALARM` for bell playback.
- **Music Stream** (`pref_output_channel_music`): Uses `STREAM_MUSIC` for bell playback.

## Volume System
- Master volume preference (0-100) from SeekBarPreference.
- Per-section volume (0-100) from Section config.
- Effective volume = section volume × master volume / 100.
- `VolumeCalc` splits effective volume between system stream volume and MediaPlayer volume using logarithmic scaling.
- `Audio` sets the system stream volume to the minimum needed, then uses MediaPlayer volume for fine control.
- Original stream volumes are saved before meditation and restored after.

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

## Screen Behavior During Meditation
- **Keep screen on** (`keep_screen_on` preference): Prevents screen timeout during meditation.
- **Brightness** (`brightness` preference): Sets screen brightness (0-100) during meditation, only when keep_screen_on is enabled.
