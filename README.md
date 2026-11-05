# Molten Vents Plus

Resource vents for **Minecraft 1.21.1 / NeoForge**, built around Create.

**Public source:** [IWasCool/molten-vents-plus](https://github.com/IWasCool/molten-vents-plus).
Mod version: **1.0.0**.

Water vents produce impure stone, molten vents produce regular ore stone, and
superheated vents produce pure stone. New formations use Create's native material
stones and cut/slab/wall variants. Activate an exposed dormant core with an
explosion, including TNT or creepers underwater, then supply the appropriate
fluid. Includes Ponder tutorials and JEI/EMI support.

Water vents are temporary starter deposits. Each active core can produce
4,096 impure stone blocks by default, then permanently becomes mineable impure
stone. Idle time does not use reserves. Remaining output is saved across world
reloads and core relocation. Existing active vents from older builds get their
remaining uses multiplied by four once; exhausted vents stay exhausted. Old
active cores without an initialized reserve receive the new capacity on their
first conversion pass. `waterCapacity` in
`config/create_water_vents-common.toml` sets the initial reserve for new vents;
already saved reserves are retained.

`moltenCapacity` in the same file sets the maximum conversions for each original
molten core, independently of other cores. It applies to all six materials;
`-1` (default) is unlimited, `0` exhausts a core immediately, and a positive
number gives it that many uses. Only successful conversions spend uses. Finite
reserves persist through saves and relocation; changing the setting does not
refill an initialized finite reserve. An exhausted molten core becomes matching
ordinary ore stone. Superheated vents remain renewable.

All active and dormant cores have bedrock hardness and blast resistance, resist
entity destruction, and cannot be broken by survival mining, Create drills or
direct machine `destroyBlock` calls. Explosions activate dormant cores in place
without destroying or dropping them. Creative removal and operator commands
remain available, as with bedrock. Spent ordinary/impure stones are mineable.

Create contraptions and Simulated/Sable assemblies cannot move cores by default.
Set `allowVentMovement = true` in `config/create_water_vents-common.toml` to allow
movement globally, or use the existing operator `/enable movable_vents` command
for a saved per-world opt-in. To block movement, set the config false and use
`/disable movable_vents`. The `movable_molten_vents` command alias and saved
`cnwMoltenVentsMovable` rule are retained. Movement permission never enables
breaking cores. Sable searches leave protected cores in their original positions.

With Jade installed on the client and server, looking at any vent core shows
its remaining uses in the normal block-info tooltip. Water vents show the
server's remaining reserve (or starting capacity while dormant); molten vents
show their actual finite reserve or **Infinite**, and superheated vents show
**Infinite**. Reading the tooltip does not consume or initialize uses.
Jade is optional, and the mod still runs without it.

Crushing pure Asurine, Veridium, Crimsite or Ochrum guarantees 12 matching native
crushed ore and eight nuggets. Six additional crushed-ore units and four
additional nuggets each roll independently at 50%, for totals of 12–18 crushed
ore and 8–12 nuggets. Coal and quartz crushing retain their existing yields.
Milling impure Asurine, Veridium, Crimsite or Ochrum rolls independently for one
matching nugget at 16% and another at 12%. Impure Scoria rolls for one quartz
at 12% and another at 8%, and remains millstone-only. Both rolls can succeed,
giving up to two items. Impure Scorchia's coal recipe is unchanged.

Water, Molten and Superheated Vent Surveyors scan their own tier within 1,000,
2,000 and 3,000 blocks, stopping after 10, six and three discoveries respectively.
Choose one of six materials on the radial selector. Searches use bounded server
assistance to confirm vents beyond render distance; older servers without that
support fall back to client-loaded terrain. Discoveries arrive individually,
with temporary HUD pings and optional Xaero/FTB map markers. Completion is silent.
Scans continue while the tool is stored; Shift-right-click or reopen the selector
to cancel. All three radios share graphite cases, rubber, neutral-gray controls,
muted-teal keys and coarse radar symbols, with progressively more hardware.

**Resource packs:** Surveyor parts/screens, scanner HUD and menu artwork, and
owned core overlays have editable PNGs in this mod's resources. See the
[texture paths and resource-pack guide](RESOURCE_PACKS.md).

**Requires:** NeoForge 21.1.251+, Create 6.0.10+, and
[Create: Molten Vents 2.1.1+](https://github.com/Apothicon02/Create-Molten-Vents).

**Languages:** all 143 Minecraft language options have complete language files.
Initial translations and regional variants are included; 23 rare or fictional
options use English fallback, and some newer strings also await translation.
See the [language coverage and contribution guide](TRANSLATIONS.md). Builds
verify language coverage, keys and formatting arguments automatically.

**Build:** JDK 21, then `./gradlew build` or `gradlew.bat build` on Windows.
Run `./gradlew validateTranslations` to check language resources separately.
Upload `build/libs/molten_vents_plus-1.0.0.jar` as the mod; the separate
`-sources.jar` is for source distribution. Tests, development worlds, caches and
local verification reports are excluded from the source repository and JARs.

**License:** [MIT](LICENSE). Derived work retains the
[Molten Vents](LICENSES/Molten-Vents-MIT.txt) and
[Create Quality of Life](LICENSES/Create-Quality-of-Life-MIT.txt) MIT notices.
Ordinary Minecraft/Create blocks retain their dependency textures. Water core
PNGs retain the default water artwork with the mod's existing colors baked in.
