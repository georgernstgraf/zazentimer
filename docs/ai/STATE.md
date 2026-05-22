# Project State

Current status as of 2026-05-22.

## Current Focus
#202 — Multi-LLM translation pipeline: Prisma schema and Gradle integration done.

## Completed (this cycle)
- [x] #202 — Created `prisma/translations/schema.prisma` (4 models: locales, strings, translations, votes)
- [x] #202 — Added `prismaValidateTranslationsSchema` Gradle task
- [x] #202 — Updated `.gitignore` for translation DB files
- [x] #202 — Researched `opencode run --model` for multi-model dispatch
- [x] #204 — External song import: fixed auto-select, playback, and back-navigation crash
- [x] #205 — Added "Edit Section" to 3-dot overflow menu in session editor
- [x] #206 — Manage Bells settings screen with custom bell deletion

## Completed (previous cycles)
- [x] #201 — Fix tiny headings for system and individual bell volume in config dialog
- [x] #200 — Snapshot creation script for fast emulator boots
- [x] #200 — Emulator scripts refactored as sourceable libraries (removed ~270 lines duplication)
- [x] #200 — Hostname-based test matrix (claw/think/other)
- [x] #200 — `-noaudio` passed explicitly, emulator output correctly redirected to logfile
- [x] #199 — Session rank persistence via MainFragment.onPause()
- [x] #197 — Prisma integration for SQLite schema documentation and validation

## Pending
- [ ] #202 — Build `scripts/translation_votes.py` (DB CRUD CLI)
- [ ] #202 — Build `scripts/dispatch_translations.py` (opencode run orchestrator)
- [ ] #202 — Decide output directory handling, model selection, voting threshold
- [ ] #64 — Promotion/Upload automation: Fastlane-like upload script

## Blockers
None

## Next Session Suggestion
Continue #202: implement `scripts/translation_votes.py` and `scripts/dispatch_translations.py`.
