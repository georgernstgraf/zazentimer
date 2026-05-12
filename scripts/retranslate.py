#!/usr/bin/env python3
"""Incremental retranslation of Android app string resources.

Modes:
  --diff          Only translate missing strings; copy keep-english strings
  --all           Re-translate ALL translatable strings in all locales
  --lint          Compare current translations against MyMemory human-vetted data
  --human-guard   Replace translations with high-confidence human matches (>=95%)
  --dry-run       Show changes without writing files
  --email EMAIL   MyMemory email for higher rate limits (50k chars/day vs 5k)
  --locales X,Y   Process only specified locale directories

Usage:
  source .venv/bin/activate
  python scripts/retranslate.py --diff
  python scripts/retranslate.py --all
  python scripts/retranslate.py --lint --locales values-de,values-ja
  python scripts/retranslate.py --human-guard --email you@example.com
  python scripts/retranslate.py --diff --dry-run
"""

import argparse
import json
import os
import re
import sys
import time
from dataclasses import dataclass
from datetime import datetime, timedelta

try:
    from deep_translator import GoogleTranslator, MyMemoryTranslator
except ImportError:
    print("Error: deep-translator not installed. Run: pip install deep-translator")
    sys.exit(1)

try:
    import requests
except ImportError:
    print("Error: requests not installed. Run: pip install requests")
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

MYMEMORY_API = "https://api.mymemory.translated.net/get"
CACHE_FILE = os.path.join(SCRIPT_DIR, ".mymemory_cache.json")
CACHE_TTL_DAYS = 7

CORE_LOCALES = {
    "values-zh": "zh-CN",
    "values-es": "es",
    "values-hi": "hi",
    "values-ar": "ar",
    "values-bn": "bn",
    "values-pt": "pt",
    "values-ru": "ru",
    "values-ja": "ja",
    "values-fr": "fr",
}

MT_ENGINES = {
    "Google Translate", "Microsoft Translator", "MT",
    "Google Translate v2", "Microsoft Translator v2",
}


@dataclass
class MyMemoryResult:
    translated_text: str
    match: float
    quality: int | None
    created_by: str

    @property
    def is_human(self):
        return self.created_by not in MT_ENGINES


def count_format_specs(text):
    return len(FORMAT_SPEC_RE.findall(text))


