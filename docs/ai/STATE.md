# Project State

Current status as of 2026-05-09.

## Current Focus
#88 Kotlin migration + all follow-ups complete. Next: #64 Play Store.

## Completed (this cycle)
- [x] #88 Java → Kotlin migration — all 7 phases + 7 follow-ups complete
  - #96: AGP 9.1.1 + Gradle 9.x + Kotlin DSL
  - #97: ViewBinding migration
  - #98: KSP migration (Room 2.8.4 + Hilt 2.59.2)
  - #99: Java → Kotlin conversion (41 files → .kt)
  - #100: Test conversion (21 files)
  - #101: SDK 34 → 36
  - #102: Final cleanup (ktlint 14.2.0 + detekt 1.23.8 + docs)
  - #103: Edge-to-edge (enableEdgeToEdge, activity-ktx)
  - #105: Idiomatic Kotlin (215 `!!` → 4, sealed class UiState, scope functions, interpolation)
  - #106: Coroutines (suspend DAOs, no ExecutorService/Thread.sleep remaining)
  - #107: Predictive back (enableOnBackInvokedCallback=true)
  - #108: Strict compiler options (explicitApiWarning, ktlint/detekt enforcement)
  - #110: Styles cleanup (themes.xml, 9 empty variants deleted, betterListView removed)
  - #111: Test infra (ScreenRobot delegation, dead code deleted, sleeps → IdlingResources)
- [x] #124 Fix unit test directory namespace mismatch
- [x] #126 Comprehensive unit & integration test suite (157 tests)
  - #127: Test infrastructure (MockK, Robolectric, room-testing, Truth, coroutines-test)
  - #128: Pure logic tests (36 tests)
  - #129: Room integration tests (34 tests)
  - #130: Logic extraction + tests (57 tests, 3 new production classes)
  - #131: Framework-dependent tests (34 tests)

## Completed (previous sessions)
- [x] #115 CI/CD pipeline overhaul (3-stage pipeline, tag-based releases)
- [x] DbOperations single-thread executor deadlock fix
- [x] All nightly tests pass (API 29-35)
- [x] #94 Upgrade Espresso to 3.7.0
- [x] #93 Four-stage test pipeline → now 3-stage with Xvfb
- [x] #92 Backup WAL data loss fix
- [x] #82 Namespace refactor to at.priv.graf.zazentimer
- [x] #38 Full UI test plan automation

## Pending
- [ ] #64 Play Store — #114 (AAB build), #113 (privacy/legal)

## Known Issues
- Gradle UTP runner fails on API 35+ — workaround with `am instrument`
- `testBellSoundPlayback` may still fail under Xvfb since `-noaudio` is retained
- 6 SystemClock.sleep() remain in instrumented tests per PITFALLS #81

## Blockers
- None

## Next Session Suggestion
1. #114 Switch build to AAB format (needed for Play Store)
2. #113 Privacy policy + legal compliance
3. Run nightly tests to verify coroutines migration on all API levels
