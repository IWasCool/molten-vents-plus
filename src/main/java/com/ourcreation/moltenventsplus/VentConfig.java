package com.ourcreation.moltenventsplus;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class VentConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue WATER_INTERVAL;
    public static final ModConfigSpec.IntValue MEGA_INTERVAL;
    public static final ModConfigSpec.BooleanValue WATER_WORLDGEN;
    public static final ModConfigSpec.BooleanValue MEGA_WORLDGEN;

    static {
        var builder = new ModConfigSpec.Builder();
        WATER_INTERVAL = builder.comment("Ticks between bounded source-water conversion passes.")
                .defineInRange("waterInterval", 20, 1, 1200);
        MEGA_INTERVAL = builder.comment("Ticks between bounded superheated-fluid conversion passes.")
                .defineInRange("megaInterval", 10, 1, 1200);
        WATER_WORLDGEN = builder.define("generateWaterVents", true);
        MEGA_WORLDGEN = builder.define("generateMegaVents", true);
        SPEC = builder.build();
    }

    private VentConfig() {}
}
