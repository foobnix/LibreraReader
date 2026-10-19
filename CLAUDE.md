# Librera Reader

## Translations: always all languages

Every string added, changed or removed in `app/src/main/res/values/strings.xml` (English) must be
added, changed or removed in **every** `app/src/main/res/values-*/strings.xml` in the same change -
never English only, never "translations later".

- Translate into each language; do not paste the English text into the other files.
- Put the line at the **same line position** as in English (right after the same preceding key).
  `Builder/build-librera-beta.sh` fails the build when any translation's line count differs from
  English, e.g. `ERROR: values-zh-rTW/strings.xml has 707 lines, values/strings.xml (en) has 710`.
- `values-he` and `values-iw` are both Hebrew; keep them identical.
- Escape apostrophes as `\'` (French, Catalan, Italian, Sardinian...), and keep placeholders such as
  `%1$s` exactly as they are in English.
- Match each file's existing tone: German uses formal *Sie*, Portuguese is Brazilian (*tela*,
  *configurações*).
- Check before finishing: every `values-*/strings.xml` has the same `wc -l` as `values/strings.xml`.
