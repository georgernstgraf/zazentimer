# Handoff

Open tasks for next agent session.

## Active Issues

1. [ ] **#115 — CI/CD pipeline overhaul** (priority: high). Main issue with 7 sub-issues. Planning is complete, all decisions documented in issue comments. Start with #117 (ci.yml rewrite) for quick impact.

2. [ ] **#64 — Play Store**. Sub-issues #114 (AAB build) and #113 (privacy/legal) ready for implementation. Play Console account is verified. Service account setup needed for #121.

## Key Context
- VPS has Xvfb, KVM, all AVDs, and Android SDK already installed
- ANDROID_HOME is NOT set — scripts must set it explicitly
- VPS RAM is 3.8 GB physical + 8 GB swap — emulators run with 4096M using swap
- API 35+ requires `am instrument` workaround (UTP bug)
- `versionCode`/`versionName` are static in both `build.gradle` AND `AndroidManifest.xml` — AndroidManifest entries should be removed, build.gradle should read from Gradle properties
- Nightly cron at 02:00 UTC (not 03:00 — conflict with openclaw sync on Sundays)
- Docs (#120) must be updated AFTER implementation is complete

## Decisions Made
All decisions documented in issue comments on #115-#122. See DECISIONS.md for the three architectural decisions recorded.
