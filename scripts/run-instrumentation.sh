#!/bin/bash
set -euo pipefail

CONTINUE_ON_ERROR=false
IGNORE_DIRTY_GIT=false
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
    --api)
        if [ -z "${2:-}" ]; then
            echo "ERROR: --api requires an argument (e.g., --api 32 or --api 29,35)"
            exit 1
        fi
        IFS=',' read -ra TARGET_APIS <<<"$2"
        shift 2
        ;;
    *)
        echo "Usage: $0 [--continue-on-error] [--ignore-dirty-git] [--api <level>[,<level>...]]"
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
    for emu_serial in $(adb devices 2>/dev/null | grep -oP 'emulator-\d+' || true); do
        adb -s "$emu_serial" emu kill 2>/dev/null || true
    done
}
trap cleanup EXIT

mkdir -p "$PROJECT_DIR/logs"

echo "=== Instrumentation Test Run — $TODAY ==="
echo "Logging to $LOG_FILE"

cd "$PROJECT_DIR"

if [ "$IGNORE_DIRTY_GIT" = false ]; then
    if [ -n "$(git status --porcelain)" ]; then
        echo "ERROR: Git repository is not clean. Commit or stash changes before running."
        git status --short
        exit 1
    fi

    git fetch origin && git pull --ff-only origin main
else
    echo "=== WARNING: --ignore-dirty-git enabled — skipping git clean check and pull ==="
fi

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

DEFAULT_APIS_STRING=$(grep "^zazentimer.test.apis=" "$PROJECT_DIR/gradle.properties" | cut -d'=' -f2)
IFS=',' read -ra DEFAULT_APIS <<<"$DEFAULT_APIS_STRING"
GRADLE_MAX_API=$(grep "^zazentimer.test.gradleMaxApi=" "$PROJECT_DIR/gradle.properties" | cut -d'=' -f2)

