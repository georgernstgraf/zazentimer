# Project State

Current status as of 2026-05-17.

## Current Focus
#180 — Closed. Bells table (V7) with runtime repair for Lena's backup import. #183 — Completed (all APIs 24/24).

## Completed (this cycle)
- [x] #183 — `MainScreenDeadStateTest` fixed: `inRoot(isDialog())` for API 36 AlertDialog focus loss
- [x] #183 — Removed `am instrument` fallback path; all APIs now use `connectedDebugAndroidTest` (Gradle)
- [x] #183 — `run-instrumentation.sh` restructured: flat early-exit pattern, `stdbuf -oL` for pipe buffering
- [x] #183 — `summarize-tests.sh` fixed: fallback to `Finished N tests`/`OK (N tests)` when Gradle progress incomplete
- [x] #183 — Full matrix validation: APIs 23-36 all PASS with 24/24 instrumented tests
- [x] #180 — #187-#191: bells table V6→V7 migration, runtime repair, UI integration, 5 migration tests
- [x] #180 — Lena's backup import: stale bell URIs from old package automatically fixed at startup

## Pending
- [x] #64 — Play Store automation: Service Account connected, local `.venv` setup via `scripts/play_store/setup.sh`, scripts active in `scripts/play_store/`.

## Blockers
None

## Next Session Suggestion
Start work on #64 (Play Store) — needs `PLAY_SERVICE_ACCOUNT_JSON` GitHub secret.
Or run a full green run to create a `tested-YYYY-MM-DD` tag with the new V7 schema.
