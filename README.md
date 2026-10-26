# Molten Vents Plus

Renewable resource vents for **Minecraft 1.21.1 / NeoForge**, built around Create.

Requires **NeoForge 21.1.251+**, **Create 6.0.10+** and [Create: Molten Vents 2.1.1+](https://github.com/Apothicon02/Create-Molten-Vents). Current mod version: **0.0.1**.

## Vents

- **Water:** common miniature formations. Water sources produce impure stone; milling gives a **12% chance of one nugget**.
- **Molten:** the original molten formations use lava to produce regular ore stones.
- **Superheated:** rare formations with the molten shape and a rim just one block wider. Superheated lava produces pure stone; crushing yields extra ores, nuggets and resources directly.
- **Oil:** renewable native oil in deserts/badlands, swamps and oceans. Activate the core and pump its outlet into tanks or a refinery.

Activate dormant cores with an explosion. Use the matching source fluid, then harvest and process the generated stone. **Ponder** explains setup and automation; **JEI** shows recipes and yields. New formations generate in new chunks.

## Optional integrations

**Create Nuclear** adds uranium, thorium and lead vents. Oil providers include **Create Diesel Generators**, **The Factory Must Grow**, **Immersive Petroleum** and **Petrochem**. Other placeable oils can use the `create_water_vents:oil_outputs` fluid tag. Superheated lava comes from a compatible provider such as **Create Quality of Life**.

Configuration: `create_water_vents-common.toml`. Operators can use `/enable movable_vents` and `/disable movable_vents`.

## Build

Use **JDK 21**:

```sh
./gradlew build
```

On Windows, use `gradlew.bat build`. The runtime JAR is produced in `build/libs/`.

The saved mod ID remains `create_water_vents` for world compatibility. Replace an older Water Vents JAR when upgrading; keep one copy of this mod.

## Translations

Includes initial translations and regional variants for **120 locales**. New translations are machine-assisted or initial authored adaptations; corrections are welcome. Unsupported rare or fictional languages use Minecraft’s English fallback.

Language files are in [`src/main/resources/assets/create_water_vents/lang`](src/main/resources/assets/create_water_vents/lang). Translation improvements are welcome; preserve translation keys and `%s` placeholders.

## License

[MIT](LICENSE). Derived vent designs retain the original [Molten Vents MIT license and attribution](LICENSES/Molten-Vents-MIT.txt). Minecraft/Create textures are referenced from their dependencies.
