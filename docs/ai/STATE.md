# Project State

Current status as of 2026-05-10.

## Current Focus
Test infrastructure modernization (#136) and state management refactoring (#137) are complete. Next: verify instrumentation tests pass with new architecture.

## Completed (this cycle)
- [x] #137 — Introduced `ZazenClock`, `MeditationRepository`, `CountingIdlingResource`; removed UI polling
- [x] #138 — Moved API levels to `gradle.properties`
- [x] #139 — Enabled `java-test-fixtures`, moved shared test utils to `src/testFixtures/`
- [x] #140 — Created `DevicePreFlightRule` in `HiltTestRunner.onStart()`
- [x] #141 — Updated `docs/ai/` with test infrastructure decisions and architecture map
- [x] #136 — Parent issue closed

## Pending
- [ ] #135 — Verify instrumentation test fixes
- [ ] #133 — Downgrade minSdk to 23
- [ ] #64 — Play Store release
- [ ] Pre-existing detekt violations (~30 issues across main source set)

## Blockers
None.

## Next Session Suggestion
Run `scripts/run-instrumentation.sh` to confirm the architectural refactoring (#137) and test infrastructure modernization (#136) have eliminated the races in `MeditationServiceTest` and `SessionCrudTest` (#135).
