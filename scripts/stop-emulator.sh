#!/usr/bin/env bash
set -euo pipefail

# ──────────────────────────────────────────────
# stop-emulator.sh — Emulator teardown library
#                    + standalone cleanup.
#
# When sourced: exports library functions.
# When executed: saves a snapshot, then kills
#                emulator and Xvfb.
#
# Library functions:
#   emulator_save_snapshot  <serial> [name]
#   emulator_kill_serial    <serial>
#   emulator_kill_all
# ──────────────────────────────────────────────

# ──────────────────────────────────────────────
# emulator_save_snapshot — save emulator state
# $1 = serial (e.g. "emulator-5554")
# $2 = snapshot name (default: "default_boot")
# ──────────────────────────────────────────────
emulator_save_snapshot() {
    local serial=$1
    local name=${2:-default_boot}
    if adb devices 2>/dev/null | grep -q "$serial"; then
        echo "Saving snapshot '$name' on $serial..." >&2
        adb -s "$serial" emu avd snapshot save "$name" 2>/dev/null || {
            echo "Warning: snapshot save failed (emulator may be busy)" >&2
            return 1
        }
        echo "Snapshot '$name' saved on $serial." >&2
    else
        echo "Emulator $serial not connected — cannot save snapshot" >&2
        return 1
    fi
}

