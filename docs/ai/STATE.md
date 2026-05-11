# Project State

Current status as of 2026-05-11.

## Current Focus
#148 completed — added 38 new unit tests covering extracted service classes and Room migrations.

## Completed (this cycle)
- [x] #148 — Added test coverage for extracted services and Room migrations
  - MigrationTest.kt: 10 tests validating all Room migration paths (1→2→3→4→5)
  - WakeLockManagerTest.kt: 6 tests for WakeLock acquisition and preference gating
  - AlarmSchedulerTest.kt: 7 tests for alarm scheduling and cancellation
  - AudioStateManagerTest.kt: 8 tests for mute/unmute and external change detection
  - BellPlayerTest.kt: 7 tests for WakeLock lifecycle, onDone callback, null bell handling
- [x] #142 — Fixed all 337 detekt violations; `./gradlew detekt` exits 0; CI unblocked
- [x] #143 — Added POST_NOTIFICATIONS permission, edge-to-edge insets, exact alarm check
- [x] #144 — Added API 36 to `zazentimer.test.apis`, created AVDs for API 29-36
- [x] #135 — All 8 API levels (36,35,34,33,32,31,30,29) pass 24/24 instrumented tests
- [x] Extracted 10 helper classes: DemoSessionCreator, MigrationHelper, WakeLockManager, MeditationServiceState, EntityMapper, AudioStateManager, AlarmScheduler, BellPlayer, TimerAnimator/AnimationRunner

## Pending
- [ ] #133 — Downgrade minSdk to 23
- [ ] #64 — Play Store release

## Blockers
- None

## Next Session Suggestion
Proceed with #133 (minSdk downgrade to 23) or #64 (Play Store release). All 206 unit tests and full API matrix (29-36) pass.
