# Project State

Current status as of 2026-06-02.

## Current Focus
Fix backup corruption and cross-device custom-bell restore (#237)

## Completed (this cycle)
- [x] #237: Backup filter — only include `bell_*` files from `filesDir`, exclude stale artifacts (`zentimer`, `profile*`)
- [x] #237: `BellCollection.initialize()` after restore so Section Edit spinner picks up restored custom bells
- [x] #237: `selectBell()` fallback to filename-suffix matching when exact URI fails (cross-device restore)
- [x] #237: `openOutputStream(uri, "wt")` truncation fix — prevents ZIP corruption on overwrite
- [x] #237: Test updates — filenames prefixed `bell_` to match new backup filter
- [x] #237: Analyzed backup `tmp/blah.zip` — confirmed internal structure, identified stale `zentimer` DB and profile artifacts

## Pending
- [ ] #64 — Play Store
- [ ] #202 — Translation Epic (runs in progress)

## Blockers
None

## Next Session Suggestion
Play Store release, or continue translation pipeline auto-resolution.
