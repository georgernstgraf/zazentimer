# Handoff

## Current Branch
`main` (Trunk-based development)

## Context
Issues #137 (State Management) and #136 (Test Infrastructure Modernization) are complete. ktlint, compile, and unit tests all pass. Pre-existing detekt violations in `main` source set remain unresolved but are unrelated to our changes.

## Open Tasks
1. [ ] **#135 — Fix remaining test races**: Verify `MeditationServiceTest` and `SessionCrudTest` pass with new architecture. Run `scripts/run-instrumentation.sh`.
2. [ ] **#133 — Downgrade minSdk to 23**: Requires creating AVDs for API 21-28 and resolving compat issues.
3. [ ] **#64 — Play Store**: Sub-issues #114 (AAB build) and #113 (privacy/legal).
4. [ ] **Pre-existing detekt violations**: ~30 detekt issues across `main` source set (`DbOperations`, `Meditation`, `TimerView`, etc.) cause CI failure. Create a dedicated issue for this.

## Success Criteria
- `scripts/run-instrumentation.sh` returns exit code 0 on a full run with real display.
- Tag `tested-YYYY-MM-DD` is automatically created and pushed.
