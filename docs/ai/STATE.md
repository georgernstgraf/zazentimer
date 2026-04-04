- [x] #10 UX fixes (Settings crash, flattened preferences), gRPC JWT token generation, and UI Test Plan creation.
- [x] #8 Startup crash
- [x] #6 Meditation start crash (PendingIntent mutability and FGS permissions)
- [x] Full automation of Start Meditation UI test scenario
- [ ] #11 Background timer reliability (in progress — code changes done, needs device testing)

## Pending
- [ ] Implement the remaining integration tests mapped out in `docs/ai/UI_TEST_PLAN.md`.
- [ ] Device-test #11: verify gong fires reliably on long background sessions with setAlarmClock().

## Blockers
- None

## Next Session Suggestion
Test #11 on device with a long meditation session (20+ min) while the app is backgrounded. If gong fires reliably, close the issue and revisit whether the screen-on warning dialog is still needed.
