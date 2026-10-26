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
    SCORCHIA("scorchia", "minecraft:coal", 0.12F / 9),
    SCORIA("scoria", "minecraft:netherite_scrap", 0.12F / 9);

    public final String id;
    public final ResourceLocation millingOutput;
    public final float millingChance;

    OreStone(String id, String output) {
        this(id, output, 0.12F);
    }

    OreStone(String id, String output, float chance) {
        this.id = id;
        this.millingOutput = ResourceLocation.parse(output);
        this.millingChance = chance;
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
