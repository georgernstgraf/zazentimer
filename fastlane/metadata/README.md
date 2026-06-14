# F-Droid Metadata

This directory holds the store-listing assets consumed by **F-Droid**. It is
intentionally minimal and is harvested by the F-Droid build bot **by
convention**, not by any reference in `.fdroid.yml` (which does not mention
`fastlane/`). There is no `Fastfile` or `Appfile` in this repo and no fastlane
CLI usage in any workflow.

For Play Store assets, see [`../../../google/README.md`](../../../google/README.md).

## Scope

Contents (complete):

| File | Purpose |
|---|---|
| `android/en-US/title.txt` | App title shown on F-Droid. |
| `android/en-US/short_description.txt` | F-Droid short description (≤ 80 chars). |
| `android/en-US/full_description.txt` | F-Droid full description. |

That is the **entire** scope: `android/en-US/` only. There are:

- **No images / screenshots** here. Play Store screenshots and icons live in
  [`../../../google/playstore-images/`](../../../google/playstore-images/), not
  under `fastlane/`.
- **No changelogs/** directory.
- **No other locales.**

## Editing rules

- **Do NOT add header comments inside the `.txt` files.** The F-Droid bot parses
  them **literally** — any `#` comment or markdown will appear verbatim in the
  store listing. This `README.md` lives **alongside** the parsed files (a
  sibling), so it is safe; do not duplicate its content into the `.txt` files.
- To add a new locale, create `android/<locale>/{title,short_description,full_description}.txt`.
- Keep filenames exactly as above — the F-Droid bot expects these literal names.
