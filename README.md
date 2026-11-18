# Molten Vents Plus

Resource vents for **Minecraft 1.21.1 / NeoForge**, version **1.0.2**.
The saved mod ID remains `create_water_vents`.

Water vents produce impure stone, molten vents produce ordinary ore stone,
and superheated vents produce pure stone. Includes configurable reserves,
Vent Surveyors, Ponder tutorials, JEI/EMI support and optional Jade readouts.
Runtime textures and language files remain in `src/main/resources`.

## Molten Vents Plus | v1.0.2

Update Log:
- Adjusted Vent Surveyor Recipes
- Adjusted Sparkle Particles

## Molten Vents Plus | v1.0.1

- Fixed the startup crash caused by a particle redirect conflict with Blueprint.
- Removed duplicate Surveyor effects when a Xaero or FTB Chunks map marker is available.
- Added blue waypoint markers to the top direction bar.
- Adjusted Surveyor and superheated lava bucket textures.

## Build

Use **JDK 21**, then run `./gradlew build` or `gradlew.bat build` on Windows.
The mod is `build/libs/molten_vents_plus-neoforge-1.21.1-1.0.2.jar`.
Dependencies download automatically through the included Gradle wrapper.

## Install

Requires NeoForge 21.1.251+, Create 6.0.10+ and Create: Molten Vents 2.1.1+
for Minecraft 1.21.1. Install on both client and server.
JEI, EMI, Jade, Xaero Minimap and FTB Chunks integrations are optional.

## License

[MIT](LICENSE). Derived work retains the
[Molten Vents](LICENSES/Molten-Vents-MIT.txt) and
[Create Quality of Life](LICENSES/Create-Quality-of-Life-MIT.txt) MIT notices.
Minecraft/Create dependency assets retain their original ownership.
