# Project State

Current status as of 2026-04-30.

## Current Focus
Issue #80 completed. CI `test-max` job now runs on API 36 using direct `adb shell am instrument` instead of Gradle UTP.

## Completed (this session)
- [x] #80 Fixed 0-tests issue on API 36/35 CI test-max job
  - Root cause: Gradle UTP (Unified Test Platform) fails to discover tests on API 35+
  - Fix: Use `adb shell am instrument -w -r` directly for test-max job
  - Requires `target: google_apis` on emulator system image
  - All 6 tests pass on API 36
- [x] Updated PITFALLS.md with precise UTP vs orchestrator distinction
- [x] Updated DECISIONS.md with test-max CI approach decision

## Completed (previous sessions)
- [x] #67 Epic: Translate App into 206 OOBE languages
- [x] Enabled `-Xlint:deprecation` — 14 warnings fixed across 10 files
- [x] Instrumentation tests: 6 tests pass on API 29, 34, 35, 36
- [x] Section list UI enhancements, volume control, audio normalization
- [x] Multiple bug fixes (#55, #56, #57, #58, #60)

## Pending
- [ ] #51 (remaining) Logcat correlation with screen navigation, full log capture per screen

## Known Issues
- Gradle UTP runner fails to discover tests on API 35+ emulators (any execution mode). Workaround: direct `am instrument`.
- API 35+ requires `target: google_apis` emulator image (default image has missing components).

## Blockers
- None

## Next Session Suggestion
Continue with #51 logcat documentation.
