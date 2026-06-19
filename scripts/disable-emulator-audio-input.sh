#!/usr/bin/env bash
set -euo pipefail

# ──────────────────────────────────────────────
# disable-emulator-audio-input.sh
#
# Patches all AVD config.ini files to disable
# audio *input* (microphone) while keeping audio
# *output* enabled. This can fix audio crackling
# on the emulator caused by the virtual microphone
# forcing the host audio device into a low-quality
# headset mode.
#
# Idempotent — safe to run multiple times.
# ──────────────────────────────────────────────

AVD_DIR="${AVD_DIR:-$HOME/.android/avd}"
PATCHED=0
SKIPPED=0

for config_ini in "$AVD_DIR"/*.avd/config.ini; do
    [ -f "$config_ini" ] || continue
    avd_name=$(basename "$(dirname "$config_ini")" .avd)

    if grep -q '^hw\.audioInput=no$' "$config_ini"; then
        echo "  skip  $avd_name — already hw.audioInput=no"
        SKIPPED=$((SKIPPED + 1))
        continue
    fi

    # Replace hw.audioInput=yes → hw.audioInput=no
    # Ensure hw.audioOutput=yes if present
    if grep -q '^hw\.audioInput=yes$' "$config_ini"; then
        sed -i 's/^hw\.audioInput=yes$/hw.audioInput=no/' "$config_ini"
        echo "  patch $avd_name — hw.audioInput=yes → no"
        PATCHED=$((PATCHED + 1))
    else
        # Line missing entirely — append it
        echo "hw.audioInput=no" >> "$config_ini"
        echo "  patch $avd_name — added hw.audioInput=no"
        PATCHED=$((PATCHED + 1))
    fi
done

echo ""
echo "Done: $PATCHED patched, $SKIPPED already correct."
