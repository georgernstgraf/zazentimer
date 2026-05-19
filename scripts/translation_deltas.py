#!/usr/bin/env python3
"""Compute translation deltas: missing and obsolete strings per locale.

Compares each values-*/strings.xml against the English master
values/strings.xml.  Skips translatable="false" entries and keys listed in
scripts/keep_english.json (format strings intended to stay in English).
Locales listed in scripts/non_llm_languages.json appear only in the
top-level "excluded" array and are omitted from the per-locale "locales"
map.  Locales without any deltas (0 missing, 0 obsolete) are also omitted
from "locales".
"""

import json
import os
import re
import sys

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
RES_DIR = os.path.join(SCRIPT_DIR, "..", "app", "src", "main", "res")
MASTER = os.path.join(RES_DIR, "values", "strings.xml")
EXCLUDE_FILE = os.path.join(SCRIPT_DIR, "non_llm_languages.json")
KEEP_ENGLISH_FILE = os.path.join(SCRIPT_DIR, "keep_english.json")
OUTPUT = os.path.join(SCRIPT_DIR, "translation_deltas.json")

# Regex: <string name="key" ...>value</string>  (dots-all for multi-line values)
STRING_RE = re.compile(
    r'<string\b[^>]*\bname="([^"]+)"[^>]*>(.*?)</string>',
    re.DOTALL,
)
TRANSLATABLE_FALSE_RE = re.compile(r'\btranslatable="false"')
XML_DECL_RE = re.compile(r'<\?xml\b.*?\?>')
COMMENT_RE = re.compile(r"<!--.*?-->", re.DOTALL)


def parse_strings(path):
    """Return {key: value} dict from a strings.xml file, skipping
    XML declaration, comments, and translatable="false" entries.

    Returns empty dict if path does not exist.
    """
    if not os.path.exists(path):
        return {}
    with open(path, "r", encoding="utf-8") as fh:
        text = fh.read()
    text = XML_DECL_RE.sub("", text)
    text = COMMENT_RE.sub("", text)
    result = {}
    for m in STRING_RE.finditer(text):
        key, value = m.group(1), m.group(2)
        attr_block = m.group(0).split(">", 1)[0]  # everything before first >
        if TRANSLATABLE_FALSE_RE.search(attr_block):
            continue
        result[key] = value.strip()
    return result


def load_excluded():
    """Return set of excluded locale codes from non_llm_languages.json."""
    if os.path.exists(EXCLUDE_FILE):
        with open(EXCLUDE_FILE, "r", encoding="utf-8") as fh:
            data = json.load(fh)
        return set(data.get("locales", []))
    return set()


def load_keep_english():
    """Return set of string keys that must stay in English (format strings)."""
    if os.path.exists(KEEP_ENGLISH_FILE):
        with open(KEEP_ENGLISH_FILE, "r", encoding="utf-8") as fh:
            data = json.load(fh)
        return set(data)
    return set()


def locale_name_from_dir(dirname):
    """'values-de' -> 'de', 'values-b+sr+Latn' -> 'b+sr+Latn'."""
    return dirname[len("values-"):]


def main():
    master_keys = parse_strings(MASTER)
    if not master_keys:
        print("ERROR: no translatable keys found in master", file=sys.stderr)
        sys.exit(1)

    keep_set = load_keep_english()
    for key in keep_set:
        master_keys.pop(key, None)

    excluded_set = load_excluded()

    # Discover on-disk locale directories
    locales = {}
    for entry in sorted(os.listdir(RES_DIR)):
        if not (entry.startswith("values-") and os.path.isdir(os.path.join(RES_DIR, entry))):
            continue
        loc = locale_name_from_dir(entry)
        xml_path = os.path.join(RES_DIR, entry, "strings.xml")
        lang_keys = parse_strings(xml_path)

        missing = sorted(set(master_keys) - set(lang_keys))
        obsolete = sorted(set(lang_keys) - set(master_keys))

        locales[loc] = {
            "directory": entry,
            "missing": missing,
            "obsolete": obsolete,
        }

    # Build top-level excluded array (only locales that exist on disk)
    excluded = sorted(loc for loc in locales if loc in excluded_set)

    # Build english_source: only keys that are missing in at least one
    # NON-excluded locale
    english_source = {}
    for loc, info in sorted(locales.items()):
        if loc in excluded_set:
            continue
        for key in info["missing"]:
            if key not in english_source:
                english_source[key] = master_keys[key]

    # Build output locales map: omit excluded and zero-delta locales
    output_locales = {}
    for loc, info in sorted(locales.items()):
        if loc in excluded_set:
            continue
        if not info["missing"] and not info["obsolete"]:
            continue
        output_locales[loc] = info

    result = {
        "excluded": excluded,
        "english_source": english_source,
        "locales": output_locales,
    }

    with open(OUTPUT, "w", encoding="utf-8") as fh:
        json.dump(result, fh, ensure_ascii=False, indent=2)
        fh.write("\n")

    # Print brief summary
    total_missing = sum(len(v["missing"]) for v in output_locales.values())
    total_obsolete = sum(len(v["obsolete"]) for v in output_locales.values())
    print(f"Master keys (translatable): {len(master_keys)}")
    print(f"Locale dirs on disk:        {len(locales)}")
    print(f"Excluded locales:           {len(excluded)}")
    print(f"Locales with deltas:        {len(output_locales)}")
    print(f"Total missing strings:      {total_missing}")
    print(f"Total obsolete strings:     {total_obsolete}")
    print(f"Output:                     {OUTPUT}")


if __name__ == "__main__":
    main()
