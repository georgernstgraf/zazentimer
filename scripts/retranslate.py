#!/usr/bin/env python3
"""Incremental retranslation of Android app string resources.

Modes:
  --diff     Only translate missing strings; copy keep-english strings
  --all      Re-translate ALL translatable strings in all locales
  --dry-run  Show what would change without writing files

Usage:
  source .venv/bin/activate
  python scripts/retranslate.py --diff
  python scripts/retranslate.py --all
  python scripts/retranslate.py --diff --dry-run
"""

import argparse
import json
import os
import re
import sys
import time

try:
    from deep_translator import GoogleTranslator, MyMemoryTranslator
except ImportError:
    print("Error: deep-translator not installed. Run: pip install deep-translator")
    sys.exit(1)

RES_BASE = "app/src/main/res"
SOURCE_FILE = os.path.join(RES_BASE, "values", "strings.xml")
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

with open(os.path.join(SCRIPT_DIR, "locales.json"), encoding="utf-8") as _f:
    LOCALES = json.load(_f)

with open(os.path.join(SCRIPT_DIR, "keep_english.json"), encoding="utf-8") as _f:
    KEEP_ENGLISH = set(json.load(_f))

PLACEHOLDER_FMT = "__{}__"

FORMAT_SPEC_RE = re.compile(r"(%%)|(?<![%])%(?:\d+\$)?[ds]")
NEWLINE_RE = re.compile(r"\\n")
ESCAPED_APOS_RE = re.compile(r"\\'")
ESCAPED_QUOT_RE = re.compile(r'\\"')
RAW_APOS_RE = re.compile(r"(?<!\\)'")

SPLIT_BY_NEWLINE = {"help_sectionlist_text"}


def count_format_specs(text):
    return len(FORMAT_SPEC_RE.findall(text))


def should_translate(name):
    if name in KEEP_ENGLISH:
        return False
    return True


def extract_strings(filepath):
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()
    pattern = re.compile(
        r'<string\s+name="([^"]+)"[^>]*>(.*?)</string>', re.DOTALL
    )
    strings = {}
    order = []
    for m in pattern.finditer(content):
        name = m.group(1)
        val = m.group(2)
        val = val.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
        strings[name] = val
        order.append(name)
    return strings, order


def mask_specials(text):
    mapping = {}
    counter = [0]

    def replacer(m):
        idx = counter[0]
        ph = PLACEHOLDER_FMT.format(idx)
        mapping[ph] = m.group(0)
        counter[0] += 1
        return ph

    text = FORMAT_SPEC_RE.sub(replacer, text)
    text = NEWLINE_RE.sub(replacer, text)
    text = ESCAPED_APOS_RE.sub(replacer, text)
    text = ESCAPED_QUOT_RE.sub(replacer, text)
    return text, mapping


def unmask_specials(text, mapping):
    for ph, orig in sorted(mapping.items(), key=lambda x: -len(x[0])):
        text = text.replace(ph, orig)
    return text


def escape_apostrophes(text):
    return RAW_APOS_RE.sub(r"\\'", text)


def translate_value(name, value, gt_code=None, mymemory_code=None):
    translator = None
    if mymemory_code:
        translator = MyMemoryTranslator(source="en-GB", target=mymemory_code)
    elif gt_code:
        translator = GoogleTranslator(source="en", target=gt_code)
    else:
        return value

    if name in SPLIT_BY_NEWLINE:
        segments = value.split("\\n")
        translated = []
        for seg in segments:
            masked, mapping = mask_specials(seg)
            try:
                t = translator.translate(masked)
                t = unmask_specials(t, mapping)
                if count_format_specs(t) != count_format_specs(seg):
                    print(f"      WARNING: placeholder count mismatch for '{name}'")
                translated.append(escape_apostrophes(t))
            except Exception as e:
                print(f"      SEGMENT FAIL: {e}")
                translated.append(seg)
            time.sleep(0.05)
        return "\\n".join(translated)

    masked, mapping = mask_specials(value)
    try:
        t = translator.translate(masked)
        t = unmask_specials(t, mapping)
        if count_format_specs(t) != count_format_specs(value):
            print(f"      WARNING: placeholder count mismatch for '{name}'")
        return escape_apostrophes(t)
    except Exception as e:
        print(f"      TRANSLATE FAIL: {e}")
        return value


def xml_escape(text):
    text = text.replace("&", "&amp;")
    text = text.replace("<", "&lt;")
    text = text.replace(">", "&gt;")
    return text


def build_strings_xml(strings, source_order):
    lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
    for name in source_order:
        if name in strings:
            lines.append(
                f'    <string name="{name}">{xml_escape(strings[name])}</string>'
            )
    lines.append("</resources>\n")
    return "\n".join(lines)


