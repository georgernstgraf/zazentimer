# Play Store Assets

This directory and its siblings hold the assets consumed by the **Google Play
Store** publication pipeline. The pipeline is driven by `scripts/play_store/*.py`
(locally) and `.github/workflows/release.yml` (CI, triggered by `v*` tags).

For F-Droid assets, see [`../fastlane/metadata/README.md`](../fastlane/metadata/README.md).

## Asset matrix

| Asset | Location | Purpose | Consumer (script / workflow) | Notes |
|---|---|---|---|---|
| Screenshots & feature graphic | `google/playstore-images/*.png` | Play Store listing imagery (portrait/landscape screenshots, 1024×500 feature graphic, 512×512 app icon) | **Manual upload** to Play Console (Store listing → Graphics) | No in-repo automation; these PNGs are the canonical source — re-upload by hand when they change. |
| Listing metadata (titles, descriptions) | `store-listings.json` (repo root, 141 KB) | Play Store listing copy across locales | **None in-repo.** No script or workflow reads or writes this file. | Hand-curated (likely produced by an external translation pipeline). Its path into Play Console is **undocumented** — flag honestly, do not fabricate a flow. |
| Release notes ("what's new") | `distribution/whatsnew/whatsnew-en-GB` | Per-locale "what's new" text shipped with a release | `.github/workflows/release.yml:69` via `r0adkll/upload-google-play@v1` (`whatsNewDirectory: distribution/whatsnew`) | Only `whatsnew-en-GB` exists. The upload step carries `continue-on-error: true` (`release.yml:70`) — a failure here is **silent** and will not fail the release workflow. |
| Service account key | `google/play-api-key.json` | Google Play Developer API authentication for the `scripts/play_store/*.py` tooling | `scripts/play_store/*.py` (loaded at `update_notes.py:21`); CI uses the `PLAY_SERVICE_ACCOUNT_JSON` GitHub Secret instead | **Gitignored** at `.gitignore:18`. Never commit. Path documented here only — no contents/secret in this README. |
| Play Store automation scripts | `scripts/play_store/` | Local Play Console operations | Invoked manually by the maintainer | See entries below. |

## `scripts/play_store/` scripts

| Script | Purpose |
|---|---|
| `setup.sh` | Bootstraps the local `.venv/` Python environment. |
| `check_status.py` | Lists current Play Console tracks and releases. |
| `update_notes.py` | Updates "what's new" release notes for a specific track **via the Play Console Publishing API** (`service.edits().tracks().update(...)` at `update_notes.py:67`). Usage: `.venv/bin/python3 scripts/play_store/update_notes.py <track> <notes> [language]`. |
| `activate_alpha_bundle.py` | Re-activates a deactivated version code in the alpha track. |

## Known inconsistencies (documented, not fixed)

- **`update_notes.py` does NOT write to `distribution/whatsnew/`.** It calls the
  Play Console Publishing API directly and performs **zero disk writes**. The
  only consumer of `distribution/whatsnew/` is the `release.yml` CI workflow.
- **Locale mismatch:** `update_notes.py` defaults to `de-DE` (`update_notes.py:19`,
  `:85`), while `distribution/whatsnew/` ships only `whatsnew-en-GB`. These two
  release-notes channels are independent and out of sync by design — do not
  "fix" this without coordinating both paths.
- **`store-listings.json` has no in-repo consumer.** Its path into Play Console
  is undocumented; treat it as a hand-maintained source-of-truth until a
  documented import/export flow is added.
