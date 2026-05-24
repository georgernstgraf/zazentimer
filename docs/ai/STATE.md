# Project State

Current status as of 2026-05-24.

## Current Focus
#202 — Building the Deno translate orchestrator with opencode HTTP API.

## Completed (this cycle)
- [x] #202 — Prisma schema + seed + migration
- [x] #202 — `voting_api.ts` Hono backend (POST /api/votes, error handling, serialized PrismaClient)
- [x] #202 — Architecture design: opencode HTTP API for LLM dispatch
- [x] #202 — Decision: Deno/TypeScript orchestrator (not Python)
- [x] #202 — Decision: one opencode session per (model, locale)
- [x] #202 — Decision: null allowed in output JSON, proficiency required
- [x] #202 — Decision: 10-minute timeout for --all runs
- [x] #202 — M:N language_proficiencies junction table with CHECK(1-5)
- [x] #202 — Skill: .opencode/skills/translate/SKILL.md (self-contained, no tools)
- [x] #202 — prisma/lib/db.ts (shared Prisma queries: getOrCreate*, getExistingVotes, upsertVote, upsertProficiency)
- [x] #202 — prisma/lib/opencode_client.ts (HTTP session client: createSession, sendMessage, closeSession)
- [x] #202 — prisma/lib/verify.ts (output verification: JSON structure, null allowed, placeholder integrity)
- [x] #202 — prisma/translate.ts (orchestrator nested loop with 10-min timeout, retry, verify)
- [x] #202 — deno.json task: translate
- [x] #202 — Sub-issue #209 created for this implementation batch
- [x] #202 — Knowledge persisted: 9 decision entries, 5 pitfalls, conventions, domain, state, handoff

## Pending
- [ ] #202 — Voting + export script (auto-resolve consensus translations)
- [ ] #64 — Promotion/Upload automation

## Blockers
None

## Next Session Suggestion
Run `deno task translate --all` to execute the first full translation round. Then build the voting mechanism to auto-resolve consensus translations.
