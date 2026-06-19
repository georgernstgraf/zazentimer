#!/usr/bin/env bash
set -euo pipefail

# ──────────────────────────────────────────────
# start-emulator.sh — Emulator management library
#                     + standalone launcher.
#
# When sourced: exports library functions.
# When executed: starts an emulator (standalone).
#
# Library functions:
#   emulator_x11_prepare
#   emulator_kill_stale
#   emulator_resolve_avd        <api_level>
#   emulator_launch             <avd> <serial> [extra_flags...]
#   emulator_wait_boot          <serial> [boot_timeout]
#   emulator_configure_system   <serial>
#
# Usage (standalone):
#   scripts/start-emulator.sh [--cold] [api_level]
#   Default API: 35
#
# Snapshot workflow:
#   --cold   → clean boot (no snapshot).  stop-emulator.sh saves a
#              snapshot for subsequent fast boots.
#   normal   → fast boot from snapshot saved by stop-emulator.sh.
#              If no snapshot exists, falls back to normal cold boot.
# ──────────────────────────────────────────────

ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"

# ──────────────────────────────────────────────
# emulator_x11_prepare — ensure DISPLAY is set
# Sets EMULATOR_XVFB_PID if Xvfb was started.
# ──────────────────────────────────────────────
emulator_x11_prepare() {
    if [ -z "${DISPLAY:-}" ]; then
        echo "DISPLAY not set — starting Xvfb on :99" >&2
        rm -f /tmp/.X99-lock /tmp/.X11-unix/X99 2>/dev/null || true

        Xvfb :99 -screen 0 1080x1920x24 &
        EMULATOR_XVFB_PID=$!
        echo "$EMULATOR_XVFB_PID" > /tmp/zazentimer-xvfb.pid

        local waited=0
        while [ $waited -lt 30 ]; do
            if xdpyinfo -display :99 >/dev/null 2>&1; then
                echo "Xvfb ready on :99 (PID $EMULATOR_XVFB_PID, ${waited}s)" >&2
                export DISPLAY=:99
                return 0
            fi
            if ! kill -0 "$EMULATOR_XVFB_PID" 2>/dev/null; then
                echo "ERROR: Xvfb failed to start (PID $EMULATOR_XVFB_PID is dead)" >&2
                EMULATOR_XVFB_PID=""
                return 1
            fi
            sleep 1
            waited=$((waited + 1))
        done

        echo "ERROR: Xvfb did not become ready within 30s" >&2
        kill "$EMULATOR_XVFB_PID" 2>/dev/null || true
        EMULATOR_XVFB_PID=""
        return 1
    else
        echo "DISPLAY is $DISPLAY — using existing display" >&2
    fi
}

# ──────────────────────────────────────────────
# emulator_kill_stale — kill all running emulators
# ──────────────────────────────────────────────
emulator_kill_stale() {
    for stale in $(adb devices 2>/dev/null | grep -oP 'emulator-\d+' || true); do
        echo "Killing stale emulator $stale" >&2
        adb -s "$stale" emu kill 2>/dev/null || true
    done
    pkill -9 -f "qemu-system-x86_64" 2>/dev/null || true
    pkill -9 -f "emulator.*-avd" 2>/dev/null || true
    sleep 3
}

# ──────────────────────────────────────────────
# emulator_resolve_avd — find AVD for API level
# $1 = api_level
# Echoes AVD name to stdout, messages to stderr.
# Returns 0 on success, 1 if not found.
# ──────────────────────────────────────────────
emulator_resolve_avd() {
    local api_level=$1
    local avd_list
    avd_list=$("$ANDROID_HOME/emulator/emulator" -list-avds 2>/dev/null || true)

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

    echo "ERROR: No AVD found for API $api_level." >&2
    echo "Available AVDs:" >&2
    echo "$avd_list" >&2
    return 1
}

# ──────────────────────────────────────────────
# emulator_launch — start an emulator in background
# $1 = avd_name
# $2 = serial (exported as ANDROID_SERIAL)
# $3 = logfile (emulator stdout/stderr redirected here)
# $@ = extra flags appended after common flags
# Echoes PID to stdout, messages to stderr.
# ──────────────────────────────────────────────
emulator_launch() {
    local avd=$1 serial=$2 logfile=$3
    shift 3
    export ANDROID_SERIAL="$serial"

    echo "Starting emulator ($avd, serial=$serial)..." >&2
    "$ANDROID_HOME/emulator/emulator" \
        -avd "$avd" \
        -gpu swiftshader_indirect \
        -no-boot-anim \
        -memory 2048 \
        "$@" >> "$logfile" 2>&1 &
    echo $!
}

