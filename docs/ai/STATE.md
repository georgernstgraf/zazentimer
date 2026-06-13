# Project State

Current status as of 2026-06-13.

## Current Focus
Decoupling timer logic and implementing pure Kotlin fakes to fix test hangs and memory leaks.

## Completed (this cycle)
- [x] #252: Extract `BellPlayer`, `MeditationRepository`, and `AlarmScheduler` interfaces to decouple Meditation from Android components and fix memory leaks (OOM) during testing.
- [x] #252: Add `runCurrent()` in tests to resolve coroutine-timing hangs.
- [x] #252: Implement comprehensive, 100% MockK-free `MeditationTest.kt` using pure Kotlin fakes.

## Pending
- [ ] F-Droid MR !39945 — await maintainer review
- [ ] Play Store release (first production deploy)

## Blockers
- None

## Next Session Suggestion
- Await F-Droid merge or move forward with Play Store production release.
