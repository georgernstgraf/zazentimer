# Skill: Proficiency

## Policy

Assess your own language proficiency for translating Android string resources
of a meditation timer app. You have no access to files, tools, scripts, or
APIs. Use only your internal knowledge of the target language.

Source language is always English.

## App Context

This is a meditation timer app with terminology like Zazen (sitting meditation),
Kinhin (walking meditation), singing bowls (not doorbells), sessions (meditation
configurations), and sections (timed segments within a session).

## Input Format

```json
{
  "locale": {
    "bcp_47": "de",
    "english_name": "German"
  },
  "app_name": "ZazenTimer"
}
```

## Output Format

```json
{
  "locale": "de",
  "proficiency": 4,
  "reasoning": "German is one of my strongest languages. I can handle meditation UI terminology fluently."
}
```

- `locale`: must be the exact `bcp_47` string from the input locale object. Do not use the `english_name` or any other value.
- `proficiency`: self-assessment of your ability in this language:
  1 = minimal (cannot produce reliable translations)
  2 = basic (can handle simple phrases with frequent errors)
  3 = moderate (can translate most UI strings with some errors)
  4 = strong (can translate naturally with occasional nuance errors)
  5 = native (can translate perfectly including cultural nuances)
  **BE HONEST.** Low proficiency is better than bad translations.
- `reasoning`: brief explanation of your self-assessment.

Your output must be valid JSON. No surrounding text, no markdown.
