# Project State

Current status as of 2026-05-09.

## Current Focus
Fixing remaining intermittent instrumentation test failures across all API levels.

## Completed (this cycle)
- [x] #88 Java → Kotlin migration — all 7 phases + 7 follow-ups complete
- [x] #126 Comprehensive unit & integration test suite (161 tests)
- [x] #132 Instrumentation script consolidation
- [x] Fixed 10+ instrumentation test failures on API 29 (navigation, selection, dialogs, DB races)
- [x] Enabled `orchestrator.failFast=true` for faster fix loops
- [x] Improved UI test stability with `contentDescription` and explicit waits in Page Objects

## Pending
- [ ] #64 Play Store — #114 (AAB build), #113 (privacy/legal)
- [ ] Fix intermittent `MeditationServiceTest` (service binding/button enabled race)
- [ ] Fix `SessionCrudTest.testUpdateSessionMetadata` (DB save race)
- [ ] Fix intermittent `SettingsTest` (PreferenceFragment scrolling)

## Known Issues
- Gradle UTP runner fails on API 35+ — workaround with `am instrument`
- Deep coroutine timing issues in some instrumented tests causing flakiness

## Blockers
- None

## Next Session Suggestion
1. Fix the last 3 intermittent test failures by implementing explicit state waiting (e.g. wait for button `isEnabled == true`).
2. Run full green run `scripts/run-instrumentation.sh` to trigger auto-tag.
3. Start #114 Switch build to AAB format.
