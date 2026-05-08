# Handoff

Open tasks for next agent session.

## Active Issues

1. [ ] **#64 — Play Store**. Sub-issues #114 (AAB build) and #113 (privacy/legal) ready for implementation.

## Completed

- [x] **#88 — Java → Kotlin migration** (Epic). All 7 phases completed 2026-05-08.
  - #96: AGP 9.x + Gradle 9.x + Kotlin DSL
  - #97: ViewBinding migration
  - #98: KSP migration (Room + Hilt)
  - #99: Java → Kotlin conversion (41 files)
  - #100: Test conversion (21 files)
  - #101: SDK 34 → 36
  - #102: Final cleanup (ktlint + detekt + docs)

## Follow-up Issues (post-88)
- #103: Proper edge-to-edge (remove opt-out)
- #105: Idiomatic Kotlin refactorings
- #106: Coroutines migration
- #107: Predictive Back Gesture
- #108: Strict Kotlin compiler options + enable ktlint/detekt enforcement
- #110: styles.xml further cleanup (depends on #103)
- #111: Test infrastructure consolidation

## Key Context
- **All nightly tests pass (API 29-35)** after 7 bug fixes from the #104 deprecated API changes.
- **DbOperations `duplicateSession()` had a deadlock** — fixed by accessing DAOs directly inside `executeSync`.
- **API 33+ tests use `am instrument`** instead of Gradle UTP due to `RootViewWithoutFocusException`.
- **`am instrument` retry on focus errors** — API 33/34 intermittently lose window focus.
- **SettingsPage uses `R.id.recycler_view`** — AndroidX PreferenceFragmentCompat uses RecyclerView not ListView.
- 3-stage pipeline: Stage 1 (commit gate, local + GitHub Actions), Stage 2 (issue close gate, local with Xvfb), Stage 3 (nightly, VPS cron 02:00 UTC)
- Tag-based releases: push `v*` tag → `release.yml` builds AAB + uploads to Play Console
- VPS has Xvfb, KVM, all AVDs, and Android SDK installed
- **`run-nightly.sh` destroys uncommitted changes** — always commit before running it
- **Scripts use `resolve_avd()`** for portable AVD detection
- **`-target google_apis` removed** — flag removed in emulator 36.5.10
