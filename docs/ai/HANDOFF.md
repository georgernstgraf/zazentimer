# Handoff

## Current Branch
`main` (Trunk-based development)

## Open Tasks
1. [ ] **#64 — Play Store**: Sub-issues #114 (AAB build) and #113 (privacy/legal). Blocked by missing `PLAY_SERVICE_ACCOUNT_JSON` GitHub secret.
2. [ ] **#164 — Missing translations**: 738 translations needed (6 new strings × 123 locales) for bell volume UI and dimming explanation. Use `scripts/retranslate.py --diff` or generate via LLM (Gemini 3.1 Pro preferred per issue description).

## Recently Completed
- ✅ **#163 — Backup version guard** (commit `4a085dd`): BackupManager reads Room DB version from SQLite header before restore. Shows AlertDialog instead of crashing on version mismatch.
- ✅ **#162 — Main screen dead state** (commits `bf2a11a`, `6ffc086`): TDD fix. Moved isRunning flag before suspend call in finishMeditation(). Regression test added.
- ✅ Debug build coexistence with `applicationIdSuffix = ".debug"`

## CI Status
- Release AAB build step fails on all commits (pre-existing, related to #64 release pipeline)
- Unit tests and lint pass locally

## Success Criteria
- `scripts/run-instrumentation.sh` returns exit code 0 on a full run with real display.
- Tag `tested-YYYY-MM-DD` is automatically created and pushed.
