# Project State

Current status as of 2026-05-12.

## Current Focus
#123 completed — Translation cleanup: 129 locales, expanded string coverage, consolidated directories.

## Completed (this cycle)
- [x] #149 — Fix retranslate.py reliability gaps (error handling, --locales, dry-run improvement)
- [x] #150 — Trim keep-english list (18→5), delete 31 abc_* strings (189→166 entries)
- [x] #151 — Externalize hardcoded strings (3 Kotlin locations + 5 nav_graph labels = 8 new R.string)
- [x] #153 — Consolidate 136→129 locale directories, update locales.json and locales_config.xml
- [x] #154 — Full retranslation for 129 locales (Google Translate + MyMemory)
- [x] #152 — Final validation, knowledge docs updated

## Pending
- [ ] #133 — Downgrade minSdk to 23
- [ ] #64 — Play Store release

## Blockers
- None

## Next Session Suggestion
Proceed with #133 (minSdk downgrade) or #64 (Play Store release). All 206 unit tests and full API matrix (29-36) pass.
