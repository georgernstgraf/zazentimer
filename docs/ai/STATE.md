# Project State

Current status as of 2026-05-07.

## Current Focus
No active focus. All nightly tests pass (API 29-35). Next: #64 Play Store sub-issues or #88 Kotlin migration.

## Completed (this cycle)
- [x] DbOperations single-thread executor deadlock fix — `duplicateSession()` now accesses DAOs directly inside `executeSync`
- [x] Restored essential `SystemClock.sleep()` for popup/fragment/PreferenceFragment timing (overzealous removal in #109)
- [x] SettingsPage uses `R.id.recycler_view` (not `android.R.id.list`) for AndroidX PreferenceFragmentCompat
- [x] Close soft keyboard before `pressBack()` in SessionCrudTest
- [x] Test scripts hardened: `resolve_avd()`, clean-state check, `dismiss_anr_dialog()`, SDK auto-detection
- [x] API 33+ use `am instrument` instead of Gradle UTP (focus issues)
- [x] `am instrument` retry on `RootViewWithoutFocusException`
- [x] All nightly tests pass (API 29-35)

## Completed (previous sessions)
- [x] #94 Upgrade Espresso to 3.7.0
- [x] #93 Four-stage test pipeline (headless filtering removed — Xvfb everywhere now)
- [x] #92 Backup WAL data loss fix
- [x] #82 Namespace refactor to at.priv.graf.zazentimer
- [x] #91 Hide session description when empty
- [x] #81 Session image visibility + grip handles
- [x] #90 About page line breaks
- [x] #86 Center plus icon on Add Section FAB
- [x] #87 System Theme option
- [x] #85 localeConfig for Android 13+
- [x] #84 MeditationService IdlingResource
- [x] #83 About page rewrite + retranslate tooling
- [x] #38 Full UI test plan automation
- [x] #80 Fixed 0-tests issue on API 36 CI

## Pending
- [ ] #64 Play Store — #114 (AAB build), #113 (privacy/legal)
- [ ] #88 Java → Kotlin migration (Epic)

## Known Issues
- Gradle UTP runner fails on API 35+ — workaround with `am instrument` documented in PITFALLS
- `testBellSoundPlayback` may still fail under Xvfb since `-noaudio` is retained

## Blockers
- None

## Next Session Suggestion
1. #114 Switch build to AAB format (needed for Play Store)
2. #113 Privacy policy + legal compliance
3. #88 Kotlin migration (long-term epic)
