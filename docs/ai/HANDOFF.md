# Handoff

## Open Tasks

1. [ ] #11 — Device-test background timer reliability
   - Code changes are done and build passes. Need to verify on a physical device with a long meditation session (20+ min) while the app is backgrounded.
   - If gong fires reliably, close the issue.
   - Consider whether the existing screen-on warning dialog (`ZazenTimerActivity.doStartMediation()` ~line 360) is still needed with `setAlarmClock()`.

## Current Branch
`main` (up to date with `origin/main`, uncommitted changes for #11)

## Key Files for Next Session
- `app/src/main/java/de/gaffga/android/zazentimer/service/Meditation.java` — timer core, `startSectionTimer()` at line 169
- `app/src/main/java/de/gaffga/android/zazentimer/service/MeditationService.java` — `onStartCommand()` at line 38
- `app/src/main/java/de/gaffga/android/zazentimer/service/SectionEndReceiver.java` — static alarm receiver
