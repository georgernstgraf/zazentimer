#!/usr/bin/env bash
set -euo pipefail

# ──────────────────────────────────────────────
# stop-emulator.sh — Kill the emulator and Xvfb
#                     started by start-emulator.sh.
#
# Reads PID files from /tmp/zazentimer-*.pid
# Cleanup is idempotent — safe to run even if
# nothing was started.
# ──────────────────────────────────────────────

SERIAL="emulator-5554"

# Kill emulator
if adb devices 2>/dev/null | grep -q "$SERIAL"; then
    echo "Killing emulator $SERIAL..."
    adb -s "$SERIAL" emu kill 2>/dev/null || true
    local wait_count=0
    while adb devices 2>/dev/null | grep -q "$SERIAL" && [ $wait_count -lt 30 ]; do
        sleep 1
        wait_count=$((wait_count + 1))
    done
else
    echo "Emulator $SERIAL not connected — skipping"
fi

# Kill emulator process from PID file
if [ -f /tmp/zazentimer-emulator.pid ]; then
    EMU_PID=$(cat /tmp/zazentimer-emulator.pid)
    if kill -0 "$EMU_PID" 2>/dev/null; then
        echo "Killing emulator process (PID $EMU_PID)..."
        kill "$EMU_PID" 2>/dev/null || true
        wait "$EMU_PID" 2>/dev/null || true
    fi
    rm -f /tmp/zazentimer-emulator.pid
fi

# Kill stray qemu/emulator processes
pkill -9 -f "qemu.*android" 2>/dev/null || true
pkill -9 -f "emulator.*-avd" 2>/dev/null || true

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