def load_cache():
    if os.path.exists(CACHE_FILE):
        with open(CACHE_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    return {}


def save_cache(cache):
    with open(CACHE_FILE, "w", encoding="utf-8") as f:
        json.dump(cache, f, ensure_ascii=False, indent=2)


def cache_key(langpair, source_text):
    truncated = source_text[:100]
    return f"{langpair}:{truncated}"


def is_cache_valid(entry):
    if "timestamp" not in entry:
        return False
    cached_time = datetime.fromisoformat(entry["timestamp"])
    return datetime.now() - cached_time < timedelta(days=CACHE_TTL_DAYS)


def mymemory_lookup(source_text, langpair, email=None, cache=None):
    key = cache_key(langpair, source_text)
    if cache is not None and key in cache and is_cache_valid(cache[key]):
        entry = cache[key]
        return MyMemoryResult(
            translated_text=entry["translated_text"],
            match=entry["match"],
            quality=entry.get("quality"),
            created_by=entry.get("created_by", ""),
        )

    params = {
        "q": source_text[:500],
        "langpair": f"en|{langpair}",
    }
    if email:
        params["de"] = email

    try:
        resp = requests.get(MYMEMORY_API, params=params, timeout=10)
        resp.raise_for_status()
        data = resp.json()
    except Exception as e:
        print(f"      MYMEMORY API FAIL: {e}")
        return None

    if data.get("responseStatus") != 200:
        return None

    rd = data.get("responseData", {})
    best = rd
    for match_entry in data.get("matches", []):
        if match_entry.get("match", 0) > best.get("match", 0):
            best = match_entry

    result = MyMemoryResult(
        translated_text=best.get("translatedText", ""),
        match=float(best.get("match", 0)),
        quality=int(best["quality"]) if best.get("quality") else None,
        created_by=best.get("created-by", ""),
    )

    if cache is not None:
        cache[key] = {
            "translated_text": result.translated_text,
            "match": result.match,
            "quality": result.quality,
            "created_by": result.created_by,
            "timestamp": datetime.now().isoformat(),
        }

    return result


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
    try:
        translator = None
        if mymemory_code:
            translator = MyMemoryTranslator(source="en-GB", target=mymemory_code)
        elif gt_code:
            translator = GoogleTranslator(source="en", target=gt_code)
        else:
            return value
    except Exception as e:
        print(f"      TRANSLATE FAIL: {e}")
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


def lint_locale(dir_name, langpair, source_strings, source_order, email, cache):
    target_file = os.path.join(RES_BASE, dir_name, "strings.xml")
    if not os.path.exists(target_file):
        print(f"  SKIP: {dir_name} not found")
        return

    existing_strings, _ = extract_strings(target_file)

    human_verified = 0
    mt_only = 0
    differs = 0
    high_conf_differs = 0
    no_match = 0
    queried = 0

    for name in source_order:
        if name not in source_strings or not should_translate(name):
            continue

        value = source_strings[name]
        current = existing_strings.get(name, "")

        masked, mapping = mask_specials(value)
        result = mymemory_lookup(masked, langpair, email=email, cache=cache)
        queried += 1

        if result is None:
            no_match += 1
            print(f"  {name}: NO MATCH")
            continue

        mm_text = unmask_specials(result.translated_text, mapping)

        if result.is_human and result.match >= 0.95:
            human_verified += 1
            status = "HUMAN OK"
        elif result.is_human:
            human_verified += 1
            status = f"HUMAN match={result.match:.2f}"
        else:
            mt_only += 1
            status = "MT"

        if mm_text and current and mm_text != current:
            differs += 1
            if result.is_human and result.match >= 0.95:
                high_conf_differs += 1
                status += " <- DIFFERS (high-conf human)"
            else:
                status += " <- differs"
            print(f"  {name}: CURRENT={current[:50]}  MYMEMORY={mm_text[:50]}  [{status}]")
        elif mm_text and current:
            print(f"  {name}: [{status}] (same)")

        if queried > 0 and queried % 5 == 0:
            time.sleep(1)

    if queried > 0:
        save_cache(cache)

    print(f"\n  --- Lint: {dir_name} ---")
    print(f"  Queried: {queried}, Human-verified: {human_verified}, MT-only: {mt_only}, No match: {no_match}")
    print(f"  Differs from current: {differs} (high-confidence human: {high_conf_differs})")


def human_guard_locale(dir_name, langpair, source_strings, source_order, email, cache, dry_run):
    target_file = os.path.join(RES_BASE, dir_name, "strings.xml")
    if not os.path.exists(target_file):
        print(f"  SKIP: {dir_name} not found")
        return 0

    existing_strings, _ = extract_strings(target_file)
    result_strings = dict(existing_strings)
    improvements = 0
    queried = 0

    for name in source_order:
        if name not in source_strings or not should_translate(name):
            continue

        value = source_strings[name]
        masked, mapping = mask_specials(value)
        mm_result = mymemory_lookup(masked, langpair, email=email, cache=cache)
        queried += 1

        if mm_result is None:
            continue

        if mm_result.match < 0.95 or not mm_result.is_human:
            continue

        mm_text = unmask_specials(mm_result.translated_text, mapping)
        mm_text = escape_apostrophes(mm_text)

        if count_format_specs(mm_text) != count_format_specs(value):
            print(f"    SKIP {name}: placeholder mismatch")
            continue

        current = existing_strings.get(name, "")
        if mm_text != current:
            improvements += 1
            if dry_run:
                print(f"    WOULD REPLACE: {name}")
                print(f"      CURRENT: {current[:60]}")
                print(f"      HUMAN:   {mm_text[:60]} (match={mm_result.match:.2f}, by={mm_result.created_by})")
            else:
                result_strings[name] = mm_text
                print(f"    REPLACED: {name} (match={mm_result.match:.2f}, by={mm_result.created_by})")

        if queried > 0 and queried % 5 == 0:
            time.sleep(1)

    if not dry_run and improvements > 0:
        os.makedirs(os.path.dirname(target_file), exist_ok=True)
        with open(target_file, "w", encoding="utf-8") as f:
            f.write(build_strings_xml(result_strings, source_order))

    if queried > 0:
        save_cache(cache)

    action = "Would replace" if dry_run else "Replaced"
    print(f"  {action}: {improvements} strings with human-vetted translations ({queried} queried)")
    return improvements


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
                        help="Comma-separated list of locale dirs (e.g., values-de,values-fr)")
    parser.add_argument("--email", type=str, default=None,
                        help="MyMemory email for higher rate limits (50k chars/day)")
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--diff", action="store_true", help="Translate missing strings only")
    group.add_argument("--all", action="store_true", help="Re-translate all translatable strings")
    group.add_argument("--lint", action="store_true",
                        help="Compare translations against MyMemory human-vetted data")
    group.add_argument("--human-guard", action="store_true",
                        help="Replace translations with high-confidence human matches")
    parser.add_argument("--dry-run", action="store_true", help="Show changes without writing")
    args = parser.parse_args()

    print(f"Reading source: {SOURCE_FILE}")
    source_strings, source_order = extract_strings(SOURCE_FILE)
    translatable = {k: v for k, v in source_strings.items() if should_translate(k)}
    print(
        f"  Total: {len(source_strings)}, "
        f"Translatable: {len(translatable)}, "
        f"Keep-english: {len(source_strings) - len(translatable)}"
    )

    if args.lint:
        mode = "lint"
    elif args.human_guard:
        mode = "human-guard"
    else:
        mode = "all" if args.all else "diff"

    print(f"  Mode: {mode}, Dry-run: {args.dry_run}")

    if mode in ("lint", "human-guard"):
        locales_map = dict(CORE_LOCALES)
        if args.locales:
            selected = set(args.locales.split(","))
            locales_map = {k: v for k, v in locales_map.items() if k in selected}

        if not locales_map:
            print("No core locales match the --locales filter")
            sys.exit(1)

        print(f"  Locales: {', '.join(locales_map.keys())}")
        cache = load_cache()

        for dir_name, langpair in locales_map.items():
            print(f"\n=== {dir_name} (langpair: en|{langpair}) ===")
            if mode == "lint":
                lint_locale(dir_name, langpair, source_strings, source_order,
                            args.email, cache)
            else:
                human_guard_locale(dir_name, langpair, source_strings, source_order,
                                   args.email, cache, args.dry_run)

        print("\n=== Done ===")
        return

    locales_to_process = LOCALES
    if args.locales:
        selected = set(args.locales.split(","))
        locales_to_process = [l for l in LOCALES if l["dir"] in selected]
        print(f"Filtered to {len(locales_to_process)} locales: {', '.join(l['dir'] for l in locales_to_process)}")

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
