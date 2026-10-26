# Molten Vents Plus

Renewable resource vents for **Minecraft 1.21.1 / NeoForge**, built around Create.

Requires **NeoForge 21.1.251+**, **Create 6.0.10+** and [Create: Molten Vents 2.1.1+](https://github.com/Apothicon02/Create-Molten-Vents). Current mod version: **0.0.1**.

## Vents

- **Water:** small angular rock chimneys. Water sources produce impure stone; milling has a small chance of a native resource.
- **Molten:** the original molten formations use lava to produce regular ore stones.
- **Superheated:** rare formations with a solid stepped rim just one block wider than molten vents. Superheated lava produces pure stone; crushing yields extra ores, nuggets and resources directly.
- **Oil:** renewable native oil in deserts/badlands, swamps and oceans. Activate the core and pump its outlet into tanks or a refinery.

Activate dormant cores with an explosion. Use the matching source fluid, then harvest and process the generated stone. **Ponder** explains setup and automation; **JEI or EMI (including Reliable EMI)** shows recipes and yields. Looking up any ore-stone variant opens one material page with its water, molten and superheated fluid/vent/output rows. Individual impure stones release water particles, regular molten-tier stones release flames, and pure stones release blue soul flames from their exposed surfaces. Vent cores have no ambient particle effect.

No custom nugget items are added. Impure stones mill for a **12% chance of an existing nugget**, or a **1 in 75 chance** of coal or netherite scrap where no native nugget is available. Pure stones crush directly into existing resources. Water and superheated cores have separate rock/fluid faces with no molten texture underneath.

New formations are spread across placement regions: **256 × 256 blocks for water**, **512 × 512 for molten and biome-dependent oil**, and **1,024 × 1,024 for superheated**. Each region selects at most one mineral of its tier, from the six Create ore stones; terrain and biome restrictions can leave it empty. At a valid site, **95%** of cluster rolls select one vent, **4.9%** select two and **0.1%** select three. Companions sit beside the first vent; unsuitable terrain or biomes can reduce a cluster. Shape and placement changes apply to new chunks; existing formations remain intact.

## Optional integrations

Oil providers include **Create Diesel Generators**, **The Factory Must Grow**, **Immersive Petroleum** and **Petrochem**. Other placeable oils can use the `create_water_vents:oil_outputs` fluid tag.

**Superheated lava** is built in when **Create Quality of Life** is absent; its native fluid takes priority when installed. The fallback uses the same animated blue textures and compacting recipe: **1,000 mB lava + 1 Blaze Cake + 1 obsidian dust (`c:dusts/obsidian`) → 500 mB superheated lava**. No heating is required. Superheated vent centers match the liquid's texture and tint, including dormant cores.

Configuration: `create_water_vents-common.toml`. Operators can use `/enable movable_vents` and `/disable movable_vents`.

## Build

Use **JDK 21**:

```sh
./gradlew build
```

On Windows, use `gradlew.bat build`. The runtime JAR is produced in `build/libs/`.

The saved mod ID remains `create_water_vents` for world compatibility. Replace an older Water Vents JAR when upgrading; keep one copy of this mod. Nuclear vents and their processing recipes have been removed. Legacy mineral blocks load as ordinary stone, their items as empty stacks, and saved mineral structure pieces remain inert. Previously added nugget items also resolve to empty stacks.

## Translations

Includes initial translations and regional variants for **120 locales**. New translations are machine-assisted or initial authored adaptations; corrections are welcome. Unsupported rare or fictional languages use Minecraft’s English fallback.

Language files are in [`src/main/resources/assets/create_water_vents/lang`](src/main/resources/assets/create_water_vents/lang). Translation improvements are welcome; preserve translation keys and `%s` placeholders.

## License

[MIT](LICENSE). Derived vent designs retain the original [Molten Vents MIT license and attribution](LICENSES/Molten-Vents-MIT.txt). Superheated lava textures, recipe and fluid behavior derive from [Create Quality of Life](https://github.com/iglee42/CreateQualityOfLife) by iglée42 under its [MIT license](LICENSES/Create-Quality-of-Life-MIT.txt). Minecraft/Create textures are referenced from their dependencies.
