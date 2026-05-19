# Skill: Translate

## Purpose

Format LLM translation work orders from the output of
`scripts/translation_deltas.py`.  Each work order contains the missing
English strings for a single locale together with strict guardrails and
Zen-context guidance.  The LLM sub-agent outputs a Phase-1-format JSON
report that `scripts/apply_translations.py` can apply.

## Usage

1. Ensure `scripts/translation_deltas.json` is up-to-date:
   ```bash
   python3 scripts/translation_deltas.py
   ```
2. Load this skill when the user asks to translate one or more locales.
3. Read `scripts/translation_deltas.json` and `scripts/non_llm_languages.json`.
4. For each requested locale (or all non-excluded locales with deltas),
   format a prompt using the template below.
5. Dispatch the prompt to a translation sub-agent (Task tool).

## Prompt Template

For **each locale** the user wants translated, assemble a prompt:

```
Translate the following Android string resources from English into
{language_name} ({locale_code}).

Output a JSON file at docs/ai/translation_reports/{locale_code}_report.json
using exactly this structure:
{
  "locale": "{locale_code}",
  "tier": {tier},
  "findings": [
    {
      "string_key": "<string name from XML>",
      "english_source": "<the English value>",
      "current_translation": null,
      "suggested_translation": "<your translation>",
      "reasoning": "<brief Zen-context note>"
    }
  ]
}

Strings to translate:

{string_table}

-----

**ABSOLUTE REQUIREMENTS — VIOLATING ANY WILL CAUSE APP CRASHES:**

1. **Format specifiers MUST be preserved exactly**.
   `%s` stays `%s`, `%1$d` stays `%1$d`, `%%` stays `%%`.
   Never change, reorder, or delete them.

2. **XML/HTML tags MUST be preserved exactly**.
   `<b>`, `</b>`, `<br>`, `<a href="...">`, `</a>`, `&lt;`, `&gt;`,
   `&amp;` must appear unchanged in the output.

3. **Escaped characters MUST be preserved**.
   `\'` stays `\'`, `\"` stays `\"`, `\n` stays `\n`.

4. **Zen-context terminology**:
   - "bell" refers to a meditation singing bowl (Klangschale), NOT a
     doorbell, telephone bell, or school bell.
   - "session" is a meditation session, not a login session.
   - "section" is a timed segment within a session.
   - Prefer meditation-appropriate vocabulary throughout.

5. **Fallback rule**: If you do NOT have high confidence in {language_name},
   or if you cannot guarantee that EVERY format specifier, XML tag, and
   escaped character will be preserved exactly, you MUST leave the string
   in English. Guessing or hallucinating will cause the application to
   crash for real users.
```

{string_table} is a markdown table:

```
| Key | English |
|-----|---------|
| bell_dimming_section_title | Bell Dimming |
| bell_volume_label_format | %1$d%% |
...
```

Include ALL missing keys for the locale from `translation_deltas.json`.

## Locale Metadata

Use these tier assignments (from the previous Phase 1 assessment):

| Tier | Locales |
|------|---------|
| 1 | es, de, fr, zh, zh-rTW, ja, pt, pt-rBR, pt-rPT, it, ru, ko, ar, nl |
| 2 | tr, pl, vi, hi, in, th, sv, da, nb, fi, el, cs, ro, hu, iw, no |
| 3 | uk, sk, bg, hr, sr, b+sr+Latn, ms, tl, fil, ca, lt, sl, lv, et, fa, bn, ta, te, ur |
| 4 | all others (sw, af, is, mk, sq, ka, hy, az, mr, gu, ml, ne, km, my, kk, uz, cy, eu, gl, ga, …) |

## Output

The translation sub-agent writes one or more JSON files to
`docs/ai/translation_reports/{locale_code}_report.json`.  These files are
compatible with `scripts/apply_translations.py` and can be applied with:

```bash
python3 scripts/apply_translations.py
```

After applying, run `scripts/translation_deltas.py` again to verify no
strings remain missing and no new validation errors exist.
