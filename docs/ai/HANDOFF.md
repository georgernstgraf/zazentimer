# HANDOFF.md

Current branch: main

Open tasks for next agent:
1. [ ] Improve code readability: Extract logic from ZazenTimerActivity into a ViewModel.
2. [ ] Modernize UI/UX: Migrate remaining components to Material 3 (currently using legacy styles).
3. [ ] Implement unit tests: Currently, only a basic test exists for BellCollection; need more robust tests for DbOperations.
4. [ ] CI/CD CI pipeline setup: Verified CI/CD is present in .github/workflows/ci.yml, but needs to be enhanced for test execution.

Context for next agent:
- The project is now buildable and installed on the target device.
- All dependencies were migrated to AndroidX.
- Basic activity instrumentation test is in place.
- AGENTS.md bootstrapping is configured.

- Last cleared: 2026-04-04.
