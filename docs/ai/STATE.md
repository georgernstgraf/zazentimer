## Completed
- [x] #10 UX fixes (Settings crash, flattened preferences), gRPC JWT token generation, and UI Test Plan creation.
- [x] #8 Startup crash
- [x] #6 Meditation start crash (PendingIntent mutability and FGS permissions)
- [x] Full automation of Start Meditation UI test scenario
- [x] #11 Background timer reliability (setAlarmClock + explicit Intent + removed doze warning)
- [x] #16 FAB icon fix (replace empty ic_plus.png with vector drawable)
- [x] #17 Session subtitle truncation fix on main screen
- [x] #18/#19 Backup/restore via Storage Access Framework + instrumented tests
- [x] CI: instrumented test runner with Android emulator
- [x] CI: Android SDK setup, JDK 17, debug APK artifact upload
- [x] #21 Phase 1: Onboarding docs, Gradle Wrapper migration, docs cleanup, dead code removal
- [x] #23 Phase 2: Modernize deprecated APIs (startForegroundService, Context constants, Activity Result API)
- [x] #22 Phase 3: Architecture modernization epic (all sub-issues complete)
- [x] #20 Improve app navigation and information architecture
  - [x] #31 Dynamic toolbar titles + NavigationUI setup
  - [x] #32 Replace Spinner with RecyclerView session list
  - [x] #33 BottomNavigationView for primary screens
  - [x] #34 FAB and per-session contextual actions
  - [x] #35 Back-press confirmation during meditation
  - [x] #36 Material Motion transitions
- [x] #51 Illustrated app documentation: 15 screenshots across 12 screens
- [x] #52 Fix Duplicate Session crash (SQLiteConstraintException on sessions._id) + instrumented test

## Completed (previous sessions)
- [x] #55 Fix corrupted meditation state after natural finish
- [x] #56 Volume system simplification

## Completed (this session)
- [x] #57 Show session name on meditation screen and meditation-in-progress indicator in toolbar
  - Session name in dedicated `TextView` below TimerView in all states (idle/running/paused)
  - Idle state redesign: `MeditationViewModel.emitIdleState()` with colored section arcs, first section name in ring, greyed stop button
  - Zen circle `ImageView` in toolbar toggled by `MeditationService.isServiceRunning()`, 16dp left margin
  - Sessions screen blocks all interactions during meditation: card selection, overflow popup, Start button, FAB
  - `SessionListAdapter.interactionsEnabled` flag guards clicks at the adapter level
- [x] #58 Fix ringer restoration and MainFragment stuck disabled after meditation ends
  - Smart ringer restoration: `mutedRingerMode` records what app set, `unmutePhone()` skips restore if user changed it
  - Race condition fix: `isRunning = false` moved to `onMeditationEnd()` before `stopSelf()`

## Pending
- [ ] #51 (remaining) Logcat correlation with screen navigation, full log capture per screen

## Blockers
- None

## Next Session Suggestion
Rerun emulator screen capture with full `adb logcat` to capture all app logs and correlate with specific screens (#51 remaining work).
