#!/bin/bash
# Setup script for Play Store automation environment

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
VENV_DIR="$PROJECT_ROOT/.venv"

echo "Creating virtual environment in $VENV_DIR..."
python3 -m venv "$VENV_DIR"

echo "Installing dependencies..."
"$VENV_DIR/bin/pip" install -r "$PROJECT_ROOT/scripts/play_store/requirements.txt"

echo "Done. You can now use the scripts in scripts/play_store/ using the environment in .venv."
echo "Example: .venv/bin/python3 scripts/play_store/check_status.py"
