# Handoff

Open tasks for next agent session.

## Active Issues

1. [ ] **#88 — Java → Kotlin migration** (Epic). All pre-flight decisions finalized. Ready for execution. Sub-issues: #96–#102.

2. [ ] **#64 — Play Store**. Sub-issues #114 (AAB build) and #113 (privacy/legal) ready for implementation.

## #88 Pre-Flight Decisions (Authoritative)

All decisions finalized 2026-05-08. Issues #99–#102 updated to match.

### Phase 1 (#96): Build Toolchain
- Big-bang: skip AGP 8.x, go to latest stable 9.x + Gradle 9.x
- Kotlin DSL same phase (required by AGP 9.x)
- Agent researches exact versions at implementation time
- All deps bump to latest stable
- **Remove** `android.enableJetifier=true` from gradle.properties
- Rollback: direct to main, fix CI immediately

### Phase 3 (#98): KSP
- Both Room + Hilt → `ksp` (Hilt must be ≥2.51.1)
- **Enable** Room schema export (`exportSchema=true` + `ksp { arg("room.schemaLocation", ...) }`)

### Phase 4 (#99): Java → Kotlin (41 files, ~5,900 lines)
- Mechanical conversion (javac2kotlin + manual cleanup), no refactorings
- **Delete** JwtCallCredentials.java + remove gRPC/JJWT deps (dead code)
- BO classes → `data class` with `var`. **Auto-generated** toString() (all fields)
- BellCollection → Kotlin `object`, rename `init()` method
- Constants → separate Constants object, update all call sites
- SAM conversion where possible, `object :` for multi-method interfaces
- Regular interfaces (NOT `fun interface`)
- **Add @Volatile** to cross-thread fields (Meditation.stopping, Meditation.paused, Audio.playing)
- All `.kt` files in `src/main/kotlin/`, update sourceSets
- Switch all fragments to viewBinding
- No deprecation fixes, no MenuProvider migration

**Package moves:**
| Current | Target | Files |
|---------|--------|-------|
| `at.priv.graf.fragments` | `at.priv.graf.zazentimer.fragments` | 14 |
| `at.priv.graf.base` | `at.priv.graf.zazentimer.base` | 1 (SpinnerUtil) |
| root `.Bell` | `.bo.Bell` | 1 |
| root `.MessageView` | `.views.MessageView` | 1 |
| root `.RunOnConnect` | `.service.RunOnConnect` | 1 |
| root `.DbOperations` | `.database.DbOperations` | 1 |
| `.grpc` | DELETE entirely | 1 (JwtCallCredentials) |

**5 commits:**
1. BO + Database + DI + Constants (package cleanup + Constants move + JwtCallCredentials deletion)
2. Audio
3. Service + ViewModel
4. Fragments + Adapters + Activity
5. TimerView (last, riskiest)

### Phase 5 (#100): Test Conversion (21 files)
- Mechanical, no consolidation, no refactoring
- Keep Page Object fluent pattern
- Stay JUnit4, leave SystemClock.sleep as-is
- Convert @RequiresDisplay with HiltTestRunner
- Unit test directory already correct (no de/gaffga/ exists)
- **Critical:** `@get:Rule(order = 0/1)` must be preserved (PITFALLS #11)

### Phase 6 (#101): SDK 34 → 36
- 34→36 in one step
- Edge-to-edge: **opt-out** via `windowOptOutEdgeToEdgeEnforcement` (#103 is follow-up)
- Predictive Back: ignore / default behavior
- styles.xml: clean up bundled AppCompat/Material copies (~1,500 lines → ~60)
- Preserve `values/bools.xml` and `values/integers.xml` (PITFALLS #23)
- minSdk stays 29, CI matrix stays 29-35

### Phase 7 (#102): Final Cleanup
- Compiler options: minimal (`jvmTarget = "17"` only)
- Add ktlint + detekt as Gradle plugins + CI steps
- No strict compiler options (that's #108, post-88)
- Verification: Stage 2 (API 29 + 35)
- Update all docs/ai/ files

### Follow-up Issues (post-88)
- #103: Proper edge-to-edge (remove opt-out)
- #105: Idiomatic Kotlin refactorings
- #106: Coroutines migration
- #107: Predictive Back Gesture
- #108: Strict Kotlin compiler options
- #110: styles.xml further cleanup (depends on #103)
- #111: Test infrastructure consolidation

## Key Context
- **All nightly tests pass (API 29-35)** after 7 bug fixes from the #104 deprecated API changes.
- **DbOperations `duplicateSession()` had a deadlock** — fixed by accessing DAOs directly inside `executeSync`.
- **API 33+ tests use `am instrument`** instead of Gradle UTP due to `RootViewWithoutFocusException`.
- **`am instrument` retry on focus errors** — API 33/34 intermittently lose window focus.
- **SettingsPage uses `R.id.recycler_view`** — AndroidX PreferenceFragmentCompat uses RecyclerView not ListView.
- 3-stage pipeline: Stage 1 (commit gate, local + GitHub Actions), Stage 2 (issue close gate, local with Xvfb), Stage 3 (nightly, VPS cron 02:00 UTC)
- Tag-based releases: push `v*` tag → `release.yml` builds AAB + uploads to Play Console
- VPS has Xvfb, KVM, all AVDs, and Android SDK installed
- **`run-nightly.sh` destroys uncommitted changes** — always commit before running it
- **Scripts use `resolve_avd()`** for portable AVD detection
- **`-target google_apis` removed** — flag removed in emulator 36.5.10
