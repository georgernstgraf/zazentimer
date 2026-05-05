# Project State

Current status as of 2026-05-06.

## Current Focus
#115 CI/CD pipeline overhaul — planning complete, sub-issues created and documented, ready for implementation.

## Completed (this cycle)
- [x] #64 Play Store issue updated with next-steps checklist (Developer Account verified)
- [x] #114 Sub-issue: Switch build to AAB format (linked to #64)
- [x] #113 Sub-issue: Privacy policy + legal compliance (linked to #64)
- [x] #115 Main issue: CI/CD pipeline overhaul (priority: high, 7 sub-issues)
  - [x] #122 Install/configure Xvfb on VPS (already installed!)
  - [x] #119 Create Stage 2 test script with $DISPLAY detection
  - [x] #118 Evaluate @RequiresDisplay obsolescence with Xvfb
  - [x] #117 Rewrite ci.yml: Stage 1 only
  - [x] #121 Create release.yml: tag-triggered AAB + Play Console
  - [x] #116 VPS cronjob for Stage 3 nightly (02:00 UTC)
  - [x] #120 Update project docs (blocked by implementation)
- [x] All sub-issues commented with decisions and sub-agent findings
- [x] Key decisions documented: 4096M RAM, -noaudio retained, versionCode from tag, no debug APK, 02:00 UTC nightly, docs after implementation

## Completed (previous sessions)
- [x] #94 Upgrade Espresso to 3.7.0
- [x] #93 Four-stage test pipeline with @RequiresDisplay + headless filtering
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
- [ ] #115 CI/CD pipeline overhaul — implementation (7 sub-issues)
- [ ] #64 Play Store — #114 (AAB build), #113 (privacy/legal)
- [ ] #88 Java → Kotlin migration (Epic)
- [ ] Unit test directory mismatch: `app/src/test/java/de/gaffga/.../audio/VolumeDimmingTest.java` still in old namespace path

## Known Issues
- Gradle UTP runner fails on API 35+ — workaround with `am instrument` documented in PITFALLS
- `PreferenceFragmentCompat` scrolling unreliable without display surface — Xvfb should fix this (pending evaluation in #118)
- `testBellSoundPlayback` may still fail under Xvfb since `-noaudio` is retained

## Blockers
- None

## Next Session Suggestion
Start implementing #115 sub-issues. Recommended order:
1. #117 (ci.yml rewrite) — quick win, removes 52-min CI
2. #122 (Xvfb — already done, just verify)
3. #119 (Stage 2 script with DISPLAY detection)
4. #118 (evaluate @RequiresDisplay under Xvfb)
5. #116 (VPS nightly cronjob)
6. #121 (release.yml — needs Play Console service account setup)
7. #120 (docs update — after all above are done)
