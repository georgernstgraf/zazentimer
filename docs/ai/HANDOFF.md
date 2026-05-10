# Handoff

## Current Branch
`main` (Trunk-based development)

## Context
We are in the middle of an iterative fix loop for instrumentation tests. Most API 29 failures are resolved. Remaining failures are due to subtle coroutine/async races that simple sleeps haven't fully eliminated.

## Open Tasks
1. [ ] **#136 — Architecture: Modernize test infrastructure**: Modernize GMD/Fixtures.
2. [ ] **#137 — Architecture: Enhance Testability**: Fix polling/races at source.
3. [ ] **Fix `MeditationServiceTest` flakiness**: The test sometimes fails because it clicks the "Stop" button before the service has bound and the fragment has transitioned to the "Running" state.
4. [ ] **Fix `SessionCrudTest.testUpdateSessionMetadata` race**: Async DB save/read race between Fragment transitions.
5. [ ] **#133 — Downgrade minSdk**: Requires creating AVDs for API 21-28.
6. [ ] **#64 — Play Store**: Sub-issues #114 (AAB build) and #113 (privacy/legal).

## Success Criteria
- `scripts/run-instrumentation.sh` returns exit code 0 on a full run with real display.
- Tag `tested-YYYY-MM-DD` is automatically created and pushed.
