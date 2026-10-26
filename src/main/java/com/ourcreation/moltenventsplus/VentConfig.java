package com.ourcreation.moltenventsplus;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class VentConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue WATER_INTERVAL;
    public static final ModConfigSpec.IntValue MEGA_INTERVAL;
    public static final ModConfigSpec.BooleanValue WATER_WORLDGEN;
    public static final ModConfigSpec.BooleanValue MEGA_WORLDGEN;
    public static final ModConfigSpec.BooleanValue OIL_WORLDGEN;
    public static final ModConfigSpec.IntValue OIL_INTERVAL;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> OIL_PRIORITY;

    static {
        var builder = new ModConfigSpec.Builder();
        WATER_INTERVAL = builder.comment("Ticks between bounded source-water conversion passes.")
                .defineInRange("waterInterval", 20, 1, 1200);
        MEGA_INTERVAL = builder.comment("Ticks between bounded superheated-fluid conversion passes.")
                .defineInRange("megaInterval", 10, 1, 1200);
        WATER_WORLDGEN = builder.define("generateWaterVents", true);
        MEGA_WORLDGEN = builder.define("generateMegaVents", true);
        OIL_WORLDGEN = builder.comment("Generate oil vents in the oil biome tags, only when a placeable tagged oil source exists.")
                .define("generateOilVents", true);
        OIL_INTERVAL = builder.comment("Ticks per oil source refill directly above an active oil core (1000 mB per refill).")
                .defineInRange("oilInterval", 100, 1, 1200);
        OIL_PRIORITY = builder.comment("Preferred native oil source IDs for an unassigned vent. Saved vents retain their selected fluid.")
                .defineListAllowEmpty("oilFluidPriority", java.util.List.of("createdieselgenerators:crude_oil",
                        "tfmg:crude_oil", "immersivepetroleum:crudeoil", "petrochem:petroleum"),
                        value -> value instanceof String id && net.minecraft.resources.ResourceLocation.tryParse(id) != null);
        SPEC = builder.build();
    }

    private VentConfig() {}
}
