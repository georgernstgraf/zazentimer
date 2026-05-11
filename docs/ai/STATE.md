# Project State

Current status as of 2026-05-11.

## Current Focus
#135 completed — all 8 API levels (36→29) pass 24/24 instrumented tests. Auto-tag `tested-2026-05-11` created and pushed.

## Completed (this cycle)
- [x] #142 — Fixed all 337 detekt violations; `./gradlew detekt` exits 0; CI unblocked
- [x] #143 — Added POST_NOTIFICATIONS permission, edge-to-edge insets, exact alarm check
- [x] #144 — Added API 36 to `zazentimer.test.apis`, created AVDs for API 29-36
- [x] #135 — All 8 API levels (36,35,34,33,32,31,30,29) pass 24/24 instrumented tests
- [x] Extracted 10 helper classes: DemoSessionCreator, MigrationHelper, WakeLockManager, MeditationServiceState, EntityMapper, AudioStateManager, AlarmScheduler, BellPlayer, TimerAnimator/AnimationRunner
- [x] Removed dead `Build.VERSION.SDK_INT < 23` code paths (minSdk=29)
- [x] Fixed MeditationViewModel LiveData declaration order (NPE on API 36)
- [x] Fixed MeditationViewModel serviceConnection null capture
- [x] Fixed resetDatabaseForTest() Dispatchers.Main deadlock
- [x] Fixed DevicePreFlightRule try-catch scope
- [x] Fixed run-instrumentation.sh: process crash detection, empty output, APK install retry, emulator cleanup, memory reduction
- [x] Fixed auto-tag bug: FAILED_APIS not cleared on retry success
- [x] Fixed TestDispatchersModule: missing kotlinx-coroutines-test androidTest dep, missing ZazenClock binding, Hilt @TestInstallIn requires replaces with all bindings re-provided

## Pending
- [ ] #133 — Downgrade minSdk to 23
- [ ] #64 — Play Store release

## Blockers
- None

## Next Session Suggestion
Proceed with #133 (minSdk downgrade to 23) or #64 (Play Store release). All test infrastructure is green and auto-tag is verified working.
