# Editing textures and making resource packs

The mod's namespace is `create_water_vents`, retained for saved-world compatibility.
Its editable PNGs are in
`src/main/resources/assets/create_water_vents/textures/` and at the same
`assets/create_water_vents/textures/` path inside the release JAR.

The Surveyor materials, screen art, HUD signals and material-wheel artwork are
real PNG resources. They do not depend on Minecraft concrete textures or
hardcoded RGB multipliers. Change a PNG to change its colors. Model geometry,
item poses, text, positioning and scan timing remain separate.

## Walkie-talkie / Surveyor textures

Files below are relative to `assets/create_water_vents/textures/item/surveyor/`.

| PNG | Default size | What it changes |
| --- | --- | --- |
| `case.png` | 16 x 16 | Main case, shoulders, bevels and extra receiver housings |
| `knob.png` | 16 x 16 | Neutral-gray tuning knob on all tiers |
| `trim.png` | 16 x 16 | Dial bases, antenna sockets, key mounts and receiver crossbar |
| `buttons.png` | 16 x 16 | Teal keys, scan paddle and gain roller |
| `rubber.png` | 16 x 16 | Antenna, receiver ends, screen backing and guards |
| `speaker.png` | 16 x 16 | Original 8 x 6 grille enlarged 2x in the top 16 x 12 pixels; lower swatch for sides |
| `screen.png` | 32 x 32 | Transparent radar ring, sweep and echoes for Water/Molten |
| `screen_advanced.png` | 32 x 32 | Superheated radar overlay with the additional echo |

Screen overlays retain their original modest full-bright lighting. Keep their
background transparent to reveal the rubber backing. Their entire PNG maps to
the display face. Model JSONs in `models/item/` expose texture aliases if a pack
needs different materials per tier. `vent_surveyor_handheld.json` holds shared
materials and all item poses; the three tier models hold the hardware.

## Scanner HUD and material wheel

Files below are relative to `assets/create_water_vents/textures/gui/`.

| PNG | Default size | Layout |
| --- | --- | --- |
| `surveyor_direction_strip.png` | 139 x 1 | Top HUD direction strip |
| `surveyor_pointer.png` | 7 x 7 | Small bearing diamond |
| `surveyor_pin.png` | 13 x 13 | Location diamond and center dot |
| `surveyor_pulse.png` | 41 x 615 | Fifteen vertical 41 x 41 pulse frames, small to large |
| `surveyor_wheel.png` | 101 x 101 | Radial selector with no hovered sector |
| `surveyor_wheel_selected.png` | 101 x 606 | Six complete 101 x 101 selector frames |

Wheel-selected frames follow the material order Asurine, Veridium, Crimsite,
Ochrum, Scorchia, Scoria. Include the full background in each selected frame.
Preserve the transparent center deadzone and outside corners. Animation frame
selection, positions and fading are controlled by the scanner; RGB and source
alpha come from the PNGs. These sheets use explicit frames in code, not
`.mcmeta` animation. Keep their aspect ratios and frame count when upscaling.

## Impure cracks, pure sparkles and vent core overlays

The actual transparent block overlays are in
`assets/create_water_vents/textures/block/overlays/`:

| PNG | What it changes |
| --- | --- |
| `impure_cracks_<face>.png` | Six supplied 16 x 16 crack faces shared by all impure materials |
| `pure_ore_sparkles_<face>.png` | Animated soft, full-bright sparkle faces shared by all pure materials |
| `pure_ore_sparkles_<face>_v1.png` through `_v15.png` | Independent animation choices for neighboring pure blocks |
| `vent_superheated_<material>.png` | Animated blue cracks on active and dormant superheated cores |
| `vent_water_<material>.png` | Animated active water-core cracks |
| `vent_water_dormant_<material>.png` | Animated dim dormant water-core cracks |

Replace `<face>` with `north`, `south`, `east`, `west`, `up` or `down`.
Each impure face has its own transparent PNG; keep its orientation so the cracks
meet at cube edges. All six materials, natural variants, cut blocks, slabs,
posts and inventory/dropped items use these faces. Slabs and posts retain their
native cropped UVs and pixel density. The old shared `impure_cracks.png` has
been replaced; resource packs targeting that path should override the six new
files instead. The unfolded 64 x 48 reference image is not a runtime texture.

