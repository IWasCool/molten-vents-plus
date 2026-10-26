package com.ourcreation.moltenventsplus.vent;

import com.ourcreation.moltenventsplus.registry.SuperheatedFluidRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import java.util.List;
import java.util.Optional;

public final class VentFluids {
    public static List<Fluid> inputs(VentTier tier) {
        return BuiltInRegistries.FLUID.stream()
                .filter(fluid -> fluid.defaultFluidState().isSource() && fluid.defaultFluidState().is(tier.input))
                .filter(fluid -> !SuperheatedFluidRegistry.hasProvider() || !SuperheatedFluidRegistry.isFallback(fluid))
                .toList();
    }

    public static Optional<Fluid> primary(VentTier tier) {
        if (tier == VentTier.SUPERHEATED)
            return BuiltInRegistries.FLUID.getOptional(SuperheatedFluidRegistry.QOL_FLUID)
                    .or(() -> Optional.of(SuperheatedFluidRegistry.SOURCE.get()));
        var loaded = inputs(tier);
        if (!loaded.isEmpty()) return Optional.of(loaded.getFirst());
        return Optional.of(tier == VentTier.WATER ? Fluids.WATER : Fluids.LAVA);
    }

    private VentFluids() {}
}
