#!/bin/bash
set -uo pipefail

FORCE=false
DRY_RUN=false

while [[ $# -gt 0 ]]; do
    case "$1" in
    --force)
        FORCE=true
        shift
        ;;
    --dry-run)
        DRY_RUN=true
        shift
        ;;
    *)
        echo "Usage: $0 [--force] [--dry-run]"
        echo ""
        echo "  --force    Use SIGKILL instead of SIGTERM"
        echo "  --dry-run  Show what would be killed without killing"
        exit 1
        ;;
    esac
done

SIG_TERM="-s SIGTERM"
SIG_KILL="-9"
SIG="${SIG_TERM}"
[ "$FORCE" = true ] && SIG="${SIG_KILL}"

do_kill() {
    local pid=$1
    local label=$2
    if [ "$DRY_RUN" = true ]; then
        echo "[DRY-RUN] Would kill $label (PID $pid)"
        return
    fi
    if kill $SIG "$pid" 2>/dev/null; then
        echo "Killed $label (PID $pid)"
    else
        echo "Already dead: $label (PID $pid)"
    fi
}

echo "=== kill-test-run.sh ==="
echo "Mode: $([ "$FORCE" = true ] && echo 'SIGKILL (force)' || echo 'SIGTERM (graceful)')"
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

pids_for() {
    ps aux | grep -E "$1" | grep -v grep | awk '{print $2}' || true
}

# ──────────────────────────────────────────────
# AVD snapshot purge helpers — mirror stop-emulator.sh verbatim.
# Duplicated (not sourced) to keep this rescue tool self-contained.
# ──────────────────────────────────────────────

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

# ──────────────────────────────────────────────
# Phase 1: Kill run-instrumentation.sh
# ──────────────────────────────────────────────
echo "=== Phase 1: Killing run-instrumentation.sh ==="
for pid in $(pids_for '[r]un-instrumentation\.sh'); do
    do_kill "$pid" "run-instrumentation.sh"
done
if [ "$DRY_RUN" = false ]; then
    echo "Waiting 5s for cleanup trap..."
    sleep 5
fi

# ──────────────────────────────────────────────
# Phase 2: Kill Gradle test runners
# ──────────────────────────────────────────────
echo ""
echo "=== Phase 2: Killing Gradle test runners ==="
for pid in $(pids_for '[c]onnectedDebugAndroidTest|[g]radlew.*connected'); do
    do_kill "$pid" "Gradle test runner"
done
if [ "$DRY_RUN" = false ]; then
    sleep 3
fi

# ──────────────────────────────────────────────
# Phase 3: Kill emulators
# ──────────────────────────────────────────────
echo ""
echo "=== Phase 3: Killing emulators ==="
if [ "$DRY_RUN" = false ]; then
    for serial in $(adb devices 2>/dev/null | grep -oP 'emulator-\d+' || true); do
        echo "adb -s $serial emu kill"
        adb -s "$serial" emu kill 2>/dev/null || true
    done
    sleep 3
fi
# Capture AVD name(s) from live qemu BEFORE the force-kill loop — after SIGKILL
# /proc/<pid> is gone. Only meaningful in --force / non-dry-run mode (the only
# path that SIGKILLs); captured unconditionally into force_avd for simplicity.
force_avd=""
if [ "$FORCE" = true ] && [ "$DRY_RUN" = false ]; then
    for pid in $(pgrep -f "qemu-system-x86_64" 2>/dev/null || true); do
        name=$(_avd_name_from_pid "$pid")
        [ -n "$name" ] && force_avd="$name"
    done
fi
for pid in $(pgrep -f "qemu-system-x86_64" 2>/dev/null || true); do
    do_kill "$pid" "qemu emulator"
done
for pid in $(pgrep -f "emulator.*-avd" 2>/dev/null || true); do
    do_kill "$pid" "emulator wrapper"
done
# In --force mode the do_kill above sent SIGKILL, which can truncate an in-flight
# QuickBoot snapshot save. We captured the AVD name before the kill loop and now
# purge its snapshots/ so the next run cold-boots clean. Non-force (SIGTERM) and
# dry-run paths never SIGKILL, so they must not purge.
if [ "$FORCE" = true ] && [ "$DRY_RUN" = false ]; then
    emulator_purge_snapshot "$force_avd"
fi
for pid in $(pgrep -f "netsimd" 2>/dev/null || true); do
    do_kill "$pid" "netsimd"
done
if [ "$DRY_RUN" = false ]; then
    sleep 3
fi

# ──────────────────────────────────────────────
# Phase 4: Kill Gradle/Kotlin daemons
# ──────────────────────────────────────────────
echo ""
echo "=== Phase 4: Killing Gradle/Kotlin daemons ==="
for pid in $(pids_for '[G]radleDaemon'); do
    do_kill "$pid" "Gradle daemon"
done
for pid in $(pids_for '[K]otlinCompileDaemon.*zazentimer'); do
    do_kill "$pid" "Kotlin compiler daemon"
done
if [ "$DRY_RUN" = false ]; then
    sleep 2
fi

# ──────────────────────────────────────────────
# Phase 5: Kill Xvfb
# ──────────────────────────────────────────────
echo ""
echo "=== Phase 5: Killing Xvfb ==="
for pid in $(pgrep -f "Xvfb :99" 2>/dev/null || true); do
    do_kill "$pid" "Xvfb"
done

# ──────────────────────────────────────────────
# Phase 6: Cleanup lock files
# ──────────────────────────────────────────────
echo ""
echo "=== Phase 6: Cleanup ==="
if [ "$DRY_RUN" = false ]; then
    rm -f /tmp/.X99-lock /tmp/.X11-unix/X99 2>/dev/null && echo "Removed Xvfb lock files" || true
fi

# ──────────────────────────────────────────────
# Verify
# ──────────────────────────────────────────────
echo ""
echo "=== Verify ==="
remaining=$(ps aux | grep -E "(gradle|emulator|run-instrumentation|Xvfb)" | grep -v grep || true)
if [ -z "$remaining" ]; then
    echo "All clean - no test processes running."
else
    echo "WARNING: Still running:"
    echo "$remaining"
    if [ "$FORCE" = false ] && [ "$DRY_RUN" = false ]; then
        echo ""
        echo "Re-run with --force to use SIGKILL."
    fi
fi
