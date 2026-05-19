#!/usr/bin/env bash
set -euo pipefail

# ──────────────────────────────────────────────
# create-emulator-snapshots.sh
#
# Creates clean, app-free emulator snapshots for
# fast test boot via -no-snapshot-save.
#
# Usage:
#   scripts/create-emulator-snapshots.sh [api_level ...]
#   scripts/create-emulator-snapshots.sh --all
#
# Without args or --all: reads from gradle.properties
#   (hostname-specific key first, then default)
# ──────────────────────────────────────────────

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
if [ ! -d "$ANDROID_HOME" ]; then
    ANDROID_HOME="/opt/android-sdk"
fi
export ANDROID_HOME
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools"

SERIAL="emulator-5554"
export ANDROID_SERIAL="$SERIAL"

# ──────────────────────────────────────────────
# Resolve API levels to process
# ──────────────────────────────────────────────
API_LEVELS=()

if [ "${1:-}" = "--all" ]; then
    shift
    # Read default + all hostname-specific API lists, merge unique
    HOST_SHORT=$(hostname -s)
    HOST_KEY="zazentimer.test.apis.${HOST_SHORT}"
    HOST_APIS=$(grep -oP "^${HOST_KEY}=\K.*" "$PROJECT_DIR/gradle.properties" 2>/dev/null || true)
    DEFAULT_APIS=$(grep -oP "^zazentimer\.test\.apis=\K.*" "$PROJECT_DIR/gradle.properties" 2>/dev/null || true)
    ALL_STR="${HOST_APIS}${HOST_APIS:+,}${DEFAULT_APIS}"
    IFS=',' read -ra API_LEVELS <<<"$ALL_STR"