def process_locale(locale_cfg, source_strings, source_order, mode, dry_run):
    dir_name = locale_cfg["dir"]
    target_file = os.path.join(RES_BASE, dir_name, "strings.xml")

    existing_strings, _ = (
        extract_strings(target_file) if os.path.exists(target_file) else ({}, [])
    )

    result = {}

    if "base_dir" in locale_cfg:
        base_file = os.path.join(RES_BASE, locale_cfg["base_dir"], "strings.xml")
        base_strings, _ = (
            extract_strings(base_file) if os.path.exists(base_file) else ({}, {})
        )
        for name in source_order:
            if name not in source_strings:
                continue
            if not should_translate(name):
                result[name] = source_strings[name]
            elif name in base_strings:
                result[name] = base_strings[name]
            else:
                result[name] = source_strings[name]
    else:
        gt_code = locale_cfg.get("gt_code")
        mymemory_code = locale_cfg.get("mymemory_code")
        for name in source_order:
            if name not in source_strings:
                continue
            if not should_translate(name):
                result[name] = source_strings[name]
                continue
            if mode == "diff" and name in existing_strings and existing_strings[name]:
                result[name] = existing_strings[name]
                continue
            if dry_run:
                translated = translate_value(name, source_strings[name], gt_code=gt_code, mymemory_code=mymemory_code)
                print(f"    DRY-RUN: {name}")
                print(f"      EN: {source_strings[name]}")
                print(f"      ->: {translated}")
                result[name] = source_strings[name]
                continue
            try:
                translated = translate_value(name, source_strings[name], gt_code=gt_code, mymemory_code=mymemory_code)
                result[name] = translated
                print(f"    OK: {name}")
            except Exception as e:
                print(f"    FAIL: {name} - {e}")
                result[name] = existing_strings.get(name, source_strings[name])
            time.sleep(0.05)

    for name in source_order:
        if name in source_strings and name not in result:
            result[name] = source_strings[name]

    changes = sum(
        1
        for n in source_order
        if n in result
        and (n not in existing_strings or result[n] != existing_strings[n])
    )
    new_strings = sum(
        1 for n in source_order if n in result and n not in existing_strings
    )

    if not dry_run:
        os.makedirs(os.path.dirname(target_file), exist_ok=True)
        with open(target_file, "w", encoding="utf-8") as f:
            f.write(build_strings_xml(result, source_order))

    return changes, new_strings


def main():
    parser = argparse.ArgumentParser(
        description="Retranslate Android app string resources"
    )
    parser.add_argument("--locales", type=str, default=None,
                        help="Comma-separated list of locale dirs to process (e.g., values-de,values-fr)")
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--diff", action="store_true", help="Translate missing strings only")
    group.add_argument("--all", action="store_true", help="Re-translate all translatable strings")
    parser.add_argument("--dry-run", action="store_true", help="Show changes without writing")
    args = parser.parse_args()

    mode = "all" if args.all else "diff"

    locales_to_process = LOCALES
    if args.locales:
        selected = set(args.locales.split(","))
        locales_to_process = [l for l in LOCALES if l["dir"] in selected]
        print(f"Filtered to {len(locales_to_process)} locales: {', '.join(l['dir'] for l in locales_to_process)}")

    print(f"Reading source: {SOURCE_FILE}")
    source_strings, source_order = extract_strings(SOURCE_FILE)
    translatable = {k: v for k, v in source_strings.items() if should_translate(k)}
    print(
        f"  Total: {len(source_strings)}, "
        f"Translatable: {len(translatable)}, "
        f"Keep-english: {len(source_strings) - len(translatable)}"
    )
    print(f"  Mode: {mode}, Dry-run: {args.dry_run}")

    total_changes = 0
    total_new = 0

    for locale_cfg in locales_to_process:
        dir_name = locale_cfg["dir"]
        label = dir_name
        if "gt_code" in locale_cfg:
            label += f" ({locale_cfg['gt_code']})"
        elif "mymemory_code" in locale_cfg:
            label += f" (mymemory:{locale_cfg['mymemory_code']})"
        else:
            label += f" (copy from {locale_cfg['base_dir']})"
        print(f"\n=== {label} ===")

        changes, new = process_locale(
            locale_cfg, source_strings, source_order, mode, args.dry_run
        )
        total_changes += changes
        total_new += new
        if changes or new:
            print(f"  Changed: {changes}, New: {new}")

    action = "Would update" if args.dry_run else "Updated"
    print(
        f"\n=== {action}: {total_changes} changed, "
        f"{total_new} new across {len(locales_to_process)} locales ==="
    )


if __name__ == "__main__":
    main()