# Print "<sum_cpu_jiffies> <sum_write_bytes> <any_d_state>" across all qemu-system-x86_64 pids.
_qemu_progress_snapshot() {
    local pids cpu_total=0 wbytes_total=0 d_state=0
    pids=$(pgrep -f "qemu-system-x86_64" 2>/dev/null || true)
    if [ -z "$pids" ]; then
        echo "0 0 0"
        return
    fi
    local pid stat suffix state cpu wbytes
    for pid in $pids; do
        stat=$(cat "/proc/$pid/stat" 2>/dev/null) || continue
        suffix=${stat##*\)}
        state=$(echo "$suffix" | awk '{print $1}')
        cpu=$(echo "$suffix" | awk '{print $12+$13}')
        wbytes=$(awk '/^write_bytes/{print $2}' "/proc/$pid/io" 2>/dev/null)
        cpu_total=$((cpu_total + ${cpu:-0}))
        wbytes_total=$((wbytes_total + ${wbytes:-0}))
        [ "$state" = "D" ] && d_state=1
    done
    echo "$cpu_total $wbytes_total $d_state"
}

# Echo the AVD name from a live qemu process's cmdline, or nothing.
# $1 = qemu pid. Must be called BEFORE the process is killed.
_avd_name_from_pid() {
    local pid=$1
    [ -n "$pid" ] && [ -r "/proc/$pid/cmdline" ] || return 0
    tr '\0' '\n' < "/proc/$pid/cmdline" | awk '/^-avd$/{getline; print; exit}'
}

# Purge the QuickBoot snapshot for an AVD so the next boot cold-boots clean.
# Use ONLY after a SIGKILL of qemu (the only kill that truncates the snapshot).
# $1 = avd name
emulator_purge_snapshot() {
    local avd=$1
    [ -z "$avd" ] && return 0
    local avd_root="${ANDROID_AVD_HOME:-$HOME/.android/avd}"
    local snap_dir="$avd_root/${avd}.avd/snapshots"
    if [ -d "$snap_dir" ]; then
        echo "Purging suspect snapshot for AVD '$avd' (SIGKILL used) -> $snap_dir" >&2
        rm -rf "$snap_dir"
    fi
}

# Gracefully shut down the emulator: issue adb emu kill, then give qemu as much
# time as it needs while it is making progress (CPU OR I/O OR D-state). Only
# escalate to SIGTERM then SIGKILL after 60s of sustained zero progress.
# $1 = serial (optional; if empty, skip adb emu kill and just poll/escalate orphans)
emulator_graceful_kill() {
    local serial="${1:-}"
    if [ -n "$serial" ] && adb devices 2>/dev/null | grep -q "$serial"; then
        echo "Requesting graceful shutdown of $serial (adb emu kill)..." >&2
        adb -s "$serial" emu kill 2>/dev/null || true
    fi

    local poll_interval=5 idle_limit=12   # 12 * 5s = 60s sustained idleness
    local prev_snap prev_cpu prev_wbytes idle=0 samples=0
    prev_snap=$(_qemu_progress_snapshot)
    prev_cpu=$(echo "$prev_snap" | awk '{print $1}')
    prev_wbytes=$(echo "$prev_snap" | awk '{print $2}')

    while true; do
        sleep "$poll_interval"
        samples=$((samples + 1))
        if ! pgrep -f "qemu-system-x86_64" >/dev/null 2>&1; then
            echo "qemu exited cleanly after $((samples * poll_interval))s." >&2
            return 0
        fi
        local snap cpu wbytes dstate progressing=0
        snap=$(_qemu_progress_snapshot)
        cpu=$(echo "$snap" | awk '{print $1}')
        wbytes=$(echo "$snap" | awk '{print $2}')
        dstate=$(echo "$snap" | awk '{print $3}')
        [ "$dstate" = "1" ] && progressing=1
        [ "$((cpu - prev_cpu))" -gt 0 ] && progressing=1
        [ "$((wbytes - prev_wbytes))" -gt 0 ] && progressing=1
        if [ "$progressing" = "1" ]; then
            idle=0
        else
            idle=$((idle + 1))
            echo "qemu idle (no CPU/IO/D-state progress) — idle streak $idle/$idle_limit ($((samples * poll_interval))s elapsed)" >&2
        fi
        prev_cpu=$cpu
        prev_wbytes=$wbytes
        if [ "$idle" -ge "$idle_limit" ]; then
            echo "qemu hung for $((idle_limit * poll_interval))s — escalating." >&2
            break
        fi
    done

    # Capture AVD name(s) from live qemu BEFORE any signal — post-SIGKILL
    # /proc/<pid> is gone and the name can no longer be recovered.
    local pid avd_name=""
    for pid in $(pgrep -f "qemu-system-x86_64" 2>/dev/null || true); do
        local name
        name=$(_avd_name_from_pid "$pid")
        [ -n "$name" ] && avd_name="$name"
    done

    for pid in $(pgrep -f "qemu-system-x86_64" 2>/dev/null || true); do
        echo "SIGTERM qemu PID $pid" >&2
        kill "$pid" 2>/dev/null || true
    done
    local term_wait=0
    while pgrep -f "qemu-system-x86_64" >/dev/null 2>&1 && [ "$term_wait" -lt 10 ]; do
        sleep 1
        term_wait=$((term_wait + 1))
    done
    if pgrep -f "qemu-system-x86_64" >/dev/null 2>&1; then
        echo "qemu survived SIGTERM — SIGKILL (last resort)" >&2
        pkill -9 -f "qemu-system-x86_64" 2>/dev/null || true
        pkill -9 -f "emulator.*-avd" 2>/dev/null || true
        sleep 2
        # SIGKILL can truncate an in-flight QuickBoot snapshot save; purge it so
        # the next run cold-boots a clean image (userdata qcow2 is preserved).
        emulator_purge_snapshot "$avd_name"
    fi
}

# ──────────────────────────────────────────────
# emulator_kill_serial — kill emulator by serial
# $1 = serial (e.g. "emulator-5554")
# ──────────────────────────────────────────────
emulator_kill_serial() {
    local serial=$1
    if adb devices 2>/dev/null | grep -q "$serial"; then
        emulator_graceful_kill "$serial"
    else
        echo "Emulator $serial not connected — skipping" >&2
    fi
}

# ──────────────────────────────────────────────
# emulator_kill_all — kill all qemu/emulator processes
# ──────────────────────────────────────────────
emulator_kill_all() {
    local emu_serial
    for emu_serial in $(adb devices 2>/dev/null | grep -oP 'emulator-\d+' || true); do
        echo "Killing leftover emulator $emu_serial" >&2
        emulator_graceful_kill "$emu_serial"
    done
    if pgrep -f "qemu-system-x86_64" >/dev/null 2>&1; then
        echo "Orphaned qemu still alive — final graceful pass (no serial)" >&2
        emulator_graceful_kill ""
    fi
    # Belt-and-suspenders final force-sweep. If qemu is still alive here, the
    # imminent SIGKILL may truncate an in-flight snapshot save — capture the AVD
    # name first, then purge after the kill (only if something was force-killed).
    local force_avd="" pid name qemu_was_alive=false
    if pgrep -f "qemu-system-x86_64" >/dev/null 2>&1; then
        qemu_was_alive=true
        for pid in $(pgrep -f "qemu-system-x86_64" 2>/dev/null || true); do
            name=$(_avd_name_from_pid "$pid")
            [ -n "$name" ] && force_avd="$name"
        done
    fi
    pkill -9 -f "qemu-system-x86_64" 2>/dev/null || true
    pkill -9 -f "emulator.*-avd" 2>/dev/null || true
    sleep 1
    if [ "$qemu_was_alive" = true ]; then
        emulator_purge_snapshot "$force_avd"
    fi
}

# ──────────────────────────────────────────────
# Standalone mode (when executed, not sourced)
# ──────────────────────────────────────────────
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    SERIAL="emulator-5554"

    emulator_save_snapshot "$SERIAL" || true

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
