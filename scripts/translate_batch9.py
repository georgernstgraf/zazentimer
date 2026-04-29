#!/usr/bin/env python3
"""Translate app strings into Batch 9 languages (ku, la, lb, mg, mi, mt, ny, om, or, ps, qu, rw)."""

import re
import os
import time
from deep_translator import GoogleTranslator

RES_BASE = "app/src/main/res"
SOURCE_FILE = os.path.join(RES_BASE, "values", "strings.xml")

LANG_DIRS = {
    'ku': 'values-ku',
    'la': 'values-la',
    'lb': 'values-lb',
    'mg': 'values-mg',
    'mi': 'values-mi',
    'mt': 'values-mt',
    'ny': 'values-ny',
    'om': 'values-om',
    'or': 'values-or',
    'ps': 'values-ps',
    'qu': 'values-qu',
    'rw': 'values-rw',
}

EXCLUDE_NAMES = {
    'about1', 'about2', 'about3',
    'app_description', 'app_name',
    'bell_name_1', 'bell_name_2', 'bell_name_3', 'bell_name_4',
    'bell_name_5', 'bell_name_6', 'bell_name_7', 'bell_name_8',
    'theme_value_dark', 'theme_value_light',
}

EXCLUDE_PREFIX = 'abc_'

PLACEHOLDER_FMT = "\u2985{}\u2986"  # ⦅N⦆

FORMAT_SPEC_RE = re.compile(r'(%%)|(?<![%])%(?:\d+\$)?[ds]')
NEWLINE_RE = re.compile(r'\\n')
ESCAPED_APOS_RE = re.compile(r"\\'")
ESCAPED_QUOT_RE = re.compile(r'\\"')
RAW_APOS_RE = re.compile(r"(?<!\\)'")


def should_translate(name):
    if name in EXCLUDE_NAMES:
        return False
    if name.startswith(EXCLUDE_PREFIX):
        return False
    return True


def extract_strings(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    pattern = re.compile(r'<string\s+name="([^"]+)"\s*>(.*?)</string>', re.DOTALL)
    strings = {}
    for match in pattern.finditer(content):
        name = match.group(1)
        value = match.group(2)
        strings[name] = value
    return strings


def extract_strings_from_raw(raw_xml):
    pattern = re.compile(r'<string\s+name="([^"]+)"\s*>(.*?)</string>', re.DOTALL)
    strings = {}
    for match in pattern.finditer(raw_xml):
        name = match.group(1)
        value = match.group(2)
        strings[name] = value
    return strings


def mask_specials(text):
    """Mask format specifiers, \\n, \\', \\" with ⦅idx⦆ placeholders.
    Returns (masked_text, {placeholder: original})."""
    mapping = {}
    counter = [0]

    def fmt_replacer(m):
        if m.group(1) is not None:
            return '%%'
        idx = counter[0]
        placeholder = PLACEHOLDER_FMT.format(idx)
        mapping[placeholder] = m.group(0)
        counter[0] += 1
        return placeholder

    def replacer(m):
        idx = counter[0]
        placeholder = PLACEHOLDER_FMT.format(idx)
        mapping[placeholder] = m.group(0)
        counter[0] += 1
        return placeholder

    text = FORMAT_SPEC_RE.sub(fmt_replacer, text)
    text = NEWLINE_RE.sub(replacer, text)
    text = ESCAPED_APOS_RE.sub(replacer, text)
    text = ESCAPED_QUOT_RE.sub(replacer, text)
    return text, mapping


def unmask_specials(text, mapping):
    for placeholder, original in mapping.items():
        text = text.replace(placeholder, original)
    return text


def escape_apostrophes(text):
    """Escape raw apostrophes for Android XML string resources."""
    return RAW_APOS_RE.sub(r"\\'", text)


def xml_escape(text):
    text = text.replace('&', '&amp;')
    text = text.replace('<', '&lt;')
    text = text.replace('>', '&gt;')
    return text


def build_strings_xml(all_strings, source_order):
    lines = ['<?xml version="1.0" encoding="utf-8"?>', '<resources>']
    for name in source_order:
        if name in all_strings:
            value = xml_escape(all_strings[name])
            lines.append(f'    <string name="{name}">{value}</string>')
    lines.append('</resources>\n')
    return '\n'.join(lines)


def translate_to_lang(source_strings, lang_code):
    """Translate all translatable strings into `lang_code`.
    Returns dict of {name: translated_text} for ALL strings."""
    result = {}
    for name, value in source_strings.items():
        if not should_translate(name):
            result[name] = value
        else:
            masked, mapping = mask_specials(value)
            try:
                translated = GoogleTranslator(source='en', target=lang_code).translate(masked)
                restored = unmask_specials(translated, mapping)
                restored = escape_apostrophes(restored)
                result[name] = restored
                print(f"    OK: {name}")
            except Exception as e:
                print(f"    FAIL: {name} - {e}")
                result[name] = value
            time.sleep(0.05)

    return result


def main():
    print("Reading source strings...")
    source_strings = extract_strings(SOURCE_FILE)
    source_order = list(source_strings.keys())
    translatable = {k: v for k, v in source_strings.items() if should_translate(k)}
    print(f"  Total strings: {len(source_strings)}")
    print(f"  To translate: {len(translatable)}")
    print(f"  Excluded: {len(source_strings) - len(translatable)}")

    for lang_code, dir_name in LANG_DIRS.items():
        target_file = os.path.join(RES_BASE, dir_name, "strings.xml")
        print(f"\n=== {lang_code} ({dir_name}) ===")

        translated = translate_to_lang(source_strings, lang_code)

        output = build_strings_xml(translated, source_order)
        os.makedirs(os.path.dirname(target_file), exist_ok=True)
        with open(target_file, 'w', encoding='utf-8') as f:
            f.write(output)

        print(f"  Written: {len(translated)} total strings")

    print("\n=== All languages translated ===")
    for lang in LANG_DIRS:
        print(f"  {lang}")


if __name__ == '__main__':
    main()
