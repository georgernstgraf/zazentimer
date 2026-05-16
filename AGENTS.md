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

## Mandatory Pre-Flight Checks
Before running any emulator or test command:
1. **Read `docs/ai/` knowledge files first** (per the bootstrap order above)
2. **Check `$DISPLAY`** — if set, use it directly; the script starts Xvfb automatically if unset
3. **Use `scripts/run-instrumentation.sh`** — never manually reimplement emulator/gradle/test logic inline
4. **Never use `-no-window`** with the Android emulator — use Xvfb or real `$DISPLAY` instead
5. **Never use `-target google_apis`** — removed in emulator 36.5.10 (PITFALLS #73)

## Workflow Skills
- **Issue management:** Use the `issue-workflow` skill for all GitHub issue operations (start, commit, finish).
- **Knowledge persistence:** Use the `knowledge-persistence` skill to persist session context into `docs/ai/` files after meaningful changes or when wrapping up.

## Pre-Push Lint
Before pushing (or committing), always run:
```
./gradlew ktlintCheck detekt --no-daemon
```
If there are violations, fix with `./gradlew ktlintFormat` first, then verify with the check command above. Do not push if lint fails.

A pre-push hook is available at `scripts/git-hooks/pre-push` — install it to automate this check:
```
cp scripts/git-hooks/pre-push .git/hooks/pre-push && chmod +x .git/hooks/pre-push
```

## Project-Specific Constraints
- **Trunk-based development with tag-based releases.** Commit directly to `main`. No branches, no PRs. Push a `v*` tag to trigger release workflow.
- Agent work is considered complete only if the application successfully starts in the emulator.
- **The main agent must not create any code.** Its sole task is to orchestrate sub-agents that solve sub-issues. Delegate all implementation work to Task agents.

## Destructive Git Operations — STRICT RULES

- **NEVER run `git checkout -- <file>` or `git restore <file>`** to discard uncommitted changes, unless the changes were made by the agent itself in the current task.
- **NEVER run `git stash drop`, `git reset --hard`, `git clean -fd`, or any command that permanently discards work.**
- If you find unexpected dirty files that don't belong to the current task, **stop and ask the user** before touching them. The user may have pending work.
- When orchestrating multiple sub-agents, each agent must only modify files within its scope. Review `git diff` output carefully and **preserve any unrelated changes**.

## Fixing Instrumented Test Failures

1. Run: `scripts/run-instrumentation.sh`
2. If exit code 0: all tests green, done
3. If exit code 1: build failure, analyze and fix
4. If exit code 2: test failure at specific API level
   - Analyze the failure output
   - Run targeted: `scripts/run-instrumentation.sh --api <level>`
   - Fix the issue
   - Commit with issue reference
   - Go to step 1 (full fail-fast run to verify)
5. Maximum 100 iterations. If not green after 100, report remaining failures.

Guard rails:
- If the SAME test fails with the SAME error twice in a row, the fix was wrong. Stop and escalate instead of looping.
- Commit after each verified fix.
- Auto-tag (`tested-YYYY-MM-DD`) only happens on full green runs with real display and no `--api` switch.

