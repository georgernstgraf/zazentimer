# Skill: Translate

## Policy

Translate English Android string resources into the target language.
You may read `translate-input.json` and `translate-output.json`.
You have no access to other files, tools, scripts, or APIs.

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
    {"key": "about_title", "translation": ["Titel", "Über"]},
    {"key": "unknown_key", "translation": null}
  ]
}
```

- `locale`: must match the input `bcp_47`.
- `translations`: one entry per input string, in the same order.
- `key`: must exactly match the input key.
- `translation`: your translation, or an **array** of alternatives, or **`null`**:
  - A single confident translation → plain string.
  - Two or more equally valid options → array of strings.
  - If you do not know the translation → `null` (entry stays in English).
  Prefer null over a bad translation.

## Rules — Violations Cause App Crashes

1. **Placeholders (`%s`, `%1$d`, `%%`)** must be preserved exactly.
   Keep them in the same position in the translated text.
2. **XML/HTML entities (`&lt;`, `&gt;`, `&amp;`, `<br>`, `<a href="...">`)**
   must appear unchanged in the translation.
3. **All input keys must appear in output.** If you skip a string,
   set `"translation": null`.
4. **No extra keys.** Output only the keys from the input.
5. **Write the result to `translate-output.json` using your file-writing tool.** The file must contain only the JSON object, starting with `{` and ending with `}`.
6. **If you are uncertain between two valid translations**, you may provide both as an array: `"translation": ["Option A", "Option B"]`. The system stores multiple candidates for later voting. A single confident translation should be a plain string.
