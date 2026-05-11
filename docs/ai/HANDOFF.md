# Handoff

## Current Branch
`main` (Trunk-based development)

## Open Tasks
1. [ ] **#133 — Downgrade minSdk to 23**: Requires creating AVDs for API 21-28 and resolving compat issues.
2. [ ] **#64 — Play Store**: Sub-issues #114 (AAB build) and #113 (privacy/legal).

## Success Criteria
- `scripts/run-instrumentation.sh` returns exit code 0 on a full run with real display.
- Tag `tested-YYYY-MM-DD` is automatically created and pushed.
