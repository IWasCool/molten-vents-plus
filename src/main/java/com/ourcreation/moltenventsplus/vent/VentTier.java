package com.ourcreation.moltenventsplus.vent;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

import static com.ourcreation.moltenventsplus.MoltenVentsPlus.MODID;

public enum VentTier {
    WATER("water", "impure_", 3, 128),
    MOLTEN("molten", "", 5, 256),
    SUPERHEATED("superheated", "pure_", 8, 512);

    public final String id;
    public final String outputPrefix;
    public final int range;
    public final int budget;
    public final TagKey<Fluid> input;

    VentTier(String id, String outputPrefix, int range, int budget) {
        this.id = id;
        this.outputPrefix = outputPrefix;
        this.range = range;
        this.budget = budget;
        input = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath(MODID, id + "_inputs"));
    }

    public static VentTier byId(String id) {
        for (var tier : values()) if (tier.id.equals(id)) return tier;
        throw new IllegalArgumentException("Unknown vent tier: " + id);
    }
}