if [ ${#TARGET_APIS[@]} -gt 0 ]; then
    APIS_TO_RUN=("${TARGET_APIS[@]}")
else
    APIS_TO_RUN=("${DEFAULT_APIS[@]}")
fi

# ──────────────────────────────────────────────
# Unit Tests
# ──────────────────────────────────────────────
echo ""
echo "========================================="
echo "  Unit Tests"
echo "========================================="
cd "$PROJECT_DIR"
mkdir -p app/build/test-results/testDebugUnitTest/binary
set +e
./gradlew test
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

        echo "ERROR: No AVD found for API $api_level. Available:" >&2
        echo "$avd_list" >&2
        return 1
    }

    clean_device_packages() {
        local serial="$1"
        echo "Cleaning stale packages on $serial..."
        for pkg in \
            de.gaffga.android.zazentimer \
            de.gaffga.android.zazentimer.test \
            at.priv.graf.zazentimer \
            at.priv.graf.zazentimer.test; do
            adb -s "$serial" uninstall "$pkg" >/dev/null 2>&1 || true
            adb -s "$serial" shell pm uninstall --user 0 "$pkg" >/dev/null 2>&1 || true
        done
        adb -s "$serial" shell cmd package install-existing at.priv.graf.zazentimer >/dev/null 2>&1 || true
        adb -s "$serial" shell cmd package install-existing at.priv.graf.zazentimer.test >/dev/null 2>&1 || true
        adb -s "$serial" uninstall at.priv.graf.zazentimer >/dev/null 2>&1 || true
        adb -s "$serial" uninstall at.priv.graf.zazentimer.test >/dev/null 2>&1 || true
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
            $([ "$IS_REAL_DISPLAY" = false ] && echo "-noaudio") \
            -no-boot-anim \
            -memory 2048 &
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
        adb -s "$serial" shell am force-stop com.google.android.apps.nexuslauncher 2>/dev/null || true
        adb -s "$serial" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
        adb -s "$serial" shell input keyevent KEYCODE_HOME 2>/dev/null || true
        sleep 5
        adb -s "$serial" shell settings put global window_animation_scale 0.0 2>/dev/null || true
        adb -s "$serial" shell settings put global transition_animation_scale 0.0 2>/dev/null || true
        adb -s "$serial" shell settings put global animator_duration_scale 0.0 2>/dev/null || true

        echo ""
        echo "========================================="
        echo "  API $api_level — Running instrumented tests"
        echo "========================================="

        set +e
        cd "$PROJECT_DIR"
        ./gradlew connectedDebugAndroidTest
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
        ./gradlew assembleDebug assembleDebugAndroidTest
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
            $([ "$IS_REAL_DISPLAY" = false ] && echo "-noaudio") \
            -no-boot-anim \
            -memory 2048 &
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
        sleep 8
        dismiss_anr_dialog "$serial"
        adb -s "$serial" shell svc power stayon true 2>/dev/null || true
        adb -s "$serial" shell am force-stop com.google.android.apps.nexuslauncher 2>/dev/null || true
        adb -s "$serial" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
        adb -s "$serial" shell input keyevent KEYCODE_HOME 2>/dev/null || true
        sleep 5
        adb -s "$serial" shell settings put global window_animation_scale 0.0 2>/dev/null || true
        adb -s "$serial" shell settings put global transition_animation_scale 0.0 2>/dev/null || true
        adb -s "$serial" shell settings put global animator_duration_scale 0.0 2>/dev/null || true

        echo ""
        echo "========================================="
        echo "  API $api_level — Installing APKs"
        echo "========================================="
        set +e
        adb -s "$serial" install -r app/build/outputs/apk/debug/app-debug.apk
        local install_app=$?
        sleep 10
        local install_test=1
        for install_attempt in 1 2; do
            adb -s "$serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
            install_test=$?
            if [ $install_test -eq 0 ]; then
                break
            fi
            echo "Test APK install failed attempt $install_attempt — retrying after delay..."
            sleep 10
        done
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

            local process_crashed
            process_crashed=$(echo "$instrument_output" | grep -c "Process crashed" || true)
            local error_in_tests
            error_in_tests=$(echo "$instrument_output" | grep -c '^Error in ' || true)
            failures=$(echo "$instrument_output" | grep -oP 'Failures:\s*\K\d+' || true)
            local combined_failures=$((${failures:-0} + ${error_in_tests:-0}))

            local any_test_output
            any_test_output=$(echo "$instrument_output" | grep -cE '^[a-z]+\.(at\.|priv\.|graf\.)' || true)
            if [ "${process_crashed:-0}" -eq 0 ] && [ "$combined_failures" -eq 0 ]; then
                if [ "$result" -eq 0 ] || ([ "$result" -eq 143 ] && [ "${any_test_output:-0}" -gt 0 ]); then
                    break
                fi
            fi

            if [ "${process_crashed:-0}" -gt 0 ]; then
                echo "Process crashed detected — not retrying (fatal)"
                break
            fi

            local focus_errors
            focus_errors=$(echo "$instrument_output" | grep -c "RootViewWithoutFocusException\|has-window-focus=false" || true)
            if [ "$focus_errors" -gt 0 ] && [ "$test_attempt" -eq 1 ]; then
                echo "Focus errors detected — retrying with wakeup..."
                adb -s "$serial" shell svc power stayon true 2>/dev/null || true
                adb -s "$serial" shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
                adb -s "$serial" shell input keyevent KEYCODE_HOME 2>/dev/null || true
                adb -s "$serial" shell settings put global window_animation_scale 0.0 2>/dev/null || true
                adb -s "$serial" shell settings put global transition_animation_scale 0.0 2>/dev/null || true
                adb -s "$serial" shell settings put global animator_duration_scale 0.0 2>/dev/null || true
                sleep 5
            else
                break
            fi
        done

        local error_in_tests_final
        error_in_tests_final=$(echo "$instrument_output" | grep -c '^Error in ' || true)
        local combined=$((${failures:-0} + ${error_in_tests_final:-0}))

        local any_test_output_final
        any_test_output_final=$(echo "$instrument_output" | grep -cE '^[a-z]+\.(at\.|priv\.|graf\.)' || true)
        local sigterm_ok=false
        if [ "$result" -eq 143 ] && [ "${any_test_output_final:-0}" -gt 0 ]; then
            sigterm_ok=true
        fi

        if [ "${process_crashed:-0}" -gt 0 ]; then
            echo "FAIL: API $api_level test process crashed"
            RESULTS[$api_level]=1
            FAILED_APIS+=("$api_level")
            ERROR_LOGS+=("API $api_level: Process crashed")
        elif [ "${any_test_output_final:-0}" -eq 0 ] && [ "${combined:-0}" -eq 0 ]; then
            echo "FAIL: API $api_level no test output (empty result)"
            RESULTS[$api_level]=1
            FAILED_APIS+=("$api_level")
            ERROR_LOGS+=("API $api_level: no test output")
        elif [ "$combined" -ne 0 ] || { [ "$result" -ne 0 ] && [ "$sigterm_ok" = false ]; }; then
            echo "FAIL: API $api_level am instrument failed (exit=$result, failures=$combined)"
            RESULTS[$api_level]=1
            FAILED_APIS+=("$api_level")
            ERROR_LOGS+=("API $api_level: am instrument exit=$result failures=$combined")
        else
            echo "PASS: API $api_level"
            RESULTS[$api_level]=0
        fi

        kill_emulator "$serial"
    }

    # Note: The Gradle test runner is used for API ≤ zazentimer.test.gradleMaxApi (currently 30).
    # For higher APIs (≥ 31) we use am instrument because PreFlightRule is applied in
    # HiltTestRunner.onStart(), which integrates seamlessly with the am instrument path.
    # If desired, the threshold can be raised to 32 to use Gradle runner + PreFlightRule
    # on API 31-32 by setting zazentimer.test.gradleMaxApi=32 in gradle.properties.
    get_test_method() {
        local api=$1
        if [ "$api" -le "$GRADLE_MAX_API" ]; then
            echo "gradle"
        else
            echo "am_instrument"
        fi
    }

    for api in "${APIS_TO_RUN[@]}"; do
        method=$(get_test_method "$api")
        pass=false
        for attempt in 1 2; do
            if [ "$method" = "gradle" ]; then
                run_gradle_test "$api"
            else
                run_am_instrument_test "$api"
            fi
            if [ "${RESULTS[$api]:-0}" -eq 0 ]; then
                pass=true
                break
            fi
            if [ $attempt -eq 1 ]; then
                echo "API $api attempt 1 failed — retrying..."
            fi
        done

        if [ "$CONTINUE_ON_ERROR" = false ] && [ "$pass" = false ]; then
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
    echo "Target APIs: all (29-36)"
fi
echo "Display: $([ "$IS_REAL_DISPLAY" = true ] && echo 'real' || echo 'Xvfb')"
echo ""
echo "Unit Tests: $([ $UNIT_RESULT -eq 0 ] && echo 'PASS' || echo 'FAIL')"
for api in "${APIS_TO_RUN[@]}"; do
    if [ -z "${RESULTS[$api]+isset}" ]; then
        echo "API $api:    SKIPPED"
    elif [ "${RESULTS[$api]}" -ne 0 ]; then
        echo "API $api:    FAIL"
    else
        echo "API $api:    PASS"
    fi
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
