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

source "$SCRIPT_DIR/start-emulator.sh"

SERIAL="emulator-5554"
export ANDROID_SERIAL="$SERIAL"

# ──────────────────────────────────────────────
# Resolve API levels to process
# ──────────────────────────────────────────────
API_LEVELS=()

if [ "${1:-}" = "--all" ]; then
    shift
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
# Process each API level
# ──────────────────────────────────────────────
FAILED=()

for api in "${API_LEVELS[@]}"; do
    echo "──────────────────────────────────────────"
    echo "API $api — Creating snapshot"
    echo "──────────────────────────────────────────"

    avd_name=$(emulator_resolve_avd "$api") || {
        echo "ERROR: No AVD found for API $api — skipping" >&2
        FAILED+=("$api")
        continue
    }
    echo "AVD: $avd_name"

    echo "  Killing stale emulators..."
    emulator_kill_stale

    # Delete existing snapshots so we get a fresh cold boot
    local avd_dir="$HOME/.android/avd/${avd_name}.avd"
    if [ -d "$avd_dir/snapshots" ]; then
        echo "  Removing existing snapshots: $avd_dir/snapshots/"
        rm -rf "$avd_dir/snapshots"
    fi
    rm -f "$avd_dir/snapshots"/* 2>/dev/null || true
    rm -rf "$avd_dir/quickboot" 2>/dev/null || true

    # Start emulator: -wipe-data for clean state, no snapshot flags so snapshot is saved on exit
    echo "  Starting emulator ($avd_name)..."
    local emu_pid
    emu_pid=$(emulator_launch "$avd_name" "$SERIAL" -wipe-data 2>/dev/null)
    echo "  Emulator PID: $emu_pid"

    if ! emulator_wait_boot "$SERIAL"; then
        echo "ERROR: Emulator API $api failed to boot" >&2
        kill "$emu_pid" 2>/dev/null || true
        FAILED+=("$api")
        continue
    fi

    emulator_configure_system "$SERIAL"

    echo "  Saving snapshot (graceful shutdown)..."
    adb -s "$SERIAL" emu kill 2>/dev/null || true
    wait "$emu_pid" 2>/dev/null || true
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
