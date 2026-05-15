You are a wise, culturally sensitive translation agent. Generate Play Store listing translations for the Zazen Meditation Timer app for these 28 BCP-47 language codes:
fr-CA (French Canada), fr-FR (French France), gl-ES (Galician), gu (Gujarati), hi-IN (Hindi), hr (Croatian), hu-HU (Hungarian), hy-AM (Armenian), id (Indonesian), is-IS (Icelandic), it-IT (Italian), iw-IL (Hebrew RTL), ja-JP (Japanese), ka-GE (Georgian), kk (Kazakh), km-KH (Khmer), kn-IN (Kannada), ko-KR (Korean), ky-KG (Kyrgyz), lo-LA (Lao), lt (Lithuanian), lv (Latvian), mk-MK (Macedonian), ml-IN (Malayalam), mn-MN (Mongolian), mr-IN (Marathi), ms (Malay), ms-MY (Malay Malaysia — same text as ms).
Title is ALWAYS "Zazen Meditation Timer" (never translate it).
Short description MUST be ≤ 80 characters.
Full description MUST be ≤ 4000 characters.
English master text:
Short description: "A meditation timer with customizable sections and authentic bell sounds."
Full description:
"Zazen Meditation Timer — A mindful companion for your sitting practice.
Design custom meditation sessions with multiple sections — Zazen, Kinhin, or any sitting practice. Each section has its own duration and bell sound. Eight authentic Japanese rin bowls and a Tibetan singing bowl provide clear signals for the start, transitions, and end of your meditation.
Features:
• Custom sessions with multiple timed sections
• Eight authentic bell sounds from Japanese and Tibetan traditions
• Individual bell volume controls
• Do Not Disturb — silence calls and notifications during meditation
• Keep screen on during practice
• Dark, light, and system themes
• Backup and restore sessions and custom sounds
• No data collection, no ads, no internet required
The timer runs reliably in the background with a persistent notification. Lock your screen or switch apps — your meditation continues undisturbed.
Open source on GitHub. Created by Stefan Gaffga, maintained by Georg Graf."
Guidelines:
- Use native scripts for all languages (Hindi/Devanagari, Japanese/kanji+kana, Korean/hangul, Hebrew/Hebrew script, Gujarati, Kannada, Malayalam, Marathi, Khmer, Lao, Georgian, Armenian, Mongolian/Cyrillic, etc.)
- "Zazen" and "Kinhin" are Japanese Buddhist terms — transliterate naturally
- Tone: calm, respectful, mindful
- ms and ms-MY use identical Malay text
- Hebrew (iw-IL) is RTL
- For fr-CA use Canadian French conventions, fr-FR use French conventions
Write a valid JSON file to `/tmp/store-listings-2.json` using the Write tool. Structure:
{"fr-CA":{"title":"Zazen Meditation Timer","shortDescription":"...","fullDescription":"..."},"fr-FR":{...},...}
After writing, verify the file exists and report how many entries were written.
