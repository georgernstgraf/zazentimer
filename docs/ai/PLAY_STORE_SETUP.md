# Play Store Automation Setup

This document explains how to set up the environment for Play Store automation scripts on a new development machine.

## Prerequisites

- Python 3.12+
- A Service Account JSON key from the Google Cloud Console.

## Local Setup

1.  **Clone the repository.**
2.  **Run the setup script:**
    ```bash
    ./scripts/play_store/setup.sh
    ```
    This will create a `.venv/` directory in the project root and install all necessary dependencies (google-api-python-client, etc.).

3.  **Place your Service Account Key:**
    The scripts currently look for the key at `~/.config/iron-country-322716-8ab0815de79f.json`.
    You can also provide a different path by modifying the `KEY_PATH` variable in the scripts if needed.

## Available Scripts

All scripts should be executed using the python interpreter from the `.venv`:

### Check Release Status
Shows all active tracks, versions, and current release notes.
```bash
.venv/bin/python3 scripts/play_store/check_status.py
```

### Update Release Notes
Updates the text for a specific track and language.
```bash
.venv/bin/python3 scripts/play_store/update_notes.py <track> "<notes>" [language]
```
- `<track>`: alpha, internal, beta, production
- `<notes>`: The new text in quotes.
- `[language]`: Optional, defaults to `de-DE`.

## CI/CD (GitHub Actions)

For automated releases, the content of the JSON key must be stored in a GitHub Secret named `PLAY_SERVICE_ACCOUNT_JSON`.
