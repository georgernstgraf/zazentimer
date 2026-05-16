#!/bin/bash
set -euo pipefail

CONTINUE_ON_ERROR=false
IGNORE_DIRTY_GIT=false
DEBUG_LOG=false
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
    --api)
        if [ -z "${2:-}" ]; then
            echo "ERROR: --api requires an argument (e.g., --api 32 or --api 29,35)"
            exit 1
        fi
        IFS=',' read -ra TARGET_APIS <<<"$2"
        shift 2
        ;;
    *)
        echo "Usage: $0 [--continue-on-error] [--ignore-dirty-git] [--debug] [--api <level>[,<level>...]]"
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
TODAY=$(date +%Y-%m-%d)
LOG_FILE=$PROJECT_DIR/logs/instrumentation-${TODAY}.log

XVFB_PID=""
UNIT_RESULT=0
declare -A RESULTS
FAILED_APIS=()
ERROR_LOGS=()
IS_REAL_DISPLAY=true
API_LOG=""

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

stop_xvfb() {
    if [ -n "${XVFB_PID:-}" ]; then
        kill "$XVFB_PID" 2>/dev/null || true
        wait "$XVFB_PID" 2>/dev/null || true
        XVFB_PID=""
    fi
    rm -f /tmp/.X99-lock /tmp/.X11-unix/X99 2>/dev/null || true
}

start_xvfb() {
    stop_xvfb

    Xvfb :99 -screen 0 1080x1920x24 &
    XVFB_PID=$!

    local waited=0
    while [ $waited -lt 30 ]; do
        if xdpyinfo -display :99 >/dev/null 2>&1; then
            log "Xvfb ready on :99 (PID $XVFB_PID, ${waited}s)"
            export DISPLAY=:99
            return 0
        fi
        if ! kill -0 "$XVFB_PID" 2>/dev/null; then
            log "ERROR: Xvfb failed to start (PID $XVFB_PID is dead)"
            XVFB_PID=""
            return 1
        fi
        sleep 1
        waited=$((waited + 1))
    done

    log "ERROR: Xvfb did not become ready within 30s"
    kill "$XVFB_PID" 2>/dev/null || true
    XVFB_PID=""
    return 1
}

