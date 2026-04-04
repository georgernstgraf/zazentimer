# UI Test Plan & Meta-Definition

This document defines all required user interface interactions, edge cases, and regression checks for the Zazen Meditation Timer.
It serves as the single source of truth for UI test coverage.

**Status Legend:**
- 🔴 Not Automated (Manual testing required)
- 🟡 Partially Automated (Basic path covered, edge cases missing)
- 🟢 Fully Automated

## 1. Startup & Main Screen
| Scenario | Description / Steps | Expected Outcome | Regressions / Bugs | Automated |
| :--- | :--- | :--- | :--- | :--- |
| **App Launch (Fresh)** | Launch app on a fresh install | Toolbar displays "Zazen Meditation Timer"; Default sessions are created and visible | #8 (Crash on init due to legacy Support Library/styles) | 🔴 |
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
| **Backup to SD** | Tap "Create Backup" | Prompts for permission/location; ZIP created | Legacy Storage Access issues | 🔴 |

## 5. Active Meditation (Service & Notification)
| Scenario | Description / Steps | Expected Outcome | Regressions / Bugs | Automated |
| :--- | :--- | :--- | :--- | :--- |
| **Timer Countdown** | Wait 1 second during active meditation | Timer visually decrements | | 🔴 |
| **Screen Lock** | Let device sleep during meditation | Meditation continues; notification is visible | WakeLock / FGS issues | 🔴 |
| **Section Transition** | Wait for one section to end | Bell plays; timer switches to next section | | 🔴 |

---

## Automation Guidelines
When automating the scenarios above, adhere to the following technological standards to keep tests consistent and maintainable:

1. **Page Object Model (POM):** Encapsulate UI interactions within Screen classes (e.g., `MainScreen.java`, `SettingsScreen.java`). Test classes should contain domain-specific commands (e.g., `mainScreen.clickStartMeditation()`), not raw Espresso `onView()` calls.
2. **Idling Resources:** Use AndroidX `IdlingResource` for asynchronous operations like Database I/O, `MeditationService` lifecycle changes, and Audio playback to prevent flaky tests.
3. **Hermetic Testing:** Ensure database state is reset between tests (e.g., using an in-memory database or clearing data via `@Before` methods).
