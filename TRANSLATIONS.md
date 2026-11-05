# Language support

Molten Vents Plus includes a language file for every one of Minecraft 1.21.1's
143 language options, including regional variants, right-to-left languages and
fictional languages. Each option has all 144 mod strings and all 12 original
Molten Vents block names. This covers block and item names, Surveyor controls
and messages, Ponder tutorials, recipe categories, structure names and movement
command responses.

Language-file coverage does not mean every string has a fluent translation.
Translations are initial contributions and may need corrections. The release
review localizes the water-reserve and Jade messages in 43 language options,
corrects Surveyor tool names in 27 options, and aligns vent material names with
Create's own translations in those options. Obsolete scanner messages have been
removed. Some newer strings in other languages still use English. These 23 options
currently use English throughout the mod's language file:

| Language | File code |
| --- | --- |
| Andalusian | `esan` |
| Bavarian | `bar` |
| Brabantian | `brb` |
| Cantabrian | `qcb_es` |
| Cornish | `kw_gb` |
| Elfdalian | `ovd` |
| Franconian | `fra_de` |
| Gallo | `go_fr` |
| Halych Ukrainian | `hal_ua` |
| Gothic | `got_de` |
| High Norwegian | `hn_no` |
| Klingon | `tlh_aa` |
| Lojban | `jbo_en` |
| Low German | `nds_de` |
| Nahuatl | `nah` |
| Quenya | `qya_aa` |
| Ripuarian | `ksh` |
| Rusyn | `ry_ua` |
| Saxon | `sxu` |
| Southern Puebla Popoloca | `pls` |
| Tzotzil | `tzo_mx` |
| Viossa | `vp_vl` |
| Võro | `vro` |

English fallback is intentional until a reliable translation is contributed.
Original Molten Vents names also retain English in some partially translated
options. No completeness or native-speaker review is implied.

## Contributing translations

Edit the UTF-8 JSON file for the selected language in both directories:

- [Mod translations](src/main/resources/assets/create_water_vents/lang/)
- [Original Molten Vents names](src/main/resources/assets/molten_vents/lang/)

Use `en_us.json` in each directory as the key reference. Translate values, keep
keys unchanged and preserve formatting arguments such as `%s`. Arguments may be reordered with
indexed placeholders such as `%2$s` and `%1$s`. Keep brand names and material
names consistent with Minecraft and Create in the target language. Active and
dormant versions of a vent share the same display name.

Run `./gradlew validateTranslations` (`gradlew.bat validateTranslations` on
Windows) with JDK 21. This checks the 143-option catalogue, JSON syntax,
nonempty strings, key parity and formatting arguments in both namespaces.
`build`, `check` and resource packaging run this check automatically. These
checks catch missing or broken text; they cannot judge translation fluency.

The language catalogue matches Minecraft 1.21.1 asset metadata. Update the
catalogue and both translation directories together when adding a language.
Contributions use the project's [MIT license](LICENSE); preserve the original
upstream notices.
