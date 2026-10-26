package com.ourcreation.moltenventsplus.vent;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import java.util.List;
import java.util.Optional;

public final class VentFluids {
    public static List<Fluid> inputs(VentTier tier) {
        return BuiltInRegistries.FLUID.stream()
                .filter(fluid -> fluid.defaultFluidState().isSource() && fluid.defaultFluidState().is(tier.input))
                .toList();
    }

    public static Optional<Fluid> primary(VentTier tier) {
        var loaded = inputs(tier);
        if (!loaded.isEmpty()) return Optional.of(loaded.getFirst());
        return switch (tier) {
            case WATER -> Optional.of(Fluids.WATER);
            case MOLTEN -> Optional.of(Fluids.LAVA);
            case SUPERHEATED -> BuiltInRegistries.FLUID.getOptional(ResourceLocation.parse("createqol:superheated_lava"));
        };
    }

    private VentFluids() {}
}
