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
for pid in $(pgrep -f "qemu.*android" 2>/dev/null || true); do
    do_kill "$pid" "qemu emulator"
done
for pid in $(pgrep -f "emulator.*-avd" 2>/dev/null || true); do
    do_kill "$pid" "emulator wrapper"
done
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
