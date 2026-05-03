# Project State

Current status as of 2026-05-02.

## Current Focus
#85 and #89 completed. App now declares 136 locales via localeConfig. All rare languages covered.

## Completed (this session)
- [x] #85 localeConfig for Android 13+ per-app language support
  - Created `res/xml/locales_config.xml` with 128 locales (BCP 47 tags)
  - Added `values-fil/strings.xml` (copy of `values-tl` for Filipino)
  - Added `android:localeConfig="@xml/locales_config"` to AndroidManifest.xml
- [x] #89 Rare language support (sub-issue of #85)
  - Added 8 languages: Assamese, Kashmiri, Maithili, Dogri, Konkani, Santali, Dhivehi, Tibetan
  - Updated `retranslate.py` with `MyMemoryTranslator` fallback for 3 languages
  - Generated all 8 `values-XX/strings.xml` files (187 strings each)
  - Total locales now: 136

## Completed (previous sessions)
- [x] #84 MeditationService IdlingResource
- [x] #83 About page rewrite + retranslate tooling
- [x] #38 Full UI test plan automation
- [x] #80 Fixed 0-tests issue on API 36 CI
- [x] #67 Epic: Translate App into 127 OOBE languages

## Pending
- [ ] #82 Refactor namespace to `zazentimer.graf.priv.at`
- [ ] #81 Session image visibility + grip handles
- [ ] #64 Play Store
- [ ] SettingsTest.testRestore/testBackup CI failures (PreferenceFragmentCompat scroll issue)

## Known Issues
- Gradle UTP runner fails to discover tests on API 35+ emulators. Workaround: direct `am instrument`.
- SettingsTest.testRestore and testBackup fail on CI (API 29 and 36) — PreferenceFragmentCompat scroll not working in headless emulator
- SectionEditTest.testBellSoundPlayback and SessionCrudTest.testOpenEditSession fail on API 36 CI only

## Blockers
- None

## Next Session Suggestion
Consider #82 (namespace refactor), #81 (session card UI), or fixing the remaining CI test failures.
