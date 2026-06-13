#!/bin/bash
set -euo pipefail

# ──────────────────────────────────────────────
# run-instrumentation.sh — Instrumented test runner
#
# Runs instrumented Android tests on a single device:
#   - Physical device preferred (if connected)
#   - Falls back to emulator (per API level)
#   - Uses `am instrument` for precise serial targeting
#   - BackupRestore tests run LAST to avoid DB corruption
#
# Usage:
#   scripts/run-instrumentation.sh [options]
#   Options:
#     --continue-on-error    Don't stop on first API failure
#     --ignore-dirty-git     Skip git clean check
#     --debug                Verbose logging
#     --force-xvfb           Force Xvfb for display
#     --cold-boot            Don't use emulator snapshots
#     --api <level[,level]>  Run specific API level(s)
# ──────────────────────────────────────────────

CONTINUE_ON_ERROR=false
IGNORE_DIRTY_GIT=false
DEBUG_LOG=false
FORCE_XVFB=false
COLD_BOOT=false
TARGET_APIS=()

while [[ $# -gt 0 ]]; do
    case "$1" in
    --continue-on-error)
        CONTINUE_ON_ERROR=true
        shift
        ;;
    --ignore-dirty-git)
        IGNORE_DIRTY_GIT=true
        shift
        ;;
    --debug)
        DEBUG_LOG=true
        shift
        ;;
    --force-xvfb)
        FORCE_XVFB=true
        shift
        ;;
    --cold-boot)
        COLD_BOOT=true
        shift
        ;;
    --api)
        if [ -z "${2:-}" ]; then
            echo "ERROR: --api requires an argument (e.g., --api 32 or --api 29,35)"
            exit 1
        fi
        IFS=',' read -ra TARGET_APIS <<<"$2"
        shift 2
        ;;
    *)
        echo "Usage: $0 [--continue-on-error] [--ignore-dirty-git] [--debug] [--force-xvfb] [--cold-boot] [--api <level>[,<level>...]]"
        exit 1
        ;;
    esac
done

# ──────────────────────────────────────────────
# Android SDK
# ──────────────────────────────────────────────
if [ -n "${ANDROID_HOME:-}" ] && [ -d "${ANDROID_HOME:-}" ]; then
    echo "=== Using ANDROID_HOME=$ANDROID_HOME ==="
elif [ -n "${ANDROID_SDK_ROOT:-}" ] && [ -d "${ANDROID_SDK_ROOT:-}" ]; then
    export ANDROID_HOME="$ANDROID_SDK_ROOT"
    echo "=== Using ANDROID_SDK_ROOT=$ANDROID_HOME ==="
elif [ -d "$HOME/Android/Sdk" ]; then
    export ANDROID_HOME="$HOME/Android/Sdk"
    echo "=== Using local SDK at $ANDROID_HOME ==="
elif [ -d "/opt/android-sdk" ]; then
    export ANDROID_HOME="/opt/android-sdk"
    echo "=== Using VPS SDK at $ANDROID_HOME ==="
else
    echo "ERROR: Android SDK not found. Set ANDROID_HOME or ANDROID_SDK_ROOT."
    exit 1
fi
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools
export ANDROID_EMU_ENABLE_CRASH_REPORTING=0

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
source "$SCRIPT_DIR/start-emulator.sh"
source "$SCRIPT_DIR/stop-emulator.sh"

TODAY=$(date +%Y-%m-%d)
LOG_FILE=$PROJECT_DIR/logs/instrumentation-${TODAY}.log

UNIT_RESULT=0
declare -A RESULTS
FAILED_APIS=()
ERROR_LOGS=()
IS_REAL_DISPLAY=true
SKIP_INSTRUMENTED=false
SNAPSHOT_FLAG=""
API_LOG=""
EMULATOR_XVFB_PID=""

USE_PHYSICAL_DEVICE=false
DEVICE_SERIAL=""
DEVICE_API_LEVEL=0
TEST_PACKAGE=""
RUNNER=""
PHASE1_CLASSES=""
PHASE2_CLASSES=""

