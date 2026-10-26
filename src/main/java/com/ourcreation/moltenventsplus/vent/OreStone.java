package com.ourcreation.moltenventsplus.vent;

import com.ourcreation.moltenventsplus.MoltenVentsPlus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;

public enum OreStone implements StringRepresentable {
    ASURINE("asurine", "create:zinc_nugget"),
    VERIDIUM("veridium", "create:copper_nugget"),
    CRIMSITE("crimsite", "minecraft:iron_nugget"),
    OCHRUM("ochrum", "minecraft:gold_nugget"),
    SCORCHIA("scorchia", "create_water_vents:coal_nugget"),
    SCORIA("scoria", "create_water_vents:netherite_nugget");

    public final String id;
    public final ResourceLocation nugget;

    OreStone(String id, String nugget) {
        this.id = id;
        this.nugget = ResourceLocation.parse(nugget);
    }

    public ResourceLocation outputId(VentTier tier) {
        return ResourceLocation.fromNamespaceAndPath(tier == VentTier.MOLTEN ? "create" : MoltenVentsPlus.MODID,
                tier.outputPrefix + id);
    }

    public Block output(VentTier tier) {
        return BuiltInRegistries.BLOCK.get(outputId(tier));
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
