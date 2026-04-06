# UI Test Plan & Meta-Definition

This document defines all required user interface interactions, edge cases, and regression checks for the Zazen Meditation Timer.
It serves as the single source of truth for UI test coverage.

**Status Legend:**
- 🔴 Not Automated (Manual testing required)
- 🟡 Partially Automated (Basic path covered, edge cases missing)
- 🟢 Fully Automated

## Test Source Files
| File | Tests | Status |
|------|-------|--------|
| `SimpleTest.java` | `testActivityLaunches()` | 🟢 |
| `ZazenTimerActivityTest.java` | `activityLaunchesSuccessfully()`, `testFreshAppLaunch()` | 🟢 |
| `BackupRestoreTest.java` | `testBackupCreatesValidZip()`, `testRestoreFromZip()` | 🟢 |
| `utils/DatabaseIdlingResource.java` | (utility, not a test) | — |

## 1. Startup & Main Screen
| Scenario | Description / Steps | Expected Outcome | Regressions / Bugs | Automated |
| :--- | :--- | :--- | :--- | :--- |
| **App Launch (Fresh)** | Launch app on a fresh install | Toolbar displays "Zazen Meditation Timer"; Default sessions are created and visible | #8 (Crash on init due to legacy Support Library/styles) | 🟢 |
| **Start Meditation** | Select a session -> Click "Start Meditation" button | Transitions to Active Meditation view; Timer starts counting down | #6 (PendingIntent mutability on Android 12+), FGS Crash (Android 14+) | 🟡 |
| **Screen Rotation** | Rotate device on Main Screen | UI adapts to landscape/portrait; Selected session remains selected | | 🔴 |

## 2. Session Management (Main Screen Options)
| Scenario | Description / Steps | Expected Outcome | Regressions / Bugs | Automated |
| :--- | :--- | :--- | :--- | :--- |
| **Copy Session** | Select Session -> Menu -> "Copy Session" | A duplicate session is created with "(Copy)" suffix and selected in the list | | 🔴 |
| **Delete Session** | Select Session -> Menu -> "Delete Session" -> Confirm | Session is removed from the database and UI list | | 🔴 |
| **Delete Cancel** | Select Session -> Menu -> "Delete Session" -> Cancel | Deletion is aborted; Session remains in list | | 🔴 |

## 3. Edit Session & Sections
| Scenario | Description / Steps | Expected Outcome | Regressions / Bugs | Automated |
| :--- | :--- | :--- | :--- | :--- |
| **Open Edit Session** | Select Session -> Menu -> "Edit Session" | Transitions to Edit Session fragment | | 🔴 |
| **Update Metadata** | Change Name & Description -> Press Back | Changes are saved to DB and reflected on Main Screen | | 🔴 |
| **Add New Section** | Click Floating Action Button (FAB) | Transitions to Edit Section screen with default values | Legacy FAB crash | 🔴 |
| **Edit Section Config** | Tap a section -> Change duration, bell count, bell gap | Values update visually; Gaps scroll correctly | | 🔴 |
| **Test Bell Sound** | Inside Edit Section -> Tap "Play" icon | Selected gong sound plays at the set volume | | 🔴 |

## 4. Settings & Preferences
| Scenario | Description / Steps | Expected Outcome | Regressions / Bugs | Automated |
| :--- | :--- | :--- | :--- | :--- |
| **Open Settings** | Menu -> Settings | Settings screen opens without crashing | `InflateException` from legacy `DialogPreference` | 🔴 |
| **Theme Toggle** | Tap "Theme" -> Select "Dark Theme" | Application UI immediately switches to Dark Theme | | 🔴 |
| **Mute Settings** | Tap "Mute Settings" checkboxes | Preferences update correctly | Nested PreferenceScreen issue (Fixed by flattening) | 🔴 |
| **Volume Adjustment** | Adjust "Maximum Bell Volume" SeekBar | Preference updates; no crashes | | 🔴 |
| **Backup to SD** | Tap "Create Backup" | Prompts for location via SAF; ZIP created | Legacy Storage Access issues | 🟡 |

## 5. UI Bug Regression Tests (#37)
| Scenario | Description / Steps | Expected Outcome | Regressions / Bugs | Automated |
| :--- | :--- | :--- | :--- | :--- |
| **Duplicate Session** | Select session -> card overflow -> "Copy" | Duplicate session created with "(Copy)" suffix; original unchanged | #37/#39 Crash from reusing old PKs (both session.id and section.id must be reset to 0) | 🔴 |
| **Back Arrow Navigation** | Navigate to meditation -> press toolbar back arrow | Returns to session list without crash | #37/#40 onBackPressed() bypassed fragment callbacks; popUpToInclusive=true cleared back stack | 🔴 |
| **Back Arrow from Edit** | Navigate to Session Edit -> press toolbar back arrow | Returns to session list without crash | #37/#40 Same root cause as above | 🔴 |
| **Stop Meditation Confirmation** | Start meditation -> press Stop button | Confirmation dialog appears; Cancel continues, Confirm stops | #37/#41 Stop button bypassed showStopConfirmationDialog() | 🔴 |
| **Add Session via Menu** | Open toolbar overflow menu -> "Add Session" | Navigates to Edit Session screen for new session | #37/#42 FAB removed, moved to overflow menu | 🔴 |
| **Session Selection Highlight** | Tap a session card on main screen | Selected card shows visual highlight (activated state); tapping another moves highlight | #37/#43 No activated-state drawable on item_session.xml | 🔴 |

## 6. Active Meditation (Service & Notification)
| Scenario | Description / Steps | Expected Outcome | Regressions / Bugs | Automated |
| :--- | :--- | :--- | :--- | :--- |
| **Timer Countdown** | Wait 1 second during active meditation | Timer visually decrements | | 🔴 |
| **Screen Lock** | Let device sleep during meditation | Meditation continues; notification is visible | WakeLock / FGS issues | 🔴 |
| **Section Transition** | Wait for one section to end | Bell plays; timer switches to next section | | 🔴 |

---

## Automation Guidelines
When automating the scenarios above, adhere to the following technological standards to keep tests consistent and maintainable:

1. **Page Object Model (POM):** Encapsulate UI interactions within Page classes (e.g., `MainPage.java`, `SettingsPage.java`, `MeditationPage.java`). Test classes should contain domain-specific commands (e.g., `mainPage.clickStartMeditation()`), not raw Espresso `onView()` calls.
2. **Idling Resources:** Use AndroidX `IdlingResource` for asynchronous operations like Database I/O, `MeditationService` lifecycle changes, and Audio playback to prevent flaky tests.
3. **Hermetic testing:** Ensure database state is reset between tests using `testInstrumentationRunnerArguments clearPackageData: 'true'` (already configured in `build.gradle` with Android Test Orchestrator).
