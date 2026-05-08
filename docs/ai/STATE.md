# Project State

Current status as of 2026-05-08.

## Current Focus
#88 Kotlin migration completed. Next: #64 Play Store or follow-up issues (#103–#111).

## Completed (this cycle)
- [x] #88 Java → Kotlin migration — all 7 phases complete
  - #96: AGP 9.1.1 + Gradle 9.x + Kotlin DSL
  - #97: ViewBinding migration
  - #98: KSP migration (Room 2.8.4 + Hilt 2.59.2)
  - #99: Java → Kotlin conversion (41 files → .kt)
  - #100: Test conversion (21 files)
  - #101: SDK 34 → 36
  - #102: Final cleanup (ktlint 14.2.0 + detekt 1.23.8 + docs)

## Completed (previous sessions)
- [x] DbOperations single-thread executor deadlock fix
- [x] Restored essential `SystemClock.sleep()` for popup/fragment/PreferenceFragment timing
- [x] SettingsPage uses `R.id.recycler_view` for AndroidX PreferenceFragmentCompat
- [x] Close soft keyboard before `pressBack()` in SessionCrudTest
- [x] Test scripts hardened: `resolve_avd()`, clean-state check, `dismiss_anr_dialog()`, SDK auto-detection
- [x] API 33+ use `am instrument` instead of Gradle UTP (focus issues)
- [x] `am instrument` retry on `RootViewWithoutFocusException`
- [x] All nightly tests pass (API 29-35)
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
- [ ] #103: Proper edge-to-edge (remove opt-out)
- [ ] #105: Idiomatic Kotlin refactorings
- [ ] #106: Coroutines migration
- [ ] #107: Predictive Back Gesture
- [ ] #108: Strict Kotlin compiler options + enable ktlint/detekt enforcement
- [ ] #110: styles.xml further cleanup (depends on #103)
- [ ] #111: Test infrastructure consolidation

## Known Issues
- Gradle UTP runner fails on API 35+ — workaround with `am instrument`
- `testBellSoundPlayback` may still fail under Xvfb since `-noaudio` is retained
- ktlint and detekt are visibility-only (continue-on-error in CI) — enforcement deferred to #108

## Blockers
- None

## Next Session Suggestion
1. #103 Proper edge-to-edge (remove opt-out)
2. #114 Switch build to AAB format (needed for Play Store)
3. #113 Privacy policy + legal compliance
