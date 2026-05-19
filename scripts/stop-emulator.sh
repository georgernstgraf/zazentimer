#!/usr/bin/env bash
set -euo pipefail

# ──────────────────────────────────────────────
# stop-emulator.sh — Emulator teardown library
#                    + standalone cleanup.
#
# When sourced: exports library functions.
# When executed: kills emulator and Xvfb.
#
# Library functions:
#   emulator_kill_serial  <serial>
#   emulator_kill_all
# ──────────────────────────────────────────────

# ──────────────────────────────────────────────
# emulator_kill_serial — kill emulator by serial
# $1 = serial (e.g. "emulator-5554")
# ──────────────────────────────────────────────
emulator_kill_serial() {
    local serial=$1
    if adb devices 2>/dev/null | grep -q "$serial"; then
        echo "Killing emulator $serial..." >&2
        adb -s "$serial" emu kill 2>/dev/null || true
        local wait_count=0
        while adb devices 2>/dev/null | grep -q "$serial" && [ $wait_count -lt 30 ]; do
            sleep 1
            wait_count=$((wait_count + 1))
        done
    else
        echo "Emulator $serial not connected — skipping" >&2
    fi
}

# ──────────────────────────────────────────────
# emulator_kill_all — kill all qemu/emulator processes
# ──────────────────────────────────────────────
emulator_kill_all() {
    for emu_serial in $(adb devices 2>/dev/null | grep -oP 'emulator-\d+' || true); do
        echo "Killing leftover emulator $emu_serial" >&2
        adb -s "$emu_serial" emu kill 2>/dev/null || true
    done
    sleep 2
    pkill -9 -f "qemu.*android" 2>/dev/null || true
    pkill -9 -f "emulator.*-avd" 2>/dev/null || true
    sleep 2
}

# ──────────────────────────────────────────────
# Standalone mode (when executed, not sourced)
# ──────────────────────────────────────────────
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    SERIAL="emulator-5554"

    emulator_kill_serial "$SERIAL"

    # Kill from PID file
    if [ -f /tmp/zazentimer-emulator.pid ]; then
        EMU_PID=$(cat /tmp/zazentimer-emulator.pid)
        if kill -0 "$EMU_PID" 2>/dev/null; then
            echo "Killing emulator process (PID $EMU_PID)..."
            kill "$EMU_PID" 2>/dev/null || true
            wait "$EMU_PID" 2>/dev/null || true
        fi
        rm -f /tmp/zazentimer-emulator.pid
    fi

    emulator_kill_all

    # Clean up AVD name file
    rm -f /tmp/zazentimer-emulator-avd

    # Kill Xvfb
    if [ -f /tmp/zazentimer-xvfb.pid ]; then
        XVFB_PID=$(cat /tmp/zazentimer-xvfb.pid)
        if [ -n "${XVFB_PID:-}" ] && kill -0 "$XVFB_PID" 2>/dev/null; then
            echo "Killing Xvfb (PID $XVFB_PID)..."
            kill "$XVFB_PID" 2>/dev/null || true
            wait "$XVFB_PID" 2>/dev/null || true
        fi
        rm -f /tmp/zazentimer-xvfb.pid
    fi

    # Clean up Xvfb lock files
    rm -f /tmp/.X99-lock /tmp/.X11-unix/X99 2>/dev/null || true

    echo "Emulator and Xvfb stopped."
fi
