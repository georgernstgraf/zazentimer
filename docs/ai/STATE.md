# Project State

Current status as of 2026-05-05.

## Current Focus
#95 Stage 4 (full instrumented tests) passed on all API levels 29–35 locally. Ready to close.

## Completed (this cycle)
- [x] #95 Stage 4 full test pass: 26/26 tests green on API 29, 30, 31, 32, 33, 34, 35 (local display emulator)
- [x] #95 Installed system images + AVDs for API 30–34 (`google_apis;x86_64`)
- [x] #95 CI verified green on all 9 jobs (build, unit-tests, test-29 through test-35)
- [x] #94 Upgrade Espresso to 3.7.0, fix testRestore scrolling robustness
- [x] #93 Four-stage test pipeline with @RequiresDisplay + headless filtering
- [x] #92 Backup WAL data loss — close DB before copy, reopen after (all exit paths)

## Completed (previous sessions)
- [x] #82 Refactor namespace from `de.gaffga.android.zazentimer` to `at.priv.graf.zazentimer`
- [x] #91 Hide session description when empty, center name vertically
- [x] #81 Session image visibility — zen circle maintains minimum 40% of available space
- [x] #81 Grip handles added to session cards + drag reorder
- [x] #90 About page line breaks fixed
- [x] #86 Center plus icon on Add Section FAB
- [x] #87 System Theme option added as default
- [x] #85 localeConfig for Android 13+ per-app language support
- [x] #84 MeditationService IdlingResource
- [x] #83 About page rewrite + retranslate tooling
- [x] #38 Full UI test plan automation
- [x] #80 Fixed 0-tests issue on API 36 CI

## Pending
- [ ] #64 Play Store
- [ ] #88 Java → Kotlin migration (Epic)
- [ ] Unit test directory mismatch: `app/src/test/java/de/gaffga/.../audio/VolumeDimmingTest.java` still in old namespace path

## Known Issues
- Gradle UTP runner fails to discover tests on API 35+ — worked around with `am instrument` in CI
- `PreferenceFragmentCompat` scrolling unreliable in headless emulators (PITFALLS #51) — `@RequiresDisplay` annotation applied to affected tests

## Blockers
- None

## Next Session Suggestion
Consider #64 (Play Store) or #88 (Kotlin migration). Fix unit test directory mismatch at some point.