# ──────────────────────────────────────────────
# emulator_wait_boot — wait for boot + services
# $1 = serial
# $2 = boot_timeout (default 300)
# Messages to stderr. Returns 0 on success, 1 on failure.
# ──────────────────────────────────────────────
emulator_wait_boot() {
    local serial=$1
    local boot_timeout=${2:-300}

    echo "Waiting for device $serial to appear..." >&2
    adb -s "$serial" wait-for-device

    echo "Waiting for boot to complete..." >&2
    local boot_elapsed=0
    local boot_done=""
    while [ $boot_elapsed -lt $boot_timeout ]; do
        boot_done=$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
        if [ "$boot_done" = "1" ]; then
            echo "Boot completed (${boot_elapsed}s)" >&2
            break
        fi
        sleep 5
        boot_elapsed=$((boot_elapsed + 5))
    done

    if [ "$boot_done" != "1" ]; then
        echo "ERROR: Emulator $serial did not boot within ${boot_timeout}s" >&2
        return 1
    fi

    echo "Waiting for system services on $serial..." >&2
    local svc_wait=0
    while [ $svc_wait -lt 60 ]; do
        local svc_check
        svc_check=$(adb -s "$serial" shell service check activity 2>/dev/null | tr -d '\r\n')
        if [ -n "$svc_check" ] && echo "$svc_check" | grep -qi "activity"; then
            echo "System services ready (${svc_wait}s)" >&2
            break
        fi
        sleep 2
        svc_wait=$((svc_wait + 2))
    done

    echo "Stabilizing services on $serial..." >&2
    local stable_count=0
    local stab_wait=0
    while [ $stab_wait -lt 45 ]; do
        sleep 10
        stab_wait=$((stab_wait + 10))
        local svc_verify
        svc_verify=$(adb -s "$serial" shell service check activity 2>/dev/null | tr -d '\r\n')
        if [ -n "$svc_verify" ] && echo "$svc_verify" | grep -qi "activity"; then
            stable_count=$((stable_count + 1))
            if [ $stable_count -ge 3 ]; then
                echo "Services stable on $serial (${stab_wait}s, $stable_count checks)" >&2
                return 0
            fi
        else
            stable_count=0
        fi
    done
    echo "Services stable on $serial (${stab_wait}s)" >&2
}

# ──────────────────────────────────────────────
# emulator_configure_system — disable animations, keep screen on
# $1 = serial
# ──────────────────────────────────────────────
emulator_configure_system() {
    local serial=$1

    echo "Disabling animations on $serial..." >&2
    adb -s "$serial" shell settings put global window_animation_scale 0.0 2>/dev/null || true
    adb -s "$serial" shell settings put global transition_animation_scale 0.0 2>/dev/null || true
    adb -s "$serial" shell settings put global animator_duration_scale 0.0 2>/dev/null || true

    echo "Keeping screen on for $serial..." >&2
    adb -s "$serial" shell svc power stayon true 2>/dev/null || true
}

# ──────────────────────────────────────────────
# Standalone mode (when executed, not sourced)
# ──────────────────────────────────────────────
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
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
    SNAPSHOT_FLAG=""
    if [ "$COLD" = true ]; then
        SNAPSHOT_FLAG="-no-snapshot"
    fi

    AVD_NAME=$(emulator_resolve_avd "$API_LEVEL") || exit 1
    echo "AVD: $AVD_NAME"
    echo "$AVD_NAME" > /tmp/zazentimer-emulator-avd

    emulator_x11_prepare || exit 1
    emulator_kill_stale

    SERIAL="emulator-5554"
    extra_flags="$SNAPSHOT_FLAG"
    [ -n "${EMULATOR_XVFB_PID:-}" ] && extra_flags="$extra_flags -noaudio"
    EMU_PID=$(emulator_launch "$AVD_NAME" "$SERIAL" "/tmp/zazentimer-emulator.log" $extra_flags)
    echo "$EMU_PID" > /tmp/zazentimer-emulator.pid
    echo "Emulator started (PID $EMU_PID, AVD $AVD_NAME)"

    emulator_wait_boot "$SERIAL" || exit 1
    emulator_configure_system "$SERIAL"

    echo "Emulator ready! (serial=$SERIAL, avd=$AVD_NAME)"
fi
