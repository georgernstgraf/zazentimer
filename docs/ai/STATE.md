# Project State

Current status as of 2026-05-24.

## Current Focus
#202 — Prisma translation voting pipeline: schema, seed data, and migration complete.

## Completed (this cycle)
- [x] #202 — Prisma schema: `locales`, `master_strings`, `llm_models`, `votes` with confidence CHECK(1-5) and CASCADE on delete
- [x] #202 — `scripts/generate_languages_seed.py` (Python, pycountry, ISO 639-3 + BCP 47 + POSIX)
- [x] #202 — `prisma/translations/languages_seed.json` (123 locale entries, generated)
- [x] #202 — `prisma/translations/llmmodels_seed.json` (10 LLM models from 6 providers)
- [x] #202 — `prisma/translations/whisper_languages.json` (100 languages, static, no Torch)
- [x] #202 — `prisma/translations/seed.ts` idempotent Deno seed script (PrismaClient, upsert, regex XML parser)
- [x] #202 — Init migration with CHECK constraint and CASCADE in CREATE TABLE
- [x] #202 — DB seeded and verified idempotent
- [x] Removed `--no-daemon` from pre-push hook; symlink `.git/hooks/pre-push -> ../../scripts/git-hooks/pre-push`
- [x] Removed dead `zazentimer.test.apis.claw` from `gradle.properties`
- [x] Missing AVDs skipped (not failed) in `run-instrumentation.sh`

## Pending
- [ ] #202 — Multi-LLM translation pipeline Python dispatch scripts + LLM voting logic
- [ ] #64 — Promotion/Upload automation: Fastlane-like upload script

## Blockers
None

## Next Session Suggestion
Build the Python dispatch scripts for #202 that call each of the 10 LLM models with translation prompts, collect candidates, and run the voting algorithm. Then run instrumented tests to verify CI health.
