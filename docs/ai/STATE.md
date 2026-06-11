# Project State

Current status as of 2026-06-11.

## Current Focus
Bug fixes — session drag-reorder persistence (#244)

## Completed (this cycle)
- [x] #244: Session rank persistence — save ranks before reloading in suspendUpdateSessionList()
- [x] #242: F-Droid pipeline green — static version, fastlane, subdir: app, autoupdate, tag fix
- [x] #241: Resilient backup restore with WAL/SHM deletion + sanitizeBellUris()
- [x] #239/#236: Empty session fix (auto-create first section)
- [x] #238: Room DB version on About screen
- [x] #240: Global crash handler with CrashActivity

## Pending
- [ ] F-Droid MR !39945 — await maintainer review
- [ ] Play Store release (first production deploy)

## Blockers
- None

## Next Session Suggestion
- Check F-Droid MR !39945 status — if merged, tag v3.0.7 for Play Store too
