# Project State

Current status as of 2026-05-24.

## Current Focus
#202 — Building the Deno translate orchestrator with opencode HTTP API.

## Completed (this cycle)
- [x] #202 — Prisma schema + seed + migration
- [x] #202 — `voting_api.ts` Hono backend (POST /api/votes, error handling, serialized PrismaClient)
- [x] #202 — Architecture design: opencode HTTP API for LLM dispatch
- [x] #202 — Deno/TypeScript orchestrator + shared lib (db.ts, opencode_client.ts, verify.ts)
- [x] #202 — Translate + Proficiency SKILL.md
- [x] #202 — translate.ts: two-phase (proficiency + translate), file-based recovery, 10-min timeout
- [x] #202 — PROVIDER_RANKING: ascending by cost/quality (zai=1, nvidia=2, opencode=4, ..., anthropic=10)
- [x] #202 — fetchAvailableModels(): parses `opencode models` output, builds fallback chains
- [x] #202 — Fallback chain per dispatch: tries cheapest provider first, fails over to expensive
- [x] #202 — opencode_client.ts: model per message (ModelRef), system in sendMessage (PromptInput API)
- [x] #202 — Sub-issue #209 created + implementation complete
- [x] #202 — Knowledge persisted

## Pending
- [ ] #202 — Voting + export script (auto-resolve consensus translations)
- [ ] #64 — Promotion/Upload automation

## Blockers
None

## Next Session Suggestion
Run `deno task translate --all` to execute the first full translation round. Then build the voting mechanism to auto-resolve consensus translations.
