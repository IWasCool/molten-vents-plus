# Molten Vents Plus

Renewable resource vents for **Minecraft 1.21.1 / NeoForge**, built around Create.

**Public source:** [IWasCool/molten-vents-plus](https://github.com/IWasCool/molten-vents-plus).
Mod version: **0.0.1**.

Water vents produce impure stone, molten vents produce regular ore stone, and
superheated vents produce pure stone. New formations use Create's native material
stones and cut/slab/wall variants. Activate an exposed dormant core with an
explosion, including TNT or creepers underwater, then supply the appropriate
fluid. Includes Ponder tutorials and JEI/EMI support.

Water, Molten and Superheated Vent Surveyors scan their own tier within 1,000,
2,000 and 3,000 blocks, stopping after 10, six and three discoveries respectively.
Choose one of six materials on the radial selector. Searches use bounded server
assistance to confirm vents beyond render distance; older servers without that
support fall back to client-loaded terrain. Discoveries arrive individually,
with temporary HUD pings and optional Xaero/FTB map markers. Completion is silent.
Scans continue while the tool is stored; Shift-right-click or reopen the selector
to cancel. All three radios share graphite cases, rubber, neutral-gray controls,
muted-teal keys and coarse radar symbols, with progressively more hardware.

**Requires:** NeoForge 21.1.251+, Create 6.0.10+, and
[Create: Molten Vents 2.1.1+](https://github.com/Apothicon02/Create-Molten-Vents).

**Languages:** all 143 Minecraft language options have complete language files.
Initial translations and regional variants are included; 23 rare or fictional
options use English fallback, and some newer strings also await translation.
See the [language coverage and contribution guide](TRANSLATIONS.md). Builds
verify language coverage, keys and formatting arguments automatically.

**Build:** JDK 21, then `./gradlew build` or `gradlew.bat build` on Windows.
Run `./gradlew validateTranslations` to check language resources separately.

**License:** [MIT](LICENSE). Derived work retains the
[Molten Vents](LICENSES/Molten-Vents-MIT.txt) and
[Create Quality of Life](LICENSES/Create-Quality-of-Life-MIT.txt) MIT notices.
Minecraft and Create textures are referenced from their dependencies.
