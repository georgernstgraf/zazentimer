# Project State

Current status as of 2026-05-05.

## Current Focus
Four-stage test pipeline implemented (#93, #94). CI verification pending.

## Completed (this cycle)
- [x] #94 Upgrade Espresso to 3.7.0, fix testRestore scrolling robustness
- [x] #93 Create `@RequiresDisplay` annotation + HiltTestRunner headless filtering
- [x] #93 Annotate display-required tests (testBackup, testRestore, testBellSoundPlayback)
- [x] #93 Rework CI to 4-stage pipeline (build / unit-tests / instrumented-headless / full-local)
- [x] #93 Fix API 35 CI: use `am instrument` instead of Gradle UTP + wake-up commands
- [x] Updated knowledge files (DECISIONS, CONVENTIONS, STATE, HANDOFF)

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
- [ ] Verify CI passes after #93/#94 pipeline changes (check `gh run list --limit 3`)
- [ ] Close #94 and #93 once CI is green

## Known Issues
- Gradle UTP runner fails to discover tests on API 35+ — worked around with `am instrument` in CI
- `PreferenceFragmentCompat` scrolling unreliable in headless emulators (PITFALLS #51) — `@RequiresDisplay` annotation applied to affected tests
- Unit test directory mismatch: `app/src/test/java/de/gaffga/.../audio/VolumeDimmingTest.java` declares package `at.priv.graf.zazentimer.audio` but file is still in old directory path

## Blockers
- None

## Next Session Suggestion
Verify CI is green for #93/#94. If green, close both issues. Then consider #64 (Play Store) or fixing the unit test directory mismatch.
