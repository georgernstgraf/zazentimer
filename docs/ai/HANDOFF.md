1. [ ] #51 — Complete logcat correlation: rerun screen capture with full `adb logcat` (not PID-filtered) and document any warnings/exceptions per screen. The PID-filtered logcat captured nothing; the app's log tags don't match the package name filter.
2. [ ] Create bug issue — Duplicate Session crashes with `SQLiteConstraintException: UNIQUE constraint failed: sessions._id` in `DbOperations.duplicateSession()` at `DbOperations.java:77`. Root cause: insert copies the original `_id` instead of letting Room auto-generate.

Last updated: 2026-04-07.