kill_all_emulators() {
    for emu_serial in $(adb devices 2>/dev/null | grep -oP 'emulator-\d+' || true); do
        log_api "Killing leftover emulator $emu_serial"
        adb -s "$emu_serial" emu kill 2>/dev/null || true
    done
    sleep 2
    pkill -9 -f "qemu.*android" 2>/dev/null || true
    pkill -9 -f "emulator.*-avd" 2>/dev/null || true
    sleep 2
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

cleanup() {
    stop_xvfb
    for emu_serial in $(adb devices 2>/dev/null | grep -oP 'emulator-\d+' || true); do
        adb -s "$emu_serial" emu kill 2>/dev/null || true
    done
}
trap cleanup EXIT

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

if [ -z "${DISPLAY:-}" ]; then
    IS_REAL_DISPLAY=false
    log "=== Starting Xvfb on :99 ==="
    if ! start_xvfb; then
        log "ERROR: Failed to start Xvfb — cannot continue"
        exit 1
    fi
else
    IS_REAL_DISPLAY=true
    log "=== DISPLAY=$DISPLAY — using existing display ==="
fi

DEFAULT_APIS_STRING=$(grep "^zazentimer.test.apis=" "$PROJECT_DIR/gradle.properties" | cut -d'=' -f2)
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
else
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
    else
        log "PASS: androidTest compilation"

    # ──────────────────────────────────────────────
    # Helper functions
    # ──────────────────────────────────────────────

    wait_for_emulator() {
        local serial="$1"
        local timeout_sec="${2:-300}"
        local elapsed=0
        local last_progress=-15
        log_api "Waiting for emulator $serial to boot..."
        adb -s "$serial" wait-for-device
        local boot_done=""
        while [ $elapsed -lt $timeout_sec ]; do
            boot_done=$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
            if [ "$boot_done" = "1" ]; then
                log_api "Emulator $serial booted (${elapsed}s)"
                break
            fi
            if [ $((elapsed - last_progress)) -ge 15 ]; then
                log_api "Still waiting for $serial... (${elapsed}s/${timeout_sec}s, boot=${boot_done:-unset})"
                last_progress=$elapsed
            fi
            sleep 5
            elapsed=$((elapsed + 5))
        done

        if [ "$boot_done" != "1" ]; then
            log_api "ERROR: Emulator $serial did not boot within ${timeout_sec}s"
            return 1
        fi

        log_api "Waiting for system services on $serial..."
        local svc_wait=0
        while [ $svc_wait -lt 60 ]; do
            local svc_check
            svc_check=$(adb -s "$serial" shell service check activity 2>/dev/null | tr -d '\r')
            if echo "$svc_check" | grep -q "Service: activity"; then
                log_api "System services ready (${svc_wait}s)"
                break
            fi
            sleep 2
            svc_wait=$((svc_wait + 2))
        done
        if [ $svc_wait -ge 60 ]; then
            log_api "WARNING: activity service not ready after 60s, proceeding anyway"
        fi

        log_api "adb shell svc power stayon true"
        adb -s "$serial" shell svc power stayon true 2>/dev/null || true
        log_api "adb shell input keyevent KEYCODE_WAKEUP"
        adb -s "$serial" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
        log_api "adb shell input keyevent KEYCODE_MENU"
        adb -s "$serial" shell input keyevent KEYCODE_MENU 2>/dev/null || true
        log_api "adb shell input keyevent KEYCODE_HOME"
        adb -s "$serial" shell input keyevent KEYCODE_HOME 2>/dev/null || true
        sleep 5
        return 0
    }

    kill_emulator() {
        local serial="$1"
        log_phase "?" "killing emulator"
        log_api "Killing emulator $serial..."
        log_api "adb emu kill"
        adb -s "$serial" emu kill 2>/dev/null || true
        sleep 5
        local wait_count=0
        while adb devices 2>/dev/null | grep -q "$serial" && [ $wait_count -lt 30 ]; do
            sleep 1
            wait_count=$((wait_count + 1))
        done
    }

    resolve_avd() {
        local api_level=$1
        local avd_list
        avd_list=$($ANDROID_HOME/emulator/emulator -list-avds 2>/dev/null)

        if echo "$avd_list" | grep -qx "test_api${api_level}"; then
            echo "test_api${api_level}"
            return 0
        fi

        if echo "$avd_list" | grep -qx "Medium_Phone_API_${api_level}"; then
            echo "Medium_Phone_API_${api_level}"
            return 0
        fi

        local match
        match=$(echo "$avd_list" | grep "${api_level}" | head -1)
        if [ -n "$match" ]; then
            echo "$match"
            return 0
        fi

        log_api "ERROR: No AVD found for API $api_level. Available:"
        log_api "$avd_list"
        return 1
    }

    clean_device_packages() {
        local serial="$1"
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

    setup_device() {
        local serial="$1"
        log_api "adb shell svc power stayon true"
        adb -s "$serial" shell svc power stayon true 2>/dev/null || true
        log_api "adb shell am force-stop com.google.android.apps.nexuslauncher"
        adb -s "$serial" shell am force-stop com.google.android.apps.nexuslauncher 2>/dev/null || true
        log_api "adb shell input keyevent KEYCODE_WAKEUP"
        adb -s "$serial" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
        log_api "adb shell input keyevent KEYCODE_HOME"
        adb -s "$serial" shell input keyevent KEYCODE_HOME 2>/dev/null || true
        sleep 5
        log_api "adb shell settings put global window_animation_scale 0.0"
        adb -s "$serial" shell settings put global window_animation_scale 0.0 2>/dev/null || true
        log_api "adb shell settings put global transition_animation_scale 0.0"
        adb -s "$serial" shell settings put global transition_animation_scale 0.0 2>/dev/null || true
        log_api "adb shell settings put global animator_duration_scale 0.0"
        adb -s "$serial" shell settings put global animator_duration_scale 0.0 2>/dev/null || true
    }

    clear_logcat() {
        local serial="$1"
        log_api "adb logcat -c"
        adb -s "$serial" logcat -c 2>/dev/null || true
    }

    prepare_isolated_run() {
        local api_level=$1
        preserve_crash_dbs "$api_level"
        kill_all_emulators
        if [ "$IS_REAL_DISPLAY" = false ]; then
            log_api "=== Restarting Xvfb for API $api_level ==="
            if ! start_xvfb; then
                log_api "FAIL: Xvfb failed to start for API $api_level"
                return 1
            fi
        fi
        return 0
    }

    run_gradle_test() {
        local api_level=$1
        local serial="emulator-5554"
        exec_api_log "$api_level"

        if ! prepare_isolated_run "$api_level"; then
            RESULTS[$api_level]=1
            FAILED_APIS+=("$api_level")
            ERROR_LOGS+=("API $api_level: Xvfb failed to start")
            return
        fi

        log_phase "$api_level" "resolving AVD"
        local avd_name
        avd_name=$(resolve_avd "$api_level") || {
            log_api "FAIL: Could not find AVD for API $api_level"
            RESULTS[$api_level]=1
            FAILED_APIS+=("$api_level")
            ERROR_LOGS+=("API $api_level: No AVD found")
            return
        }
        local result=0

        log_api ""
        log_api "========================================="
        log_api "  API $api_level — Starting emulator ($avd_name)"
        log_api "========================================="
        log_phase "$api_level" "starting emulator (AVD $avd_name)"

        $ANDROID_HOME/emulator/emulator \
            -avd "$avd_name" \
            -no-snapshot \
            -gpu swiftshader_indirect \
            $([ "$IS_REAL_DISPLAY" = false ] && echo "-noaudio") \
            -no-boot-anim \
            -memory 2048 >> "$API_LOG" 2>&1 &
        local emu_pid=$!
        sleep 2
        log_api "Emulator started (PID $emu_pid, AVD $avd_name)"

        log_phase "$api_level" "waiting for boot"
        if ! wait_for_emulator "$serial"; then
            log_api "FAIL: API $api_level emulator did not boot"
            RESULTS[$api_level]=1
            FAILED_APIS+=("$api_level")
            ERROR_LOGS+=("API $api_level: Emulator failed to boot")
            kill_emulator "$serial"
            return
        fi

        log_phase "$api_level" "cleaning packages"
        clean_device_packages "$serial"
        dismiss_anr_dialog "$serial"

        setup_device "$serial"
        clear_logcat "$serial"

        log_phase "$api_level" "running tests"
        log_api ""
        log_api "========================================="
        log_api "  API $api_level — Running instrumented tests"
        log_api "========================================="

        set +e
        cd "$PROJECT_DIR"
        ./gradlew connectedDebugAndroidTest 2>&1 | tee -a "$API_LOG"
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

        kill_emulator "$serial"
    }


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
    fi
    fi

# ──────────────────────────────────────────────
# Summary
# ──────────────────────────────────────────────
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
