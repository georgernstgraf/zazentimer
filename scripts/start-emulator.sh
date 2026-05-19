#!/usr/bin/env bash
set -euo pipefail

# ──────────────────────────────────────────────
# start-emulator.sh — Start Xvfb (if needed) and an Android emulator
#                     for headless Prisma DB extraction.
#
# Output:
#   Exports ANDROID_SERIAL=emulator-5554
#   Writes /tmp/zazentimer-emulator.pid
#   Writes /tmp/zazentimer-xvfb.pid (only if Xvfb was started)
#   Writes /tmp/zazentimer-emulator-avd
#
# Usage:
#   source scripts/start-emulator.sh [api_level]
#
# Default API: 35
# ──────────────────────────────────────────────

COLD=false
API_LEVEL=""

while [[ $# -gt 0 ]]; do
    case "$1" in
    --cold)
        COLD=true
        shift
        ;;
    *)
        API_LEVEL="$1"
        shift
        ;;
    esac
done

API_LEVEL="${API_LEVEL:-35}"
SNAPSHOT_FLAG="-no-snapshot-save"
if [ "$COLD" = true ]; then
    SNAPSHOT_FLAG="-no-snapshot"
fi

AVD_NAME=""
ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"

# ──────────────────────────────────────────────
# Resolve AVD
# ──────────────────────────────────────────────
resolve_avd() {
    local avd_list
    avd_list=$("$ANDROID_HOME/emulator/emulator" -list-avds 2>/dev/null || true)

    if echo "$avd_list" | grep -qx "test_api${API_LEVEL}"; then
        echo "test_api${API_LEVEL}"
        return 0
    fi

    if echo "$avd_list" | grep -qx "Medium_Phone_API_${API_LEVEL}"; then
        echo "Medium_Phone_API_${API_LEVEL}"
        return 0
    fi

    local match
    match=$(echo "$avd_list" | grep "${API_LEVEL}" | head -1)
    if [ -n "$match" ]; then
        echo "$match"
        return 0
    fi

    echo "ERROR: No AVD found for API $API_LEVEL. Available AVDs:" >&2
    echo "$avd_list" >&2
    return 1
}

AVD_NAME=$(resolve_avd) || exit 1
echo "AVD: $AVD_NAME"
echo "$AVD_NAME" > /tmp/zazentimer-emulator-avd

# ──────────────────────────────────────────────
# Start Xvfb (only if $DISPLAY is not set)
# ──────────────────────────────────────────────
if [ -z "${DISPLAY:-}" ]; then
    echo "DISPLAY not set — starting Xvfb on :99"
    rm -f /tmp/.X99-lock /tmp/.X11-unix/X99 2>/dev/null || true

    Xvfb :99 -screen 0 1080x1920x24 &
    XVFB_PID=$!
    echo "$XVFB_PID" > /tmp/zazentimer-xvfb.pid

    local waited=0
    while [ $waited -lt 30 ]; do
        if xdpyinfo -display :99 >/dev/null 2>&1; then
            echo "Xvfb ready on :99 (PID $XVFB_PID, ${waited}s)"
            export DISPLAY=:99
            break
        fi
        if ! kill -0 "$XVFB_PID" 2>/dev/null; then
            echo "ERROR: Xvfb failed to start (PID $XVFB_PID is dead)" >&2
            exit 1
        fi
        sleep 1
        waited=$((waited + 1))
    done

    if [ -z "${DISPLAY:-}" ]; then
        echo "ERROR: Xvfb did not become ready within 30s" >&2
        kill "$XVFB_PID" 2>/dev/null || true
        exit 1
    fi
else
    echo "DISPLAY is $DISPLAY — skipping Xvfb"
fi

# ──────────────────────────────────────────────
# Kill any stale emulators
# ──────────────────────────────────────────────
for stale in $(adb devices 2>/dev/null | grep -oP 'emulator-\d+' || true); do
    echo "Killing stale emulator $stale"
    adb -s "$stale" emu kill 2>/dev/null || true
done
pkill -9 -f "qemu.*android" 2>/dev/null || true
pkill -9 -f "emulator.*-avd" 2>/dev/null || true
sleep 3

# ──────────────────────────────────────────────
# Start emulator
# ──────────────────────────────────────────────
SERIAL="emulator-5554"
export ANDROID_SERIAL="$SERIAL"

echo "Starting emulator ($AVD_NAME, serial $SERIAL)..."
"$ANDROID_HOME/emulator/emulator" \
    -avd "$AVD_NAME" \
    $SNAPSHOT_FLAG \
    -gpu swiftshader_indirect \
    $([ -z "${DISPLAY:-}" ] && echo "-noaudio") \
    -no-boot-anim \
    -memory 2048 >> /tmp/zazentimer-emulator.log 2>&1 &
EMU_PID=$!
echo "$EMU_PID" > /tmp/zazentimer-emulator.pid
echo "Emulator started (PID $EMU_PID, AVD $AVD_NAME)"

# ──────────────────────────────────────────────
# Wait for boot
# ──────────────────────────────────────────────
echo "Waiting for device $SERIAL to appear..."
adb wait-for-device

echo "Waiting for boot to complete..."
boot_timeout=300
boot_elapsed=0
boot_done=""
while [ $boot_elapsed -lt $boot_timeout ]; do
    boot_done=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
    if [ "$boot_done" = "1" ]; then
        echo "Boot completed (${boot_elapsed}s)"
        break
    fi
    sleep 5
    boot_elapsed=$((boot_elapsed + 5))
done

if [ "$boot_done" != "1" ]; then
    echo "ERROR: Emulator did not boot within ${boot_timeout}s" >&2
    exit 1
fi

echo "Waiting for system services..."
svc_wait=0
while [ $svc_wait -lt 60 ]; do
    svc_check=""
    svc_check=$(adb shell service check activity 2>/dev/null | tr -d '\r\n')
    if [ -n "$svc_check" ] && echo "$svc_check" | grep -qi "activity"; then
        echo "System services ready (${svc_wait}s)"
        break
    fi
    sleep 2
    svc_wait=$((svc_wait + 2))
done || true

echo "Stabilizing services..."
stable_count=0
stab_wait=0
while [ $stab_wait -lt 45 ]; do
    sleep 10
    stab_wait=$((stab_wait + 10))
    svc_verify=""
    svc_verify=$(adb shell service check activity 2>/dev/null | tr -d '\r\n')
    if [ -n "$svc_verify" ] && echo "$svc_verify" | grep -qi "activity"; then
        stable_count=$((stable_count + 1))
        if [ $stable_count -ge 3 ]; then
            echo "Services stable (${stab_wait}s, $stable_count checks)"
            break
        fi
    else
        stable_count=0
    fi
done

echo "Keeping screen on..."
adb shell svc power stayon true 2>/dev/null || true

echo "Emulator ready! (serial=$SERIAL, avd=$AVD_NAME)"
