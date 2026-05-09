# Handoff

Open tasks for next agent session.

## Active Issues

1. [ ] **#64 — Play Store**. Sub-issues #114 (AAB build) and #113 (privacy/legal) ready for implementation.

## Completed

- [x] **#88 — Java → Kotlin migration** (Epic). All 7 phases completed 2026-05-08.
- [x] **#126 — Comprehensive unit & integration test suite** (161 tests). Completed 2026-05-09.

## Follow-up Issues (post-88)
- #103: Proper edge-to-edge (remove opt-out)
- #105: Idiomatic Kotlin refactorings
- #106: Coroutines migration
- #107: Predictive Back Gesture
- #108: Strict Kotlin compiler options + enable ktlint/detekt enforcement
- #110: styles.xml further cleanup (depends on #103)
- #111: Test infrastructure consolidation

## Key Context
- **161 unit/integration tests** across 12 test files (was 1 file / 7 tests before #126)
- **3 extracted production classes**: MeditationTimer, SectionArcCalculator, BackupManager
- Test deps: MockK 1.14.4, Robolectric 4.14.1, room-testing 2.8.4, Truth 1.4.4, coroutines-test 1.10.2
- DbOperations companion `toEntity`/`toBo` are private — tests use reflection
- `exportSchema=false` prevents Room migration tests — need to enable
- Audio MediaPlayer created inline — tests use `mockkConstructor`
- 3-stage pipeline: Stage 1 (commit gate), Stage 2 (issue close gate, Xvfb), Stage 3 (nightly 02:00 UTC)
- Tag-based releases: push `v*` tag → `release.yml` → AAB + Play Console
- **`run-nightly.sh` destroys uncommitted changes** — always commit before running it
- **Scripts use `resolve_avd()`** for portable AVD detection
- **`-target google_apis` removed** — flag removed in emulator 36.5.10
