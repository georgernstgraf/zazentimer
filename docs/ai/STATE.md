# Project State

Current status as of 2026-05-03.

## Current Focus
Namespace refactored to `at.priv.graf.zazentimer`. Docs updated to match.

## Completed (this cycle)
- [x] #82 Refactor namespace from `de.gaffga.android.zazentimer` to `at.priv.graf.zazentimer`
- [x] #91 Hide session description when empty, center name vertically
- [x] #81 Session image visibility — zen circle maintains minimum 40% of available space
- [x] #81 Grip handles added to session cards (`item_session.xml` + `ic_drag_handle.xml`)
- [x] #81 Session card drag reorder (long-press via `SessionTouchHelperCallback`)
- [x] #90 About page line breaks fixed (`\n` → `<br>` for `Html.fromHtml()`)
- [x] #86 Center plus icon on Add Section FAB
- [x] #87 System Theme option added as default

## Completed (previous sessions)
- [x] #85 localeConfig for Android 13+ per-app language support
- [x] #89 Rare language support (sub-issue of #85)
- [x] #84 MeditationService IdlingResource
- [x] #83 About page rewrite + retranslate tooling
- [x] #38 Full UI test plan automation
- [x] #80 Fixed 0-tests issue on API 36 CI
- [x] #67 Epic: Translate App into 127 OOBE languages

## Pending
- [ ] #64 Play Store
- [ ] SettingsTest.testRestore/testBackup CI failures (PreferenceFragmentCompat scroll issue)

## Known Issues
- Gradle UTP runner fails to discover tests on API 35+ emulators. Workaround: direct `am instrument`.
- SettingsTest.testRestore and testBackup fail on CI (API 29 and 36) — PreferenceFragmentCompat scroll not working in headless emulator
- SectionEditTest.testBellSoundPlayback and SessionCrudTest.testOpenEditSession fail on API 36 CI only

## Blockers
- None

## Next Session Suggestion
Consider #64 (Play Store) or fixing the remaining CI test failures.
