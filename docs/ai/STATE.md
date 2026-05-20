# Project State

Current status as of 2026-05-20.

## Current Focus
Fixing dialog headings size/styling (#201).

## Completed (this cycle)
- [x] #201 — Fix tiny headings for system and individual bell volume in config dialog
- [x] #200 — Snapshot creation script for fast emulator boots
- [x] #200 — Emulator scripts refactored as sourceable libraries (removed ~270 lines duplication)
- [x] #200 — Hostname-based test matrix (claw/think/other)
- [x] #200 — `-noaudio` passed explicitly, emulator output correctly redirected to logfile
- [x] #199 — Session rank persistence via MainFragment.onPause()

## Pending
- [ ] #64 — Promotion/Upload automation: Fastlane-like upload script
- [ ] #197 — Migrate from Room to Prisma (large refactoring)

## Blockers
None

## Next Session Suggestion
Push `v*` tag to trigger release workflow. Then #64 or #197.
