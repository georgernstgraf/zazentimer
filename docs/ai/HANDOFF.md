# Handoff

## Current Branch
`main` (Trunk-based development)

## Open Tasks
1. [ ] **#183 — Full validation run**: Launched via `at now` at 17:46 CEST. Monitor with `ps aux | grep run-instrument` or `tail logs/instrumentation-2026-05-16.log`. Run `scripts/summarize-tests.sh --date 2026-05-16` when done. The `run-instrumentation.sh` script has 209 lines of unstaged changes (the rewrite) — these are committed now as `541901f` (summarize script only, the instrumentation script rewrite was already committed earlier).
2. [ ] **#64 — Play Store**: Sub-issues #114 (AAB build) and #113 (privacy/legal). Blocked by missing `PLAY_SERVICE_ACCOUNT_JSON` GitHub secret.

## AVD Inventory (all installed)
`test_api23` `test_api24` `test_api25` `test_api26` `test_api27` `test_api28` `test_api29` `test_api30` `test_api31` `test_api32` `test_api33` `test_api34` `test_api35` `test_api36`

## Known Test Failures (from previous partial runs)
- **API 31**: `SectionEditTest.testEditSectionConfig` — `keyDispatchingTimedOut`, 15/25 completed
- **API 36**: `DuplicateSessionTest.testDuplicateSessionDoesNotCrash` — UTP aborted, 3/25 completed
- **API 34**: PASS — 25/25

## Key Files This Session
- `scripts/summarize-tests.sh` — new, committed (`541901f`)
- `scripts/run-instrumentation.sh` — committed earlier
- `docs/ai/*` — updated with `at` scheduler knowledge

## How to Launch Long-Running Tests
```bash
echo "cd /home/georg/repos/georgernstgraf/zazentimer && scripts/run-instrumentation.sh --continue-on-error --ignore-dirty-git --debug >/dev/null 2>&1" | at now
```
Check: `atq` (queue), `ps aux | grep run-instrument` (running), `tail logs/instrumentation-$(date +%Y-%m-%d).log` (progress)

## CI Status
- Release AAB build step fails on all commits (pre-existing, related to #64 release pipeline)
- Unit tests and lint pass locally
