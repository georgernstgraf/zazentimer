# Pitfalls

Things that do not work, subtle bugs, and non-obvious constraints.
Read this file carefully before making changes in affected areas.

- **Espresso & 300ms Polling**: Legacy polling in ViewModels prevented Espresso from reaching an idle state, causing `AppNotIdleException`.
- **Service Binding Race**: Fragments attempting to interact with `MeditationService` before `onServiceConnected` caused NPEs or lost commands; use `MeditationRepository` as the stable intermediary.
- **UTP / API 35 Bug**: AGP 9.1.1 UTP runner may report "0 tests found" on API 35; use manual `am instrument` fallback in scripts.
- **Emulator Hardware**: Never use `-target google_apis` with newer emulators (36.5.10+); use `-target android`.
- **Database Race**: DB operations are async; without `IdlingResource`, tests may read old data before a write finishes.