elif [ $# -gt 0 ]; then
    for arg in "$@"; do
        API_LEVELS+=("$arg")
    done
else
    HOST_SHORT=$(hostname -s)
    HOST_KEY="zazentimer.test.apis.${HOST_SHORT}"
    HOST_APIS=$(grep -oP "^${HOST_KEY}=\K.*" "$PROJECT_DIR/gradle.properties" 2>/dev/null || true)
    if [ -n "$HOST_APIS" ]; then
        IFS=',' read -ra API_LEVELS <<<"$HOST_APIS"
    else
        DEFAULT_APIS=$(grep -oP "^zazentimer\.test\.apis=\K.*" "$PROJECT_DIR/gradle.properties" 2>/dev/null || true)
        if [ -n "$DEFAULT_APIS" ]; then
            IFS=',' read -ra API_LEVELS <<<"$DEFAULT_APIS"
        fi
    fi
fi

if [ ${#API_LEVELS[@]} -eq 0 ]; then
    echo "ERROR: No API levels specified." >&2
    echo "Usage: $0 [api_level ...] | --all" >&2
    exit 1
fi

# Deduplicate
mapfile -t API_LEVELS < <(printf '%s\n' "${API_LEVELS[@]}" | sort -nu)

echo "Will create snapshots for API levels: ${API_LEVELS[*]}"
echo "ANDROID_HOME=$ANDROID_HOME"
echo ""

# ──────────────────────────────────────────────
# Helper functions
# ──────────────────────────────────────────────

resolve_avd() {
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
    echo ""
    return 1
}

kill_stale_emulators() {
    for stale in $(adb devices 2>/dev/null | grep -oP 'emulator-\d+' || true); do
        echo "Killing stale emulator $stale"
        adb -s "$stale" emu kill 2>/dev/null || true
    done
    pkill -9 -f "qemu.*android" 2>/dev/null || true
    pkill -9 -f "emulator.*-avd" 2>/dev/null || true
    sleep 3
}

wait_for_boot() {
    echo "  Waiting for device $SERIAL to appear..."
    adb wait-for-device

    echo "  Waiting for boot to complete..."
    local boot_timeout=300
    local boot_elapsed=0
    local boot_done=""
    while [ $boot_elapsed -lt $boot_timeout ]; do
        boot_done=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
        if [ "$boot_done" = "1" ]; then
            echo "  Boot completed (${boot_elapsed}s)"
            return 0
        fi
        sleep 5
        boot_elapsed=$((boot_elapsed + 5))
    done
    echo "ERROR: Emulator did not boot within ${boot_timeout}s" >&2
    return 1
}

wait_for_services() {
    echo "  Waiting for system services..."
    local svc_wait=0
    while [ $svc_wait -lt 60 ]; do
        local svc_check
        svc_check=$(adb shell service check activity 2>/dev/null | tr -d '\r\n')
        if [ -n "$svc_check" ] && echo "$svc_check" | grep -qi "activity"; then
            break
        fi
        sleep 2
        svc_wait=$((svc_wait + 2))
    done

    echo "  Stabilizing services..."
    local stable_count=0
    local stab_wait=0
    while [ $stab_wait -lt 45 ]; do
        sleep 10
        stab_wait=$((stab_wait + 10))
        local svc_verify
        svc_verify=$(adb shell service check activity 2>/dev/null | tr -d '\r\n')
        if [ -n "$svc_verify" ] && echo "$svc_verify" | grep -qi "activity"; then
            stable_count=$((stable_count + 1))
            if [ $stable_count -ge 3 ]; then
                echo "  Services stable (${stab_wait}s, $stable_count checks)"
                return 0
            fi
        else
            stable_count=0
        fi
    done
    echo "  Services stable (${stab_wait}s)"
}

configure_system() {
    echo "  Disabling animations..."
    adb shell settings put global window_animation_scale 0.0 2>/dev/null || true
    adb shell settings put global transition_animation_scale 0.0 2>/dev/null || true
    adb shell settings put global animator_duration_scale 0.0 2>/dev/null || true

    echo "  Keeping screen on..."
    adb shell svc power stayon true 2>/dev/null || true
}

# ──────────────────────────────────────────────
# Process each API level
# ──────────────────────────────────────────────
FAILED=()

for api in "${API_LEVELS[@]}"; do
    echo "──────────────────────────────────────────"
    echo "API $api — Creating snapshot"
    echo "──────────────────────────────────────────"

    avd_name=$(resolve_avd "$api")
    if [ -z "$avd_name" ]; then
        echo "ERROR: No AVD found for API $api — skipping" >&2
        FAILED+=("$api")
        continue
    fi
    echo "AVD: $avd_name"

    echo "  Killing stale emulators..."
    kill_stale_emulators

    # Delete existing snapshots so we get a fresh cold boot
    local avd_dir="$HOME/.android/avd/${avd_name}.avd"
    if [ -d "$avd_dir/snapshots" ]; then
        echo "  Removing existing snapshots: $avd_dir/snapshots/"
        rm -rf "$avd_dir/snapshots"
    fi
    rm -f "$avd_dir/snapshots"/* 2>/dev/null || true
    rm -rf "$avd_dir/quickboot" 2>/dev/null || true

    # Start emulator: -wipe-data for clean state, no -no-snapshot so snapshot is saved on exit
    echo "  Starting emulator ($avd_name)..."
    "$ANDROID_HOME/emulator/emulator" \
        -avd "$avd_name" \
        -wipe-data \
        -gpu swiftshader_indirect \
        -no-boot-anim \
        -memory 2048 \
        > /tmp/zazentimer-snapshot-api${api}.log 2>&1 &
    EMU_PID=$!
    echo "  Emulator PID: $EMU_PID"

    if ! wait_for_boot; then
        echo "ERROR: Emulator API $api failed to boot" >&2
        kill "$EMU_PID" 2>/dev/null || true
        FAILED+=("$api")
        continue
    fi

    wait_for_services
    configure_system

    echo "  Saving snapshot (graceful shutdown)..."
    adb emu kill 2>/dev/null || true
    wait "$EMU_PID" 2>/dev/null || true
    sleep 3

    echo "  Snapshot for API $api ($avd_name) created."
    echo ""
done

echo ""
echo "──────────────────────────────────────────"
echo "Snapshot creation complete."
echo "──────────────────────────────────────────"

if [ ${#FAILED[@]} -eq 0 ]; then
    echo "All API levels: OK"
else
    echo "Failed API levels: ${FAILED[*]}"
    exit 1
fi
