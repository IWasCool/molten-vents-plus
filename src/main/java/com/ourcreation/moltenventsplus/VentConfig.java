package com.ourcreation.moltenventsplus;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class VentConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue WATER_INTERVAL;
    public static final ModConfigSpec.IntValue WATER_CAPACITY;
    public static final ModConfigSpec.IntValue MOLTEN_CAPACITY;
    public static final ModConfigSpec.BooleanValue ALLOW_VENT_MOVEMENT;
    public static final ModConfigSpec.IntValue MEGA_INTERVAL;
    public static final ModConfigSpec.BooleanValue WATER_WORLDGEN;
    public static final ModConfigSpec.BooleanValue MEGA_WORLDGEN;

    static {
        var builder = new ModConfigSpec.Builder();
        WATER_INTERVAL = builder.comment("Ticks between bounded source-water conversion passes.")
                .defineInRange("waterInterval", 20, 1, 1200);
        WATER_CAPACITY = builder.comment("Total impure stone blocks a water vent can produce before its core is exhausted.",
                        "Only successful conversions consume this reserve. Existing saved reserves are retained.")
                .defineInRange("waterCapacity", 4096, 1, Integer.MAX_VALUE);
        MOLTEN_CAPACITY = builder.comment("Maximum successful conversions per molten vent core, across all six materials.",
                        "-1 means unlimited. 0 exhausts a core without producing anything.",
                        "Finite reserves are saved in each core and retained across reloads and relocation.",
                        "Changing this value affects molten cores without an initialized finite reserve.")
                .defineInRange("moltenCapacity", -1, -1, Integer.MAX_VALUE);
        ALLOW_VENT_MOVEMENT = builder.comment("Allow active and dormant water, molten and superheated cores to move",
                        "with Create contraptions and Simulated/Sable assemblies. Default: false.",
                        "The saved cnwMoltenVentsMovable gamerule is an additional per-world opt-in.",
                        "To block movement, set this false and /disable movable_vents in that world.",
                        "This does not allow mining, drills, explosions or entities to break cores.")
                .define("allowVentMovement", false);
        MEGA_INTERVAL = builder.comment("Ticks between bounded superheated-fluid conversion passes.")
                .defineInRange("megaInterval", 10, 1, 1200);
        WATER_WORLDGEN = builder.define("generateWaterVents", true);
        MEGA_WORLDGEN = builder.define("generateMegaVents", true);
        SPEC = builder.build();
    }

    private VentConfig() {}
}
