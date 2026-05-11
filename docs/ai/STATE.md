# Project State

Current status as of 2026-05-11.

## Current Focus
Detekt violations fixed (#142), POST_NOTIFICATIONS + API 36 compatibility added (#143), API 36 integrated into test matrix (#144). Instrumented tests verified passing on 6 of 8 API levels (#135).

## Completed (this cycle)
- [x] #142 — Fixed all 337 detekt violations; `./gradlew detekt` exits 0; CI unblocked
- [x] #143 — Added POST_NOTIFICATIONS permission, edge-to-edge insets, exact alarm check
- [x] #144 — Added API 36 to `zazentimer.test.apis`, created AVDs for API 29-36
- [x] #135 — Instrumented tests verified PASS on 6 of 8 API levels (36,35,34,32,30,29)
- [x] Extracted 10 helper classes: DemoSessionCreator, MigrationHelper, WakeLockManager, MeditationServiceState, EntityMapper, AudioStateManager, AlarmScheduler, BellPlayer, TimerAnimator/AnimationRunner
- [x] Removed dead `Build.VERSION.SDK_INT < 23` code paths (minSdk=29)
- [x] Fixed MeditationViewModel LiveData declaration order (NPE on API 36)
- [x] Fixed MeditationViewModel serviceConnection null capture
- [x] Fixed resetDatabaseForTest() Dispatchers.Main deadlock
- [x] Fixed DevicePreFlightRule try-catch scope
- [x] Fixed run-instrumentation.sh: process crash detection, empty output, APK install retry, emulator cleanup, memory reduction

## Pending
- [ ] #135 — Retry instrumented tests on API 33 (systemd-oomd kills emulator) and API 31 (unverified)
- [ ] #133 — Downgrade minSdk to 23
- [ ] #64 — Play Store release

## Blockers
- API 33: `systemd-oomd` kills emulator under memory pressure on this host
- API 31: unverified (agent timed out)

## Next Session Suggestion
Retry API 33 and 31 test runs. If systemd-oomd persists, temporarily disable it or reduce emulator memory further. Then run full matrix for auto-tag.
