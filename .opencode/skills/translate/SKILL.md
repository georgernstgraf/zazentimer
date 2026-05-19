# Skill: Translate

## Policy — Read First

**NO AUTOMATED BLIND SCRIPTS.**  The LLM performs translations interactively
using explicit Zen meditation context.  Script-based, context-free automated
translation of meditation-specific strings (e.g. via `retranslate.py`,
Google Translate API, or MyMemory) is **strictly forbidden**.  Every locale
must be translated by an LLM sub-agent that understands Zazen, Kinhin,
mindfulness, and singing-bowl terminology.

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
2. Read `scripts/translation_deltas.json` and `scripts/non_llm_languages.json`.
3. **Work in batches of 5–10 locales** — do not process all locales at once.
   Smaller batches preserve quality and prevent context dilution.
4. For each batch, dispatch one LLM sub-agent per locale using the
   prompt template below.
5. After each batch completes, verify:
   ```bash
   python3 scripts/translation_deltas.py
   ```
   If any locale in the batch still has missing keys, re-dispatch only
   those remaining keys.

## 5-Step Process (per Batch)

1. **Extract English Source** — read the exact English values from
   `translation_deltas.json` to capture all format placeholders (`%s`,
   `%1$d`, `%%`), XML tags (`<br>`, `<a href="...">`), and escaped
   characters (`\'`, `\n`).
2. **Batch Processing** — dispatch 5–10 LLM sub-agents in parallel
   using the Task tool, one per locale.
3. **Apply Zen Context** — each sub-agent translates with awareness that
   "bell" = singing bowl (Klangschale), "session" = meditation session,
   "section" = timed segment, "Zazen"/"Kinhin" are Japanese Buddhist terms.
4. **Iterative Injection** — after translation, the sub-agent writes
   `docs/ai/translation_reports/{locale_code}_report.json`.  Run
   `scripts/apply_translations.py` to inject findings into the XML files.
5. **Verification** — run `python3 scripts/translation_deltas.py` after
   injection to confirm all keys in the batch are covered.  Run
   `./gradlew lintDebug` to catch XML formatting errors.

## Prompt Template

For **each locale** in the current batch, assemble a prompt:

```
--- DO NOT USE SCRIPTS OR APIs ---
You are a human-language translator. Translate the following Android
string resources from English into {language_name} ({locale_code}) with
full awareness of Zen meditation context. Do NOT use Google Translate,
MyMemory, or any automated translation engine — you must perform the
translation yourself, understanding the meditation-specific terminology.

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
   - "Zazen" and "Kinhin" are Japanese Buddhist terms — transliterate
     naturally, do not translate them.
   - Prefer meditation-appropriate, calm vocabulary throughout.

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
| system_volume_subtitle | Controls the phone\'s alarm volume for all bells. |
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

The translation sub-agent writes JSON files to
`docs/ai/translation_reports/{locale_code}_report.json`.  These files are
compatible with `scripts/apply_translations.py` and can be applied with:

```bash
python3 scripts/apply_translations.py
```

After applying an entire batch, run the verification loop:

```bash
python3 scripts/translation_deltas.py
```

If any locale in the batch still has `missing` keys in the output, those
keys must be re-translated before the batch is considered complete.
