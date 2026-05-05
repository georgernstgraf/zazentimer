#!/bin/bash
set -euo pipefail

export ANDROID_HOME=/opt/android-sdk
export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

XVFB_PID=""
API29_RESULT=0
API35_RESULT=0

cleanup() {
    if [ -n "$XVFB_PID" ]; then
        kill "$XVFB_PID" 2>/dev/null || true
        wait "$XVFB_PID" 2>/dev/null || true
    fi
    adb devices | grep -q "emulator" && adb -s emulator-5554 emu kill 2>/dev/null || true
}
trap cleanup EXIT

if [ -z "${DISPLAY:-}" ]; then
    echo "=== DISPLAY not set — starting Xvfb on :99 ==="
    Xvfb :99 -screen 0 1080x1920x24 &
    XVFB_PID=$!
    export DISPLAY=:99
    sleep 2
    echo "Xvfb started (PID $XVFB_PID)"
else
    echo "=== DISPLAY=$DISPLAY — using existing display ==="
fi

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

# ──────────────────────────────────────────────
# API 29 — Gradle connectedDebugAndroidTest
# ──────────────────────────────────────────────
echo ""
echo "========================================="
echo "  API 29 — Starting emulator (test_api29)"
echo "========================================="
$ANDROID_HOME/emulator/emulator \
    -avd test_api29 \
    -no-snapshot \
    -gpu swiftshader_indirect \
    -noaudio \
    -no-boot-anim \
    -memory 4096 &
API29_SERIAL="emulator-5554"

if ! wait_for_emulator "$API29_SERIAL"; then
    echo "FAIL: API 29 emulator did not boot"
    API29_RESULT=1
else
    echo ""
    echo "========================================="
    echo "  API 29 — Running instrumented tests"
    echo "========================================="
    set +e
    cd "$PROJECT_DIR"
    ./gradlew connectedDebugAndroidTest --no-daemon
    API29_RESULT=$?
    set -e
fi

kill_emulator "$API29_SERIAL"

# ──────────────────────────────────────────────
# API 35 — am instrument (UTP workaround)
# ──────────────────────────────────────────────
echo ""
echo "========================================="
echo "  API 35 — Building APKs"
echo "========================================="
cd "$PROJECT_DIR"
./gradlew assembleDebug assembleDebugAndroidTest --no-daemon

echo ""
echo "========================================="
echo "  API 35 — Starting emulator (test_api35)"
echo "========================================="
$ANDROID_HOME/emulator/emulator \
    -avd test_api35 \
    -no-snapshot \
    -gpu swiftshader_indirect \
    -noaudio \
    -no-boot-anim \
    -memory 4096 \
    -target google_apis &
API35_SERIAL="emulator-5554"

if ! wait_for_emulator "$API35_SERIAL"; then
    echo "FAIL: API 35 emulator did not boot"
    API35_RESULT=1
else
    echo ""
    echo "========================================="
    echo "  API 35 — Installing APKs"
    echo "========================================="
    set +e
    adb -s "$API35_SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
    INSTALL_APP=$?
    adb -s "$API35_SERIAL" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
    INSTALL_TEST=$?
    set -e

    if [ $INSTALL_APP -ne 0 ] || [ $INSTALL_TEST -ne 0 ]; then
        echo "FAIL: APK installation failed (app=$INSTALL_APP, test=$INSTALL_TEST)"
        API35_RESULT=1
    else
        echo ""
        echo "========================================="
        echo "  API 35 — Running instrumented tests"
        echo "========================================="
        set +e
        adb -s "$API35_SERIAL" shell am instrument -w \
            at.priv.graf.zazentimer.test/at.priv.graf.zazentimer.HiltTestRunner
        API35_RESULT=$?
        set -e
    fi
fi

kill_emulator "$API35_SERIAL"

# ──────────────────────────────────────────────
# Summary
# ──────────────────────────────────────────────
echo ""
echo "========================================="
echo "  Stage 2 Summary"
echo "========================================="
echo "API 29: $([ $API29_RESULT -eq 0 ] && echo 'PASS' || echo 'FAIL')"
echo "API 35: $([ $API35_RESULT -eq 0 ] && echo 'PASS' || echo 'FAIL')"
echo "========================================="

if [ $API29_RESULT -ne 0 ] || [ $API35_RESULT -ne 0 ]; then
    exit 1
fi

exit 0