Pure sparkles use the same six `<face>` names. Each PNG is a vertical strip of
48 native 16 x 16 frames (16 x 768 total) with matching `.png.mcmeta` metadata:
mostly two ticks per frame, with individual three- and four-tick holds for
uneven pulse timing. Loops vary from 5.2 to 5.6 seconds (about 12% longer than
the earlier 4.8-second loop). Longer pauses favor dim frames so the glow peaks
stay soft. The original PNG pixels are preserved. Each face also has fifteen
`_v<number>` copies with different frame starting points, forward/reverse
sequences and hold patterns in their metadata. Native weighted
multipart blockstates choose one of sixteen arrangements from each block's
position, so neighboring stones can twinkle independently. The choice stays
stable across chunk rebuilds; it adds no saved block properties or ticking code.
Occasional matching neighbors are possible. Inventory items use the default
animation. All six materials and every profile share these choices.

Small sparkle quads sample the original cross-shaped glyphs at half their old
width and height, retaining their spacing, soft fades and gold material. The
shared models are `models/block/overlays/pure_sparkles_<profile>_v<number>.json`,
where `<profile>` is `full`, `slab` or `post` and `<number>` is 0 through 15.
To change sparkle shapes, keep replacement art inside the sampled glyph cells
or update these models' explicit UV rectangles and positions. Override all
sixteen sprite copies to apply artwork to every animation choice; preserve
each copy's metadata if independent timing is desired. In the `frames` array,
an integer uses the default two-tick duration; an object such as
`{"index": 7, "time": 3}` displays source frame 7 for three ticks.

Pure blocks use a solid native stone model plus a translucent sparkle part;
pure items and impure blocks/items use NeoForge's built-in composite loader
with ordered base/overlay item passes. Overlay planes have explicit UVs and
remain inside each native shape, with a normal offset of 0.001 model units
to avoid z-fighting and atlas bleed. Slab and post glyphs are clipped to their
native faces. Native base geometry remains unchanged. No custom renderer is
needed. This replaces `pure_glints.png`; static alternatives and artwork
previews from the source ZIP are not bundled.

Overlay color and opacity now also have editable native model settings in
`models/block/impure_*.json` and `models/block/overlays/pure_sparkles_*.json`.
Each overlay element's
`neoforge_data.color` is an eight-digit ARGB hex string: the first two digits
set opacity, followed by red, green and blue. Pure sparkles use `80ffd078`
for a subdued gold at approximately half opacity. Impure cracks use deeper material-matched colors at full model opacity;
the supplied PNG alpha still blends the cracks softly over the stone.
Set the color to `ffffffff` to use a replacement PNG's unmodified RGBA.
These are native model materials; no Java renderer applies another tint.

Replace `<material>` with `asurine`, `veridium`, `crimsite`, `ochrum`,
`scorchia` or `scoria`. Each core PNG contains a vertical strip of 16 x 16
frames and has a matching `.png.mcmeta` animation file. Transparent pixels
reveal the ordinary stone underneath. Paint the PNG to change the crack shape
as well as its color; crack outlines are no longer carved into hundreds of
model faces. One shared twelve-quad model supplies the stone and overlay planes.
No Java color multiplier or optional fluid-provider sprite overrides these PNGs.

The pure sparkle PNGs retain the supplied pixels and existing full-bright face
data, with the gold/opacity treatment defined in the models; this does not add
particles or emitted block light.
The mod loads after the optional core so its legacy resource overrides cannot
hide these owned models and paths. The old full-stone `pure_<material>.png`
files and obsolete slab/post models have been removed.

Actual fluid animation and bucket artwork remain in `textures/fluid/` and
`textures/item/superheated_lava_bucket.png`; changing a core overlay does not
require changing the fluid itself. Fluid-provider gameplay and recipes are
unaffected.

Ordinary Create stones in generated formations and the original Molten Vents
blocks still belong to their required dependencies. Their textures are not
duplicated here. This guide covers this mod's Surveyors, HUD and owned core
overlays; it does not claim to replace every dependency's artwork.

## Minimal resource-pack layout for Minecraft 1.21.1

Make a folder or ZIP containing `pack.mcmeta` at its root:

```json
{
  "pack": {
    "pack_format": 34,
    "description": "My Molten Vents Plus textures"
  }
}
```

Include only the textures you change, with their exact paths, for example:

```text
pack.mcmeta
assets/create_water_vents/textures/item/surveyor/case.png
assets/create_water_vents/textures/item/surveyor/screen.png
assets/create_water_vents/textures/gui/surveyor_pin.png
```

Place the folder or ZIP in Minecraft's `resourcepacks` directory, enable it
above other packs, and reload resources with F3+T. No mod rebuild is needed.
For animated fluid/core textures, include the matching `.png.mcmeta` when
changing frame layout or timing. Use integer pixel scaling for crisp art.

The radio PNGs replace the older `item/vent_surveyor.png` and
`item/vent_surveyor_detail.png` atlases. Packs targeting those old paths must
move their overrides to the new files. HUD labels still use Minecraft's normal
font and language resources. Existing licenses and upstream attribution remain
in the repository and release JAR.
