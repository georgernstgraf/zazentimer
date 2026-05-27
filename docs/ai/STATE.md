# Project State

Current status as of 2026-05-27.

## Current Focus
#202 — Translation pipeline (runs in progress, ~100 locales seeded)

## Completed (this cycle)
- [x] Verbose translate log: `stringCount`/`emptyCount`/`skippedMasterString` instead of `stored`/`skipped`
- [x] Proficiency start log line before each proficiency assessment
- [x] Always-on verbose language-start stats with provider label
- [x] Verify error enrichment: raw output snippet + JSON.parse error in error messages
- [x] Skill "no access to files" relaxed — models may read their own output files
- [x] System prompt always sent on retry (not just first request)
- [x] PROVIDER_RANKING → MODEL_PROVIDERS: per-model provider mapping
- [x] llmmodels_seed.json → llmmodels_master.json: renamed, mistral-large added, seed deletes obsolete models
- [x] Model-DB validation: error if MODEL_PROVIDERS model not in DB, warning if DB model not in MODEL_PROVIDERS
- [x] Provider label in language-start log: "only provider (xyz)" vs "provider (rank X/Y)/model"

## Pending
- [ ] #64 — Play Store
- [ ] #202 — Translation Epic (runs in progress)
- [ ] #207 — db refactor: `_id` -> `id`

## Blockers
None

## Next Session Suggestion
Check latest translation run results. If all models have completed enough locales, implement voting auto-resolve → export strings.xml.
