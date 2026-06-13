#!/bin/bash
set -euo pipefail

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

if [ -n "${ANDROID_HOME:-}" ] && [ -d "${ANDROID_HOME}" ]; then
    echo "=== Using ANDROID_HOME=$ANDROID_HOME ==="
elif [ -n "${ANDROID_SDK_ROOT:-}" ] && [ -d "${ANDROID_SDK_ROOT}" ]; then
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
    if [ ${#TARGET_APIS[@]} -gt 0 ]; then
        log "Target APIs: ${TARGET_APIS[*]}"
    else
        log "Target APIs: all (23-36)"
    fi
    log "Display: $([ "$IS_REAL_DISPLAY" = true ] && echo 'real' || echo 'Xvfb')"
    log ""
    log "Unit Tests: $([ $UNIT_RESULT -eq 0 ] && echo 'PASS' || echo 'FAIL')"
    for api in "${APIS_TO_RUN[@]}"; do
        if [ -z "${RESULTS[$api]+isset}" ]; then
            log "API $api:    SKIPPED"
        elif [ "${RESULTS[$api]}" -ne 0 ]; then
            log "API $api:    FAIL"
        else
            log "API $api:    PASS"
        fi
    done
    log "========================================="
}

cleanup() {
    emulator_kill_all
    if [ -n "${EMULATOR_XVFB_PID:-}" ] && kill -0 "$EMULATOR_XVFB_PID" 2>/dev/null; then
        kill "$EMULATOR_XVFB_PID" 2>/dev/null || true
    fi
    rm -f /tmp/.X99-lock /tmp/.X11-unix/X99 2>/dev/null || true
}
trap cleanup EXIT

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

run_gradle_test() {
    local api_level=$1
    local serial="emulator-5554"
    exec_api_log "$api_level"

    preserve_crash_dbs "$api_level"
    emulator_kill_all

    log_phase "$api_level" "resolving AVD"
    local avd_name
    avd_name=$(emulator_resolve_avd "$api_level") || {
        log_api "SKIP: No AVD for API $api_level — skipping"
        return
    }
    local result=0

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
        return
    fi

    log_phase "$api_level" "cleaning packages"
    clean_device_packages "$serial" "$api_level"
    dismiss_anr_dialog "$serial"

    emulator_configure_system "$serial"
    clear_logcat "$serial"

    log_api "Pushing backup fixture to /data/local/tmp/..."
    adb -s "$serial" push "$PROJECT_DIR/app/src/test/resources/backups/zentimer_backup_room_v2.zip" /data/local/tmp/ 2>/dev/null || log_api "WARNING: backup fixture zip not found — skipping push"

    log_phase "$api_level" "running tests"
    log_api ""
    log_api "========================================="
    log_api "  API $api_level — Running instrumented tests"
    log_api "========================================="

    set +e
    cd "$PROJECT_DIR"
    stdbuf -oL ./gradlew connectedDebugAndroidTest 2>&1 | tee -a "$API_LOG"
    result=$?
    set -e

    if [ $result -ne 0 ]; then
        log_api "FAIL: API $api_level tests failed (exit $result)"
        RESULTS[$api_level]=$result
        FAILED_APIS+=("$api_level")
        ERROR_LOGS+=("API $api_level: connectedDebugAndroidTest exit code $result")
        dump_logcat "$api_level" "$serial"
    else
        log_api "PASS: API $api_level"
        RESULTS[$api_level]=0
        if [ "$DEBUG_LOG" = true ]; then
            dump_logcat "$api_level" "$serial"
        fi
    fi

    emulator_kill_serial "$serial"
}

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

# Set snapshot flag based on --cold-boot
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
# Pre-flight: Compile androidTest sources
# ──────────────────────────────────────────────
log ""
log "========================================="
log "  Pre-flight: Compiling androidTest"
log "========================================="
set +e
./gradlew compileDebugAndroidTestKotlin 2>&1
compile_result=$?
set -e

if [ $compile_result -ne 0 ]; then
    log "FAIL: androidTest compilation failed — skipping instrumented tests"
    UNIT_RESULT=1
    print_summary
    exit 1
fi
log "PASS: androidTest compilation"

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

API_IDX=0
for api in "${APIS_TO_RUN[@]}"; do
    API_IDX=$((API_IDX + 1))
    log ""
    log "========================================="
    log "  API $api ($API_IDX/$TOTAL_APIS)"
    log "========================================="

    pass=false
    for attempt in 1 2; do
        run_gradle_test "$api"
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

# ──────────────────────────────────────────────
# Summary
# ──────────────────────────────────────────────
print_summary

# Auto-tag on green full-matrix run
if [ $UNIT_RESULT -eq 0 ] && [ ${#FAILED_APIS[@]} -eq 0 ]; then
    if [ "$IS_REAL_DISPLAY" = true ] && [ ${#TARGET_APIS[@]} -eq 0 ]; then
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
