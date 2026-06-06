# Project State

Current status as of 2026-06-06.

## Current Focus
Global crash handler: catching uncaught exceptions via UncaughtExceptionHandler + CrashActivity pop-up (#240)

## Completed (this cycle)
- [x] #237: Backup filter, BellCollection refresh, selectBell URI fallback, ZIP truncation fix
- [x] #202: Closed — Python scripts obsolete, Deno/TS voting pipeline replaces original plan
- [x] #234: Closed — stale bell list fix already implemented
- [x] #64: Updated description — accurate checklist of Play Store readiness
- [x] #238: Room DB version on About screen — reads actual `PRAGMA user_version` at runtime via `DbOperations.getActualDatabaseVersion()`
- [x] #236: New session pre-fills name/description + auto-creates first section — no more empty session bug
- [x] Global UncaughtExceptionHandler in ZazenTimerApplication — intercepts all uncaught exceptions (#240)
- [x] CrashActivity — AlertDialog pop-up in separate process showing exception type, message, and first 10 stack frames (#240)
- [x] Copy Stack Trace & Exit button — copies full trace to clipboard, then exits (#240)
- [x] Exit button — dismisses dialog and terminates app (#240)

## Pending
- [ ] #64 — Play Store (promote alpha → production after testing completes)

## Blockers
None

## Next Session Suggestion
Play Store release, or continue translation pipeline auto-resolution.
