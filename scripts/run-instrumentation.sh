#!/bin/bash
set -euo pipefail

CONTINUE_ON_ERROR=false
TARGET_APIS=()

while [[ $# -gt 0 ]]; do
    case "$1" in
    --continue-on-error)
        CONTINUE_ON_ERROR=true
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
        echo "Usage: $0 [--continue-on-error] [--api <level>[,<level>...]]"
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

cleanup() {
    if [ -n "${XVFB_PID:-}" ]; then
        kill "$XVFB_PID" 2>/dev/null || true
        wait "$XVFB_PID" 2>/dev/null || true
    fi
    adb devices | grep -q "emulator" && adb -s emulator-5554 emu kill 2>/dev/null || true
}
trap cleanup EXIT

mkdir -p "$PROJECT_DIR/logs"

echo "=== Instrumentation Test Run — $TODAY ==="
echo "Logging to $LOG_FILE"

cd "$PROJECT_DIR"

if [ -n "$(git status --porcelain)" ]; then
    echo "ERROR: Git repository is not clean. Commit or stash changes before running."
    git status --short
    exit 1
fi

git fetch origin && git pull --ff-only origin main

if [ -z "${DISPLAY:-}" ]; then
    echo "=== Starting Xvfb on :99 ==="
    Xvfb :99 -screen 0 1080x1920x24 &
    XVFB_PID=$!
    export DISPLAY=:99
    IS_REAL_DISPLAY=false
    sleep 2
    echo "Xvfb started (PID $XVFB_PID)"
else
    echo "=== DISPLAY=$DISPLAY — using existing display ==="
fi

if [ ${#TARGET_APIS[@]} -gt 0 ]; then
    APIS_TO_RUN=("${TARGET_APIS[@]}")
else
    APIS_TO_RUN=(29 30 31 32 33 34 35)
fi

# ──────────────────────────────────────────────
# Unit Tests
# ──────────────────────────────────────────────
echo ""
echo "========================================="
echo "  Unit Tests"
echo "========================================="
cd "$PROJECT_DIR"
set +e
./gradlew test --no-daemon
UNIT_RESULT=$?
set -e

if [ $UNIT_RESULT -ne 0 ]; then
    echo "FAIL: Unit tests failed — skipping instrumented tests"
else
    echo "PASS: Unit tests"

    wait_for_emulator() {
        local serial="$1"
        local timeout_sec="${2:-300}"
        local elapsed=0
        echo "Waiting for emulator $serial to boot..."
        adb -s "$serial" wait-for-device
        while [ $elapsed -lt $timeout_sec ]; do
            local boot_done
            boot_done=$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
            if [ "$boot_done" = "1" ]; then
                echo "Emulator $serial booted (${elapsed}s)"
                adb -s "$serial" shell svc power stayon true
                adb -s "$serial" shell input keyevent KEYCODE_WAKEUP
                adb -s "$serial" shell input keyevent KEYCODE_MENU
                adb -s "$serial" shell input keyevent KEYCODE_HOME
                sleep 5
                return 0
            fi
            sleep 5
            elapsed=$((elapsed + 5))
        done
        echo "ERROR: Emulator $serial did not boot within ${timeout_sec}s"
        return 1
    }

    kill_emulator() {
        local serial="$1"
        echo "Killing emulator $serial..."
        adb -s "$serial" emu kill 2>/dev/null || true
        sleep 2
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

        echo "ERROR: No AVD found for API $api_level. Available:" >&2
        echo "$avd_list" >&2
        return 1
    }

    clean_device_packages() {
        local serial="$1"
        echo "Cleaning stale packages on $serial..."
        for pkg in \
            de.gaffga.android.zazentimer \
            de.gaffga.android.zazentimer.test; do
            adb -s "$serial" uninstall "$pkg" >/dev/null 2>&1 || true
        done
    }

    dismiss_anr_dialog() {
        local serial="$1"
        for attempt in 1 2 3; do
            local anr_window
            anr_window=$(adb -s "$serial" shell "dumpsys window windows" 2>/dev/null | grep -c "Application Error\|isn't responding\|is not responding" || true)
            if [ "$anr_window" -eq 0 ] 2>/dev/null; then
                return 0
            fi
            echo "ANR dialog detected on $serial (attempt $attempt) — dismissing..."
            adb -s "$serial" shell input keyevent KEYCODE_DPAD_RIGHT 2>/dev/null || true
            adb -s "$serial" shell input keyevent KEYCODE_ENTER 2>/dev/null || true
            sleep 3
        done
    }

    run_gradle_test() {
        local api_level=$1
        local serial="emulator-5554"
        local avd_name
        avd_name=$(resolve_avd "$api_level") || {
            echo "FAIL: Could not find AVD for API $api_level"
            RESULTS[$api_level]=1
            FAILED_APIS+=("$api_level")
            ERROR_LOGS+=("API $api_level: No AVD found")
            return
        }
        local result=0

        echo ""
        echo "========================================="
        echo "  API $api_level — Starting emulator ($avd_name)"
        echo "========================================="

        $ANDROID_HOME/emulator/emulator \
            -avd "$avd_name" \
            -no-snapshot \
            -gpu swiftshader_indirect \
            -noaudio \
            -no-boot-anim \
            -memory 4096 &
        sleep 2

        if ! wait_for_emulator "$serial"; then
            echo "FAIL: API $api_level emulator did not boot"
            RESULTS[$api_level]=1
            FAILED_APIS+=("$api_level")
            ERROR_LOGS+=("API $api_level: Emulator failed to boot")
            kill_emulator "$serial"
            return
        fi

        clean_device_packages "$serial"
        dismiss_anr_dialog "$serial"
        adb -s "$serial" shell svc power stayon true 2>/dev/null || true
        adb -s "$serial" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
        adb -s "$serial" shell input keyevent KEYCODE_HOME 2>/dev/null || true
        sleep 5

        echo ""
        echo "========================================="
        echo "  API $api_level — Running instrumented tests"
        echo "========================================="

        set +e
        cd "$PROJECT_DIR"
        ./gradlew connectedDebugAndroidTest --no-daemon
        result=$?
        set -e

        if [ $result -ne 0 ]; then
            echo "FAIL: API $api_level tests failed (exit $result)"
            RESULTS[$api_level]=$result
            FAILED_APIS+=("$api_level")
            ERROR_LOGS+=("API $api_level: connectedDebugAndroidTest exit code $result")
        else
            echo "PASS: API $api_level"
            RESULTS[$api_level]=0
        fi

        kill_emulator "$serial"
    }

    run_am_instrument_test() {
        local api_level=$1
        local serial="emulator-5554"
        local result=0

        echo ""
        echo "========================================="
        echo "  API $api_level — Building APKs"
        echo "========================================="
        cd "$PROJECT_DIR"
        set +e
        ./gradlew assembleDebug assembleDebugAndroidTest --no-daemon
        local build_result=$?
        set -e

        if [ $build_result -ne 0 ]; then
            echo "FAIL: API $api_level build failed"
            RESULTS[$api_level]=$build_result
            FAILED_APIS+=("$api_level")
            ERROR_LOGS+=("API $api_level: Build failed (exit $build_result)")
            return
        fi

        local avd_name
        avd_name=$(resolve_avd "$api_level") || {
            echo "FAIL: Could not find AVD for API $api_level"
            RESULTS[$api_level]=1
            FAILED_APIS+=("$api_level")
            ERROR_LOGS+=("API $api_level: No AVD found")
            return
        }

        echo ""
        echo "========================================="
        echo "  API $api_level — Starting emulator ($avd_name)"
        echo "========================================="

        $ANDROID_HOME/emulator/emulator \
            -avd "$avd_name" \
            -no-snapshot \
            -gpu swiftshader_indirect \
            -noaudio \
            -no-boot-anim \
            -memory 4096 &
        sleep 2

        if ! wait_for_emulator "$serial"; then
            echo "FAIL: API $api_level emulator did not boot"
            RESULTS[$api_level]=1
            FAILED_APIS+=("$api_level")
            ERROR_LOGS+=("API $api_level: Emulator failed to boot")
            kill_emulator "$serial"
            return
        fi

        clean_device_packages "$serial"
        dismiss_anr_dialog "$serial"
        adb -s "$serial" shell svc power stayon true 2>/dev/null || true
        adb -s "$serial" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
        adb -s "$serial" shell input keyevent KEYCODE_HOME 2>/dev/null || true
        sleep 5

        echo ""
        echo "========================================="
        echo "  API $api_level — Installing APKs"
        echo "========================================="
        set +e
        adb -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
        local install_app=$?
        adb -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
        local install_test=$?
        set -e

        if [ $install_app -ne 0 ] || [ $install_test -ne 0 ]; then
            echo "FAIL: API $api_level APK installation failed (app=$install_app, test=$install_test)"
            RESULTS[$api_level]=1
            FAILED_APIS+=("$api_level")
            ERROR_LOGS+=("API $api_level: APK install failed (app=$install_app, test=$install_test)")
            kill_emulator "$serial"
            return
        fi

        echo ""
        echo "========================================="
        echo "  API $api_level — Running instrumented tests"
        echo "========================================="

        local instrument_output
        local failures=1
        for test_attempt in 1 2; do
            set +e
            instrument_output=$(adb -s "$serial" shell am instrument -w \
                at.priv.graf.zazentimer.test/at.priv.graf.zazentimer.HiltTestRunner 2>&1)
            result=$?
            echo "$instrument_output"
            set -e

            failures=$(echo "$instrument_output" | grep -oP 'Failures:\s*\K\d+' || true)
            if [ "${failures:-0}" -eq 0 ] && [ "$result" -eq 0 ]; then
                break
            fi

            local focus_errors
            focus_errors=$(echo "$instrument_output" | grep -c "RootViewWithoutFocusException\|has-window-focus=false" || true)
            if [ "$focus_errors" -gt 0 ] && [ "$test_attempt" -eq 1 ]; then
                echo "Focus errors detected — retrying with wakeup..."
                adb -s "$serial" shell svc power stayon true 2>/dev/null || true
                adb -s "$serial" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
                adb -s "$serial" shell input keyevent KEYCODE_HOME 2>/dev/null || true
                sleep 5
            else
                break
            fi
        done

        if [ "$result" -ne 0 ] || [ "${failures:-0}" -ne 0 ]; then
            echo "FAIL: API $api_level am instrument failed (exit=$result, failures=${failures:-unknown})"
            RESULTS[$api_level]=1
            FAILED_APIS+=("$api_level")
            ERROR_LOGS+=("API $api_level: am instrument exit=$result failures=${failures:-unknown}")
        else
            echo "PASS: API $api_level"
            RESULTS[$api_level]=0
        fi

        kill_emulator "$serial"
    }

    for api in "${APIS_TO_RUN[@]}"; do
        if [ "$api" -le 32 ]; then
            run_gradle_test "$api"
        else
            run_am_instrument_test "$api"
        fi

        if [ "$CONTINUE_ON_ERROR" = false ] && [ "${RESULTS[$api]:-0}" -ne 0 ]; then
            echo ""
            echo "FAIL-FAST: Stopping at API $api due to failure"
            break
        fi
    done
fi

# ──────────────────────────────────────────────
# Summary
# ──────────────────────────────────────────────
echo ""
echo "========================================="
echo "  Test Summary — $TODAY"
echo "========================================="
echo "Mode: $([ "$CONTINUE_ON_ERROR" = true ] && echo 'continue-on-error' || echo 'fail-fast')"
if [ ${#TARGET_APIS[@]} -gt 0 ]; then
    echo "Target APIs: ${TARGET_APIS[*]}"
else
    echo "Target APIs: all (29-35)"
fi
echo "Display: $([ "$IS_REAL_DISPLAY" = true ] && echo 'real' || echo 'Xvfb')"
echo ""
echo "Unit Tests: $([ $UNIT_RESULT -eq 0 ] && echo 'PASS' || echo 'FAIL')"
for api in "${APIS_TO_RUN[@]}"; do
    status="PASS"
    if [ "${RESULTS[$api]:-1}" -ne 0 ]; then
        status="FAIL"
    fi
    echo "API $api:    $status"
done
echo "========================================="

# Auto-tag on green full-matrix run
if [ $UNIT_RESULT -eq 0 ] && [ ${#FAILED_APIS[@]} -eq 0 ]; then
    if [ "$IS_REAL_DISPLAY" = true ] && [ ${#TARGET_APIS[@]} -eq 0 ]; then
        TAG="tested-${TODAY}"
        if git tag -l "$TAG" | grep -q "$TAG"; then
            echo "Tag $TAG already exists — skipping"
        else
            echo "All tests passed on real display — tagging HEAD as $TAG"
            git tag "$TAG" HEAD
            git push origin "$TAG"
        fi
    elif [ "$IS_REAL_DISPLAY" = false ]; then
        echo "All tests passed but running under Xvfb — skipping auto-tag"
    fi
fi

if [ $UNIT_RESULT -ne 0 ]; then
    exit 1
fi

if [ ${#FAILED_APIS[@]} -gt 0 ]; then
    exit 2
fi

exit 0
