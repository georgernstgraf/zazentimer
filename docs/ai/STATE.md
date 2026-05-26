# Project State

Current status as of 2026-05-26.

## Current Focus
#232 — Translate pipeline improvements: session timeout, stall retries, log analyzer

## Completed (this cycle)
- [x] #219 — Include null-vote strings in translate skip set
- [x] #220 — Fix voting backend comparison timeout (WAL checkpoint + duplicate query)
- [x] #222 — Timestamp format, proficiency threshold skip, log cleanup
- [x] #229 — WAL checkpoint after each translate batch
- [x] #230 — Voting backend overhaul: model stats table, language eval view, tooltips, detail links
- [x] #231 — Explicit FK columns for language_proficiencies (remove implicit M:N junction tables)
- [x] #232 — 60min session timeout + stall retry + log analyzer script

## Pending
- [ ] #64 — Promotion/Upload automation
- [ ] Voting: auto-resolve consensus translations → export `strings.xml`

## Blockers
None

## Next Session Suggestion
Run the translate pipeline with the 60-min timeout and monitor `scripts/analyze_translate_logs.sh` output for any remaining slow providers.
