# Handoff

## Current Branch
`main` (Trunk-based development)

## Open Tasks
1. [ ] **#135 — Retry API 33 tests**: `systemd-oomd` kills emulator. Try `sudo systemctl stop systemd-oomd` before running, or reduce emulator memory to 1024. Run `scripts/run-instrumentation.sh --api 33 --ignore-dirty-git`.
2. [ ] **#135 — Retry API 31 tests**: Unverified. Run `scripts/run-instrumentation.sh --api 31 --ignore-dirty-git`.
3. [ ] **#135 — Full matrix run**: After 33 and 31 pass, run `scripts/run-instrumentation.sh` for auto-tag.
4. [ ] **#133 — Downgrade minSdk to 23**: Requires creating AVDs for API 21-28 and resolving compat issues.
5. [ ] **#64 — Play Store**: Sub-issues #114 (AAB build) and #113 (privacy/legal).

## Success Criteria
- `scripts/run-instrumentation.sh` returns exit code 0 on a full run with real display.
- Tag `tested-YYYY-MM-DD` is automatically created and pushed.
