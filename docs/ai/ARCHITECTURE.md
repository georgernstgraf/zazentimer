# Architecture

Living structural map of the system as of 2026-04-04.
Overwritten when structural changes occur during a session.

## Overview
Reconstructed legacy Android application using the `android.support` libraries.

## Commands (`commands/`)
| Command | Purpose | Delegates to |
|---------|---------|-------------|
| `./gradle-7.5/bin/gradle build` | Build the project | Gradle build system |
| `adb install -r ...` | Install the APK to device | ADB |

## Knowledge Files (`docs/ai/`)
| File | Purpose | Update mode |
|------|---------|------------|
| UI_TEST_PLAN.md | Meta-definition for UI test scenarios | Append/Update |
| HANDOFF.md | Open tasks for next session | Overwrite |
| DECISIONS.md | Chronological record of choices | Append |
| ARCHITECTURE.md | Living structural map | Overwrite |
| CONVENTIONS.md | Ongoing rules to follow | Append |
| PITFALLS.md | Hard-won failure knowledge | Append |
| DOMAIN.md | Business/domain rules | Append |
| STATE.md | Current project status | Overwrite |

## Data Flows
- APK source -> decompilation output -> project source/resources -> gradle build -> APK.
