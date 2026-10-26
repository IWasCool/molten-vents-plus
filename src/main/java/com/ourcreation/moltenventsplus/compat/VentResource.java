package com.ourcreation.moltenventsplus.compat;

import com.ourcreation.moltenventsplus.registry.ResourceVentRegistry;
import com.ourcreation.moltenventsplus.vent.VentTier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/** Registry lookups keep the resource providers optional on both physical sides. */
public enum VentResource implements StringRepresentable {
    URANIUM("uranium", "create_water_vents:uranium_nugget", 0x86A44E),
    THORIUM("thorium", "createnuclear:thorium_nugget", 0x72B8B4),
    LEAD("lead", "createnuclear:lead_nugget", 0x777995),
    OIL("oil", "minecraft:air", 0x30272B);

    public final String id;
    public final ResourceLocation nugget;
    public final int color;

    VentResource(String id, String nugget, int color) {
        this.id = id;
        this.nugget = ResourceLocation.parse(nugget);
        this.color = color;
    }

    public Optional<Block> nativeOre() {
        return this == OIL ? Optional.empty() : BuiltInRegistries.BLOCK.getOptional(
                ResourceLocation.fromNamespaceAndPath("createnuclear", id + "_ore"));
    }

    public boolean available() {
        return this == OIL ? !OilFluids.outputs().isEmpty()
                : nativeOre().isPresent() && BuiltInRegistries.ITEM.containsKey(nugget);
    }

    public Optional<Block> output(VentTier tier) {
        if (this == OIL || !available()) return Optional.empty();
        return tier == VentTier.MOLTEN ? nativeOre()
                : Optional.of(ResourceVentRegistry.stone(this, tier == VentTier.WATER));
    }

    @Override public String getSerializedName() { return id; }
}
