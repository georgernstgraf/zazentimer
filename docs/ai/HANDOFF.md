# Handoff

Open tasks for next agent session.

## Active Issues

1. [ ] **#64 — Play Store**. Sub-issues #114 (AAB build) and #113 (privacy/legal) ready for implementation. Play Console account is verified. Service account setup needed for `release.yml`.

2. [ ] **#88 — Java → Kotlin migration** (Epic). Long-term effort, no immediate blockers.

## Key Context
- **All nightly tests pass (API 29-35)** after 7 bug fixes from the #104 deprecated API changes.
- **DbOperations `duplicateSession()` had a deadlock** — fixed by accessing DAOs directly inside `executeSync`.
- **API 33+ tests use `am instrument`** instead of Gradle UTP due to `RootViewWithoutFocusException`.
- **`am instrument` retry on focus errors** — API 33/34 intermittently lose window focus.
- **SettingsPage uses `R.id.recycler_view`** — AndroidX PreferenceFragmentCompat uses RecyclerView not ListView.
- **#104 Deprecated API fixes are COMPLETE.** All 6 fixes applied and verified.
- **#115 CI/CD pipeline overhaul is COMPLETE.** All 7 sub-issues (#116–#122) implemented.
- 3-stage pipeline: Stage 1 (commit gate, local + GitHub Actions), Stage 2 (issue close gate, local with Xvfb), Stage 3 (nightly, VPS cron 02:00 UTC)
- Tag-based releases: push `v*` tag → `release.yml` builds AAB + uploads to Play Console
- GitHub Actions now runs only 2 jobs: `build` (AAB) + `unit-tests` (~4 min)
- VPS has Xvfb, KVM, all AVDs, and Android SDK installed
- `ANDROID_HOME` and `ANDROID_SDK_ROOT` now set in `~/.profile` AND in test scripts
- **`run-nightly.sh` destroys uncommitted changes** — always commit before running it
- **Scripts now use `resolve_avd()` for portable AVD detection** — works on both VPS (`test_apiN`) and dev machines (`Medium_Phone_API_N`)
- **`@RequiresDisplay` / headless filtering removed** — Xvfb everywhere makes it unnecessary
- **`-target google_apis` removed from all emulator commands** — flag removed in emulator 36.5.10

## Decisions Made
All decisions documented in DECISIONS.md. Key recent ones:
- Three-stage pipeline with local gates (DECISIONS: #115)
- Tag-based releases for Play Store (DECISIONS: #115)
- Xvfb for headless instrumented tests on VPS (DECISIONS: #115)
