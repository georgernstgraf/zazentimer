# Handoff

## Current Branch
`main` (Trunk-based development)

## Open Tasks
1. [ ] **#64 — Play Store**: Sub-issues #114 (AAB build) and #113 (privacy/legal). Blocked by missing `PLAY_SERVICE_ACCOUNT_JSON` GitHub secret.
2. [ ] **#164 — Missing translations**: 738 translations needed (6 new strings × 123 locales) for bell volume UI and dimming explanation. Use `scripts/retranslate.py --diff` or generate via LLM.

## Recently Completed
- ✅ **#165 — DND + Meditation refactor**: INTERRUPTION_FILTER_PRIORITY with alarm policy. AudioStateManager simplified. Meditation split into stopImmediate/finishAfterLastBell.
- ✅ **#169 — Short sections overlapping audio**: Wait for last gong to finish before ending session.
- ✅ **#163 — Backup version guard**: Reads Room DB version from SQLite header before restore.
- ✅ **#162 — Main screen dead state**: TDD fix.

## CI Status
- Release AAB build step fails on all commits (pre-existing, related to #64 release pipeline)
- Unit tests and lint pass locally

## Success Criteria
- `scripts/run-instrumentation.sh` returns exit code 0 on a full run with real display.
- Tag `tested-YYYY-MM-DD` is automatically created and pushed.
