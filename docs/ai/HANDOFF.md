# Handoff

## Current Branch
`main` (Trunk-based development)

## Context
We have completed the core architectural refactoring of #137. State management is now handled via `MeditationRepository`, UI polling has been removed, and DB operations are synchronized with Espresso via `CountingIdlingResource`.

## Open Tasks
1. [ ] **Verify fixes for #135 and SessionCrudTest**: Run `scripts/run-instrumentation.sh` to confirm races are gone.
2. [ ] **#136 — Architecture: Modernize test infrastructure**: Modernize GMD/Fixtures.
3. [ ] **#133 — Downgrade minSdk**: Requires creating AVDs for API 21-28.
4. [ ] **#64 — Play Store**: Sub-issues #114 (AAB build) and #113 (privacy/legal).

## Success Criteria
- `scripts/run-instrumentation.sh` returns exit code 0 on a full run with real display.
- Tag `tested-YYYY-MM-DD` is automatically created and pushed.