APP_APK="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
TEST_APK="$PROJECT_DIR/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
BACKUP_FIXTURE="$PROJECT_DIR/app/src/test/resources/backups/zentimer_backup_room_v2.zip"

# ──────────────────────────────────────────────
# Logging
# ──────────────────────────────────────────────
log() {
    echo "[$(date '+%H:%M:%S')] $*"
}

exec_api_log() {
    local api_level=$1
    API_LOG="$PROJECT_DIR/logs/api${api_level}-${TODAY}.log"
    : > "$API_LOG"
    echo "=== API $api_level — $(date) ===" >> "$API_LOG"
}

log_api() {
    local msg="[$(date '+%H:%M:%S')] $*"
    echo "$msg"
    [ -n "${API_LOG:-}" ] && echo "$msg" >> "$API_LOG" 2>/dev/null || true
}

log_phase() {
    local api_level=$1
    shift
    log_api "API $api_level — Phase: $*"
}

dump_logcat() {
    local api_level=$1
    local serial="$2"
    local logcat_file="$PROJECT_DIR/logs/api${api_level}-${TODAY}-logcat.txt"
    log_api "Dumping logcat to $logcat_file ..."
    adb -s "$serial" logcat -d > "$logcat_file" 2>/dev/null || true
    log_api "Logcat dumped ($(wc -l < "$logcat_file") lines)"
}

preserve_crash_dbs() {
    local api_level=$1
    local dest="$PROJECT_DIR/logs/crashdb-api${api_level}-${TODAY}"
    local found=false
    for db in /tmp/android-georg/emu-crash-*.db; do
        [ -f "$db" ] || continue
        if [ "$found" = false ]; then
            mkdir -p "$dest"
            found=true
        fi
        mv "$db" "$dest/" 2>/dev/null || true
        log_api "Preserved crash DB: $(basename "$db") -> $dest/"
    done
}

