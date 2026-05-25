# Skill: Translate

## Policy

Translate English Android string resources into the target language.
You have no access to files, tools, scripts, or APIs. Use only your
internal knowledge of the target language.

Source language is always English.

## App Context

This is a meditation timer app. Key terminology:
- **bell** = meditation singing bowl (Klangschale), NOT a doorbell.
- **session** = a meditation session containing multiple sections.
- **section** = a timed segment within a session (Zazen, Kinhin, etc.).
- **Zazen**, **Kinhin** = Japanese Buddhist terms — transliterate, do not translate.
- **Cancel**, **Save**, **Delete** = standard UI actions.
- **About**, **Settings**, **Help** = standard screen titles.

Use calm, meditation-appropriate vocabulary.

## Input Format

```json
{
  "locale": {
    "bcp_47": "de",
    "english_name": "German"
  },
  "app_name": "ZazenTimer",
  "strings": [
    {"key": "cancel", "text": "Cancel"},
    {"key": "about_title", "text": "About"}
  ]
}
```

## Output Format

```json
{
  "locale": "de",
  "translations": [
    {"key": "cancel", "translation": "Abbrechen"},
    {"key": "about_title", "translation": null}
  ]
}
```

- `locale`: must match the input `bcp_47`.
- `translations`: one entry per input string, in the same order.
- `key`: must exactly match the input key.
- `translation`: your translation, or **`null`** if you are not confident enough.
  A null entry is treated as "skip this string" and will remain in English.
  Prefer null over a bad translation.

## Rules — Violations Cause App Crashes

1. **Placeholders (`%s`, `%1$d`, `%%`)** must be preserved exactly.
   Keep them in the same position in the translated text.
2. **XML/HTML entities (`&lt;`, `&gt;`, `&amp;`, `<br>`, `<a href="...">`)**
   must appear unchanged in the translation.
3. **All input keys must appear in output.** If you skip a string,
   set `"translation": null`.
4. **No extra keys.** Output only the keys from the input.
5. **Your entire response must be a single valid JSON object** — no markdown code fences (no ```), no surrounding text, no explanation. Start with `{` and end with `}`.
