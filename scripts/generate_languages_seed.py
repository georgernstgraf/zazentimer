#!/usr/bin/env python3
"""Generate languages_mapping.json from res/values-* directories."""

import json
import os
import re
import sys

try:
    import pycountry
except ImportError:
    print("ERROR: pycountry not installed. Run: pip install pycountry", file=sys.stderr)
    sys.exit(1)

RES_DIR = "app/src/main/res"
WHISPER_PATH = "prisma/translations/whisper_languages.json"
OUTPUT_PATH = "prisma/translations/languages_seed.json"

def parse_bcp47(dir_name):
    """Convert a directory name like 'values-de' or 'values-b+sr+Latn' into BCP 47 tag."""
    tag = dir_name[len("values-"):]
    
    if tag.startswith("b+"):
        # Android BCP 47 format: b+language+script+...
        parts = tag[2:].split("+")
        bcp47 = "-".join(parts)
    else:
        # Standard format: language or language-r<region>
        # Replace -r<region> with -<region>
        bcp47 = re.sub(r'-r([A-Z]{2})', r'-\1', tag)
    
    return bcp47

def parse_posix(bcp47):
    """Convert BCP 47 tag to POSIX locale code.
    
    Rules:
    - de -> de
    - pt-BR -> pt_BR (region gets underscore)
    - sr-Latn -> sr@latin (script becomes @modifier, lowercase)
    """
    parts = bcp47.split("-")
    language = parts[0]
    
    remaining = parts[1:]
    region = None
    script = None
    
    for part in remaining:
        if len(part) == 2 and part.isalpha() and part.isupper():
            region = part
        elif len(part) == 4 and part[0].isupper() and part[1:].islower():
            script = part
    
    result = language
    if region:
        result += "_" + region
    if script:
        result += "@" + script.lower()
    
    return result

def get_language_entry(bcp47):
    """Resolve BCP 47 primary subtag to (iso_639_3, english_name) using pycountry."""
    primary = bcp47.split("-")[0].lower()
    lang = None
    
    # Try ISO 639-1 (alpha_2) lookup first
    try:
        lang = pycountry.languages.get(alpha_2=primary)
    except Exception:
        pass
    
    # Try ISO 639-3 (alpha_3) lookup (for languages without ISO 639-1)
    if not lang:
        try:
            lang = pycountry.languages.get(alpha_3=primary)
        except Exception:
            pass
    
    if lang:
        iso3 = getattr(lang, 'alpha_3', primary)
        name = getattr(lang, 'name', None)
        return iso3, name
    
    # Fallback: use primary subtag itself
    return primary, None

def get_whisper_name(bcp47, whisper_map):
    """Look up English language name in Whisper's language table."""
    primary = bcp47.split("-")[0].lower()
    name = whisper_map.get(primary)
    return name if name else None

def main():
    # Resolve paths relative to script location
    script_dir = os.path.dirname(os.path.abspath(__file__))
    res_path = os.path.normpath(os.path.join(script_dir, "..", RES_DIR))
    whisper_path = os.path.normpath(os.path.join(script_dir, "..", WHISPER_PATH))
    output_path = os.path.normpath(os.path.join(script_dir, "..", OUTPUT_PATH))
    
    if not os.path.isdir(res_path):
        print(f"ERROR: {res_path} not found", file=sys.stderr)
        sys.exit(1)
    
    if not os.path.isfile(whisper_path):
        print(f"ERROR: {whisper_path} not found — run scripts/extract_whisper_languages.py first", file=sys.stderr)
        sys.exit(1)
    
    with open(whisper_path, "r", encoding="utf-8") as f:
        whisper_map = json.load(f)
    
    entries = []
    
    for entry in sorted(os.listdir(res_path)):
        if not entry.startswith("values-"):
            continue
        if not os.path.isdir(os.path.join(res_path, entry)):
            continue
        
        dir_name = entry
        bcp47 = parse_bcp47(dir_name)
        posix = parse_posix(bcp47)
        iso3, english_name = get_language_entry(bcp47)
        whisper = get_whisper_name(bcp47, whisper_map)
        
        entries.append({
            "directory": dir_name,
            "bcp_47": bcp47,
            "posix_code": posix,
            "iso_639_3": iso3,
            "english_name": english_name,
            "whisper_response": whisper,
        })
    
    # Validation
    for field in ["directory", "bcp_47", "posix_code", "iso_639_3"]:
        values = [e[field] for e in entries]
        if len(values) != len(set(values)):
            from collections import Counter
            dupes = {v for v, c in Counter(values).items() if c > 1}
            print(f"WARNING: Duplicate values for {field}: {dupes}", file=sys.stderr)
    
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(entries, f, indent=2, ensure_ascii=False)
    
    print(f"Generated {len(entries)} locale entries → {output_path}")

if __name__ == "__main__":
    main()