print_summary() {
    log ""
    log "========================================="
    log "  Test Summary — $TODAY"
    log "========================================="
    log "Mode: $([ "$CONTINUE_ON_ERROR" = true ] && echo 'continue-on-error' || echo 'fail-fast')"
    if [ "$USE_PHYSICAL_DEVICE" = true ]; then
        log "Device: $DEVICE_SERIAL (physical, API $DEVICE_API_LEVEL)"
    elif [ ${#TARGET_APIS[@]} -gt 0 ]; then
        log "Target APIs: ${TARGET_APIS[*]}"
    else
        log "Target APIs: all (23-36)"
    fi
    log "Display: $([ "$IS_REAL_DISPLAY" = true ] && echo 'real' || echo 'Xvfb')"
    log ""
    log "Unit Tests: $([ $UNIT_RESULT -eq 0 ] && echo 'PASS' || echo 'FAIL')"
    if [ "$USE_PHYSICAL_DEVICE" = true ]; then
        if [ -z "${RESULTS[$DEVICE_API_LEVEL]+isset}" ]; then
            log "API $DEVICE_API_LEVEL:  SKIPPED"
        elif [ "${RESULTS[$DEVICE_API_LEVEL]}" -ne 0 ]; then
            log "API $DEVICE_API_LEVEL:  FAIL"
        else
            log "API $DEVICE_API_LEVEL:  PASS"
        fi
    else
        for api in "${APIS_TO_RUN[@]}"; do
            if [ -z "${RESULTS[$api]+isset}" ]; then
                log "API $api:    SKIPPED"
            elif [ "${RESULTS[$api]}" -ne 0 ]; then
                log "API $api:    FAIL"
            else
                log "API $api:    PASS"
            fi
        done
    fi
    log "========================================="
}

# ──────────────────────────────────────────────
# Cleanup
# ──────────────────────────────────────────────
cleanup() {
    emulator_kill_all
    if [ -n "${EMULATOR_XVFB_PID:-}" ] && kill -0 "$EMULATOR_XVFB_PID" 2>/dev/null; then
        kill "$EMULATOR_XVFB_PID" 2>/dev/null || true
    fi
    rm -f /tmp/.X99-lock /tmp/.X11-unix/X99 2>/dev/null || true
}
trap cleanup EXIT

# ──────────────────────────────────────────────
# Device helpers
# ──────────────────────────────────────────────
clean_device_packages() {
    local serial="$1"
    local api_level="${2:-0}"
    if [ "$api_level" -ge 36 ]; then
        log_api "Skipping package cleanup on API $api_level (can destabilize system services)"
        return 0
    fi
    log_api "Cleaning stale packages on $serial..."
    for pkg in \
        de.gaffga.android.zazentimer \
        de.gaffga.android.zazentimer.test \
        at.priv.graf.zazentimer \
        at.priv.graf.zazentimer.test \
        at.priv.graf.zazentimer.debug \
        at.priv.graf.zazentimer.debug.test; do
        adb -s "$serial" uninstall "$pkg" 2>/dev/null || true
        adb -s "$serial" shell pm uninstall --user 0 "$pkg" 2>/dev/null || true
    done
    adb -s "$serial" shell cmd package install-existing at.priv.graf.zazentimer 2>/dev/null || true
    adb -s "$serial" shell cmd package install-existing at.priv.graf.zazentimer.test 2>/dev/null || true
    adb -s "$serial" shell cmd package install-existing at.priv.graf.zazentimer.debug 2>/dev/null || true
    adb -s "$serial" shell cmd package install-existing at.priv.graf.zazentimer.debug.test 2>/dev/null || true
    adb -s "$serial" uninstall at.priv.graf.zazentimer 2>/dev/null || true
    adb -s "$serial" uninstall at.priv.graf.zazentimer.test 2>/dev/null || true
    adb -s "$serial" uninstall at.priv.graf.zazentimer.debug 2>/dev/null || true
    adb -s "$serial" uninstall at.priv.graf.zazentimer.debug.test 2>/dev/null || true
}

dismiss_anr_dialog() {
    local serial="$1"
    for attempt in 1 2 3; do
        local anr_window
        anr_window=$(adb -s "$serial" shell "dumpsys window windows" 2>/dev/null | grep -c "Application Error\|isn't responding\|is not responding" || true)
        if [ "$anr_window" -eq 0 ] 2>/dev/null; then
            return 0
        fi
        log_api "ANR dialog detected on $serial (attempt $attempt) — dismissing..."
        adb -s "$serial" shell input keyevent KEYCODE_DPAD_RIGHT 2>/dev/null || true
        adb -s "$serial" shell input keyevent KEYCODE_ENTER 2>/dev/null || true
        sleep 3
    done
}

clear_logcat() {
    local serial="$1"
    log_api "adb logcat -c"
    adb -s "$serial" logcat -c 2>/dev/null || true
}

# ──────────────────────────────────────────────
# resolve_physical_device — prefer physical device over emulator
# Echoes serial to stdout. Returns 0 if found, 1 otherwise.
# ──────────────────────────────────────────────
resolve_physical_device() {
    local serial
    serial=$(adb devices 2>/dev/null \
        | grep -v "^List" \
        | grep -v "^$" \
        | grep -v "emulator-" \
        | grep "device$" \
        | awk '{print $1}' \
        | head -1) || true
    if [ -n "$serial" ]; then
        echo "$serial"
        return 0
    fi
    return 1
}

# ──────────────────────────────────────────────
# get_device_api_level
# ──────────────────────────────────────────────
get_device_api_level() {
    local serial=$1
    local level
    level=$(adb -s "$serial" shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r')
    echo "${level:-0}"
}

# ──────────────────────────────────────────────
# discover_runner_and_package — from installed test APK on device
# Sets TEST_PACKAGE and RUNNER. Returns 0 on success.
# ──────────────────────────────────────────────
discover_runner_and_package() {
    local serial=$1
    local instr_output
    instr_output=$(adb -s "$serial" shell pm list instrumentation 2>/dev/null | grep zazentimer || true)

    if [ -z "$instr_output" ]; then
        log_api "ERROR: No zazentimer instrumentation found on device $serial"
        return 1
    fi

    # Parse: instrumentation:at.priv.graf.zazentimer.debug.test/at.priv.graf.zazentimer.HiltTestRunner (target=...)
    local first_line
    first_line=$(echo "$instr_output" | head -1)
    TEST_PACKAGE=$(echo "$first_line" | sed 's/instrumentation://' | cut -d'/' -f1)
    RUNNER=$(echo "$first_line" | sed 's/instrumentation://' | cut -d'/' -f2 | cut -d' ' -f1)

    log_api "Test package: $TEST_PACKAGE"
    log_api "Runner: $RUNNER"
    return 0
}

# ──────────────────────────────────────────────
# discover_test_classes — build phase 1/2 class lists from source
# Sets PHASE1_CLASSES and PHASE2_CLASSES. Returns 0 on success.
# ──────────────────────────────────────────────
discover_test_classes() {
    local test_dir="$PROJECT_DIR/app/src/androidTest/kotlin/at/priv/graf/zazentimer"
    if [ ! -d "$test_dir" ]; then
        log "ERROR: Test source directory not found: $test_dir"
        return 1
    fi

    local found_classes=()
    for f in "$test_dir"/*Test.kt; do
        [ -f "$f" ] || continue
        local basename
        basename=$(basename "$f" .kt)
        case "$basename" in
            AbstractZazenTest|BackupRestoreInstrumentedTest|BackupTest)
                continue
                ;;
        esac
        found_classes+=("at.priv.graf.zazentimer.$basename")
    done

    if [ ${#found_classes[@]} -eq 0 ]; then
        log "ERROR: No phase 1 test classes discovered in $test_dir"
        return 1
    fi

    PHASE1_CLASSES=$(IFS=,; echo "${found_classes[*]}")
    PHASE2_CLASSES="at.priv.graf.zazentimer.BackupRestoreInstrumentedTest"

    log "Phase 1 classes (${#found_classes[@]}): $PHASE1_CLASSES"
    log "Phase 2 classes: $PHASE2_CLASSES"
}

# ──────────────────────────────────────────────
# install_apks — install app + test APKs
# ──────────────────────────────────────────────
install_apks() {
    local serial=$1
    log_api "Installing app APK: $(basename "$APP_APK")"
    if ! adb -s "$serial" install -r -t "$APP_APK" 2>&1 | tee -a "$API_LOG"; then
        log_api "ERROR: Failed to install app APK"
        return 1
    fi
    log_api "Installing test APK: $(basename "$TEST_APK")"
    if ! adb -s "$serial" install -r -t "$TEST_APK" 2>&1 | tee -a "$API_LOG"; then
        log_api "ERROR: Failed to install test APK"
        return 1
    fi
    return 0
}

# ──────────────────────────────────────────────
# push_backup_fixture — push to /sdcard/Download/
# ──────────────────────────────────────────────
push_backup_fixture() {
    local serial=$1
    if [ -f "$BACKUP_FIXTURE" ]; then
        log_api "Pushing backup fixture to /sdcard/Download/..."
        adb -s "$serial" push "$BACKUP_FIXTURE" /sdcard/Download/ 2>&1 | tee -a "$API_LOG" || {
            log_api "WARNING: Failed to push backup fixture"
        }
    else
        log_api "WARNING: Backup fixture not found at $BACKUP_FIXTURE — skipping push"
    fi
}

# ──────────────────────────────────────────────
# remove_backup_fixture
# ──────────────────────────────────────────────
remove_backup_fixture() {
    local serial=$1
    adb -s "$serial" shell rm -f /sdcard/Download/zentimer_backup_room_v2.zip 2>/dev/null || true
}

# ──────────────────────────────────────────────
# grant_storage_permissions — MANAGE_EXTERNAL_STORAGE for API 30+
# ──────────────────────────────────────────────
grant_storage_permissions() {
    local serial=$1
    local test_pkg=$2
    local api_level=$3
    if [ "$api_level" -ge 30 ]; then
        log_api "Granting MANAGE_EXTERNAL_STORAGE for API $api_level..."
        adb -s "$serial" shell appops set "$test_pkg" MANAGE_EXTERNAL_STORAGE allow 2>/dev/null || true
    fi
    # Grant all runtime permissions for the test app
    adb -s "$serial" shell pm grant "$test_pkg" android.permission.READ_EXTERNAL_STORAGE 2>/dev/null || true
    adb -s "$serial" shell pm grant "$test_pkg" android.permission.WRITE_EXTERNAL_STORAGE 2>/dev/null || true
}

# ──────────────────────────────────────────────
# run_test_phase — execute am instrument for a class list
# Returns 0 on success, 1 on failure.
# ──────────────────────────────────────────────
run_test_phase() {
    local serial=$1
    local classes=$2
    local label=$3

    log_api "Running $label..."
    log_api "  Command: am instrument -w -e class $classes $TEST_PACKAGE/$RUNNER"

    local phase_output
    local result=0
    set +e
    phase_output=$(adb -s "$serial" shell am instrument -w -e class "$classes" "$TEST_PACKAGE/$RUNNER" 2>&1)
    result=$?
    set -e
    echo "$phase_output" >> "$API_LOG"

    if [ $result -ne 0 ]; then
        log_api "FAIL: $label (exit code $result)"
        return 1
    fi

    if echo "$phase_output" | grep -q "FAILURES!!"; then
        log_api "FAIL: $label (failures detected in output)"
        return 1
    fi

    if echo "$phase_output" | grep -q "INSTRUMENTATION_FAILED\|Process crashed"; then
        log_api "FAIL: $label (instrumentation failed or process crashed)"
        return 1
    fi

    log_api "PASS: $label"
    return 0
}

# ──────────────────────────────────────────────
# run_api_tests — test flow for one API level on a given serial
# ──────────────────────────────────────────────
run_api_tests() {
    local serial=$1
    local api_level=$2
    local is_emulator=${3:-false}

    exec_api_log "$api_level"

    if [ "$is_emulator" = true ]; then
        preserve_crash_dbs "$api_level"
        emulator_kill_all

        local avd_name
        log_phase "$api_level" "resolving AVD"
        avd_name=$(emulator_resolve_avd "$api_level") || {
            log_api "SKIP: No AVD for API $api_level — skipping"
            return 0
        }

        log_api ""
        log_api "========================================="
        log_api "  API $api_level — Starting emulator ($avd_name)"
        log_api "========================================="
        log_phase "$api_level" "starting emulator (AVD $avd_name)"

        local emu_pid
        local extra_flags="$SNAPSHOT_FLAG"
        [ "$IS_REAL_DISPLAY" = false ] && extra_flags="$extra_flags -noaudio"
        emu_pid=$(emulator_launch "$avd_name" "$serial" "$API_LOG" $extra_flags)
        sleep 2
        log_api "Emulator started (PID $emu_pid, AVD $avd_name)"

        log_phase "$api_level" "waiting for boot"
        if ! emulator_wait_boot "$serial"; then
            log_api "FAIL: API $api_level emulator did not boot"
            RESULTS[$api_level]=1
            FAILED_APIS+=("$api_level")
            ERROR_LOGS+=("API $api_level: Emulator failed to boot")
            emulator_kill_serial "$serial"
            return 0
        fi
    fi

    log_phase "$api_level" "cleaning packages"
    if [ "$is_emulator" = true ]; then
        clean_device_packages "$serial" "$api_level"
    else
        # On physical device: only clean debug/test packages to preserve user data
        adb -s "$serial" uninstall at.priv.graf.zazentimer.debug 2>/dev/null || true
        adb -s "$serial" uninstall at.priv.graf.zazentimer.debug.test 2>/dev/null || true
    fi
    dismiss_anr_dialog "$serial"

    if [ "$is_emulator" = true ]; then
        emulator_configure_system "$serial"
    else
        # Minimal system config for physical devices
        adb -s "$serial" shell settings put global window_animation_scale 0.0 2>/dev/null || true
        adb -s "$serial" shell settings put global transition_animation_scale 0.0 2>/dev/null || true
        adb -s "$serial" shell settings put global animator_duration_scale 0.0 2>/dev/null || true
        adb -s "$serial" shell svc power stayon true 2>/dev/null || true
    fi
    clear_logcat "$serial"

    log_phase "$api_level" "installing APKs"
    if ! install_apks "$serial"; then
        RESULTS[$api_level]=1
        FAILED_APIS+=("$api_level")
        ERROR_LOGS+=("API $api_level: APK installation failed")
        if [ "$is_emulator" = true ]; then
            emulator_kill_serial "$serial"
        fi
        return 0
    fi

    log_phase "$api_level" "discovering runner and package"
    if ! discover_runner_and_package "$serial"; then
        RESULTS[$api_level]=1
        FAILED_APIS+=("$api_level")
        ERROR_LOGS+=("API $api_level: Runner discovery failed")
        if [ "$is_emulator" = true ]; then
            emulator_kill_serial "$serial"
        fi
        return 0
    fi

    log_phase "$api_level" "pushing backup fixture"
    push_backup_fixture "$serial"

    log_phase "$api_level" "granting storage permissions"
    grant_storage_permissions "$serial" "$TEST_PACKAGE" "$api_level"

    log_phase "$api_level" "running Phase 1 (main tests)"
    log_api ""
    log_api "========================================="
    log_api "  API $api_level — Phase 1: Main tests"
    log_api "========================================="

    local phase1_result=0
    if ! run_test_phase "$serial" "$PHASE1_CLASSES" "Phase 1 (main tests)"; then
        phase1_result=1
    fi

    local phase2_result=0
    if [ $phase1_result -eq 0 ]; then
        log_phase "$api_level" "running Phase 2 (backup restore tests)"
        log_api ""
        log_api "========================================="
        log_api "  API $api_level — Phase 2: Backup restore"
        log_api "========================================="

        if ! run_test_phase "$serial" "$PHASE2_CLASSES" "Phase 2 (backup restore)"; then
            phase2_result=1
        fi
    else
        log_api "SKIP: Phase 2 skipped due to Phase 1 failure"
    fi

    # Cleanup
    log_phase "$api_level" "cleanup"
    remove_backup_fixture "$serial"

    local total_result=$((phase1_result + phase2_result))
    if [ $total_result -ne 0 ]; then
        log_api "FAIL: API $api_level (phase1=$phase1_result, phase2=$phase2_result)"
        RESULTS[$api_level]=$total_result
        FAILED_APIS+=("$api_level")
        ERROR_LOGS+=("API $api_level: test failure")
        if [ "$DEBUG_LOG" = true ] || [ $total_result -ne 0 ]; then
            dump_logcat "$api_level" "$serial"
        fi
    else
        log_api "PASS: API $api_level"
        RESULTS[$api_level]=0
        if [ "$DEBUG_LOG" = true ]; then
            dump_logcat "$api_level" "$serial"
        fi
    fi

    if [ "$is_emulator" = true ]; then
        emulator_kill_serial "$serial"
    fi
}

# ══════════════════════════════════════════════
# Main
# ══════════════════════════════════════════════

mkdir -p "$PROJECT_DIR/logs"

exec > >(tee -a "$LOG_FILE") 2>&1

log "=== Instrumentation Test Run — $TODAY ==="

cd "$PROJECT_DIR"

if [ "$IGNORE_DIRTY_GIT" = false ]; then
    if [ -n "$(git status --porcelain)" ]; then
        log "ERROR: Git repository is not clean. Commit or stash changes before running."
        git status --short
        exit 1
    fi

    git fetch origin && git pull --ff-only origin main
else
    log "=== WARNING: --ignore-dirty-git enabled — skipping git clean check and pull ==="
fi

# ──────────────────────────────────────────────
# Hostname-specific configuration
# ──────────────────────────────────────────────
HOST_SHORT=$(hostname -s)
case "$HOST_SHORT" in
    claw)
        FORCE_XVFB=true
        log "=== Host claw: forcing Xvfb ==="
        ;;
    think)
        if [ -z "${DISPLAY:-}" ] && [ "$FORCE_XVFB" != true ]; then
            SKIP_INSTRUMENTED=true
            log "=== Host think: no DISPLAY and --force-xvfb not set — skipping instrumented tests ==="
        fi
        ;;
    *)
        SKIP_INSTRUMENTED=true
        log "=== Unknown host $HOST_SHORT — skipping instrumented tests ==="
        ;;
esac

if [ "$FORCE_XVFB" = true ]; then
    unset DISPLAY
    IS_REAL_DISPLAY=false
elif [ -z "${DISPLAY:-}" ]; then
    IS_REAL_DISPLAY=false
else
    IS_REAL_DISPLAY=true
fi

if [ "$SKIP_INSTRUMENTED" != true ]; then
    log "=== Preparing display for instrumented tests ==="
    emulator_x11_prepare || exit 1
fi

# ──────────────────────────────────────────────
# API list: hostname-specific or default
# ──────────────────────────────────────────────
DEFAULT_APIS_STRING=$(grep -oP "^zazentimer\.test\.apis=\K.*" "$PROJECT_DIR/gradle.properties" || true)

if [ "$COLD_BOOT" = true ]; then
    SNAPSHOT_FLAG="-no-snapshot"
else
    SNAPSHOT_FLAG=""
fi

IFS=',' read -ra DEFAULT_APIS <<<"$DEFAULT_APIS_STRING"

if [ ${#TARGET_APIS[@]} -gt 0 ]; then
    APIS_TO_RUN=("${TARGET_APIS[@]}")
else
    APIS_TO_RUN=("${DEFAULT_APIS[@]}")
fi

TOTAL_APIS=${#APIS_TO_RUN[@]}

# ──────────────────────────────────────────────
# Unit Tests
# ──────────────────────────────────────────────
log ""
log "========================================="
log "  Unit Tests"
log "========================================="
cd "$PROJECT_DIR"
mkdir -p app/build/test-results/testDebugUnitTest/binary
set +e
./gradlew test 2>&1
UNIT_RESULT=$?
set -e

if [ $UNIT_RESULT -ne 0 ]; then
    log "FAIL: Unit tests failed — skipping instrumented tests"
    print_summary
    exit 1
fi
log "PASS: Unit tests"

# ──────────────────────────────────────────────
# Pre-flight: Build APKs
# ──────────────────────────────────────────────
log ""
log "========================================="
log "  Pre-flight: Building APKs"
log "========================================="
set +e
./gradlew assembleDebug assembleDebugAndroidTest 2>&1
compile_result=$?
set -e

if [ $compile_result -ne 0 ]; then
    log "FAIL: APK build failed — skipping instrumented tests"
    UNIT_RESULT=1
    print_summary
    exit 1
fi
log "PASS: APKs built successfully"

# Verify APKs exist
if [ ! -f "$APP_APK" ]; then
    log "ERROR: App APK not found at $APP_APK"
    exit 1
fi
if [ ! -f "$TEST_APK" ]; then
    log "ERROR: Test APK not found at $TEST_APK"
    exit 1
fi
log "  App APK:  $APP_APK ($(du -h "$APP_APK" | cut -f1))"
log "  Test APK: $TEST_APK ($(du -h "$TEST_APK" | cut -f1))"

# ──────────────────────────────────────────────
# Discover test classes
# ──────────────────────────────────────────────
log ""
log "========================================="
log "  Discovering test classes"
log "========================================="
if ! discover_test_classes; then
    log "ERROR: Failed to discover test classes"
    exit 1
fi

# ──────────────────────────────────────────────
# Instrumented tests
# ──────────────────────────────────────────────

if [ "$SKIP_INSTRUMENTED" = true ]; then
    log ""
    log "========================================="
    log "  Instrumented tests SKIPPED"
    log "========================================="
    print_summary
    exit $UNIT_RESULT
fi

# ──────────────────────────────────────────────
# Resolve device: prefer physical, fallback emulator
# ──────────────────────────────────────────────
log ""
log "========================================="
log "  Resolving test device"
log "========================================="

DEVICE_SERIAL=$(resolve_physical_device) || true
if [ -n "$DEVICE_SERIAL" ]; then
    USE_PHYSICAL_DEVICE=true
    DEVICE_API_LEVEL=$(get_device_api_level "$DEVICE_SERIAL")
    log "Physical device found: $DEVICE_SERIAL (API $DEVICE_API_LEVEL)"
    log "Using physical device for instrumented tests"
else
    USE_PHYSICAL_DEVICE=false
    log "No physical device found — will use emulator(s)"
fi

if [ "$USE_PHYSICAL_DEVICE" = true ]; then
    # ──────────────────────────────────────────
    # Physical device path
    # ──────────────────────────────────────────
    API_IDX=1

    log ""
    log "========================================="
    log "  Physical device ($DEVICE_SERIAL, API $DEVICE_API_LEVEL)"
    log "========================================="

    pass=false
    for attempt in 1 2; do
        run_api_tests "$DEVICE_SERIAL" "$DEVICE_API_LEVEL" false
        if [ "${RESULTS[$DEVICE_API_LEVEL]:-0}" -eq 0 ]; then
            pass=true
            break
        fi
        if [ $attempt -eq 1 ]; then
            log "Attempt 1 failed on physical device — retrying..."
        fi
    done

    if [ "$pass" = true ]; then
        FAILED_APIS=()
    fi

else
    # ──────────────────────────────────────────
    # Emulator path
    # ──────────────────────────────────────────
    API_IDX=0
    for api in "${APIS_TO_RUN[@]}"; do
        API_IDX=$((API_IDX + 1))
        log ""
        log "========================================="
        log "  API $api ($API_IDX/$TOTAL_APIS) — Emulator"
        log "========================================="

        pass=false
        for attempt in 1 2; do
            run_api_tests "emulator-5554" "$api" true
            if [ "${RESULTS[$api]:-0}" -eq 0 ]; then
                pass=true
                break
            fi
            if [ $attempt -eq 1 ]; then
                log_api "API $api attempt 1 failed — retrying..."
            fi
        done

        if [ "$pass" = true ]; then
            _new_failed=()
            for _f in "${FAILED_APIS[@]}"; do
                [ "$_f" != "$api" ] && _new_failed+=("$_f")
            done
            FAILED_APIS=("${_new_failed[@]}")
        fi

        if [ "$CONTINUE_ON_ERROR" = false ] && [ "$pass" = false ]; then
            log ""
            log "FAIL-FAST: Stopping at API $api due to failure"
            break
        fi
    done
fi

# ──────────────────────────────────────────────
# Summary
# ──────────────────────────────────────────────
print_summary

# Auto-tag on green full-matrix run
if [ $UNIT_RESULT -eq 0 ] && [ ${#FAILED_APIS[@]} -eq 0 ]; then
    if [ "$USE_PHYSICAL_DEVICE" = true ]; then
        if [ "$IS_REAL_DISPLAY" = true ] || [ "$IS_REAL_DISPLAY" = false ]; then
            TAG="tested-${TODAY}"
            if git tag -l "$TAG" | grep -q "$TAG"; then
                log "Tag $TAG already exists — skipping"
            else
                log "All tests passed on physical device — tagging HEAD as $TAG"
                git tag "$TAG" HEAD
                git push origin "$TAG"
            fi
        fi
    elif [ "$IS_REAL_DISPLAY" = true ] && [ ${#TARGET_APIS[@]} -eq 0 ]; then
        TAG="tested-${TODAY}"
        if git tag -l "$TAG" | grep -q "$TAG"; then
            log "Tag $TAG already exists — skipping"
        else
            log "All tests passed on real display — tagging HEAD as $TAG"
            git tag "$TAG" HEAD
            git push origin "$TAG"
        fi
    elif [ "$IS_REAL_DISPLAY" = false ]; then
        log "All tests passed but running under Xvfb — skipping auto-tag"
    fi
fi

if [ $UNIT_RESULT -ne 0 ]; then
    exit 1
fi

if [ ${#FAILED_APIS[@]} -gt 0 ]; then
    exit 2
fi

exit 0