ich gehe auf http://localhost:8000/strings

jeder string hat einen "compare" link. gut.

compare -> details

strings/:id sollte für den String haben:

- es gibt Übersetzungen in <N> Sprachen
- es gibt `gesettlete` Übersetungen in <M> Sprachen

haben wir auch einen settlement-score? dann ginge auch noch eine sortierbare Tabelle darunter:

| bcp-47 | sprachennname | Übersetzung | settlement-score | settled ja/nein |
