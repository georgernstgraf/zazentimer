# Handoff

Open tasks for next agent session.

## Active Issues

1. [ ] **#64 — Play Store**. Sub-issues #114 (AAB build) and #113 (privacy/legal) ready for implementation.

## Completed

- [x] **#88 — Java → Kotlin migration** (Epic). All 7 phases + 8 follow-ups completed 2026-05-09.
- [x] **#132 — Instrumentation script consolidation**. Completed 2026-05-09.
- [x] **#126 — Comprehensive unit & integration test suite** (157 tests). Completed 2026-05-09.

## Key Context
- **Zero Java files remain** — 100% Kotlin codebase
- **Zero ExecutorService/Thread.sleep/.get()** in production code — all coroutines
- **MeditationUiState is sealed class** (Idle/Running/Paused)
- **4 `!!` remain** — all safe `_binding!!` in fragments
- **ScreenRobot** delegation pattern replaces BasePage inheritance
- **enableEdgeToEdge()** called in Activity, activity-ktx added
- **Predictive back** enabled via manifest attribute
- **ktlint + detekt enforced** in CI (not just visibility)
- **explicitApiWarning()** enabled — switch to strict `explicitApi()` after all declarations annotated
- 2-stage pipeline: Stage 1 (commit gate, CI), Stage 2 (instrumented tests, `run-instrumentation.sh`)
- Tag-based releases: push `v*` tag → `release.yml` → AAB + Play Console
- **`run-instrumentation.sh` requires clean git** — commit or stash before running
- **`-target google_apis` removed** — flag removed in emulator 36.5.10
