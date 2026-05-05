## Knowledge Bootstrap
Before starting any task, read the following files in order:
1. `docs/ai/HANDOFF.md` ← **read first, act on it**
2. `docs/ai/CONVENTIONS.md`
3. `docs/ai/DECISIONS.md`
4. `docs/ai/ARCHITECTURE.md`
5. `docs/ai/PITFALLS.md`
6. `docs/ai/STATE.md`
7. `docs/ai/DOMAIN.md` (if task involves business logic)
8. `docs/ai/ONBOARDING.md` (if setting up the development environment)

If `HANDOFF.md` contains open tasks, complete them before starting

## Workflow Skills
- **Issue management:** Use the `issue-workflow` skill for all GitHub issue operations (start, commit, finish).
- **Knowledge persistence:** Use the `knowledge-persistence` skill to persist session context into `docs/ai/` files after meaningful changes or when wrapping up.

## Project-Specific Constraints
- **Trunk-based development with tag-based releases.** Commit directly to `main`. No branches, no PRs. Push a `v*` tag to trigger release workflow.
- Agent work is considered complete only if the application successfully starts in the emulator.
- **The main agent must not create any code.** Its sole task is to orchestrate sub-agents that solve sub-issues. Delegate all implementation work to Task agents.

