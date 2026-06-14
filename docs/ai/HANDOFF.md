# Hand Off

## Open Tasks
1. [ ] #256: Run the question pre-flight for all 11 sub-issues in parallel, collect the sub-agents' responses, synthesize them, and present the consolidated questions.
2. [ ] #255: Run `scripts/run-instrumentation.sh` to verify the new `am instrument` two-phase approach works on all API levels (or physical device).
3. [ ] #255: Create `BackupRestoreUiTest.kt` (UiAutomator, SAF picker flow) — future task, not blocking for #255 closure.
4. [ ] **#270 (just completed, pending orchestrator verification)**: Run `scripts/run-instrumentation.sh` to verify the BellCollection removal across the full API 23-36 matrix. The API 34 FK crash (`DuplicateSessionTest.testDuplicateSessionCreatesCopyWithPrefix`) should be resolved. All JVM tests + detekt + assembleDebug are green.
5. [ ] **#270 follow-up**: Migrate remaining `runBlocking` callsites in `SectionEditFragment` (`fillDataFromViews`, `installPlayGongListener`, `installBellSelectionListener`) and `MainFragment.onPause()` to `lifecycleScope.launch` where structurally feasible. These were kept on `runBlocking` because synchronous bell-result usage immediately after the call made a launch-based migration too invasive for this refactor.

Last updated: 2026-06-15
