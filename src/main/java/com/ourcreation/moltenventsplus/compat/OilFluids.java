package com.ourcreation.moltenventsplus.compat;

import com.ourcreation.moltenventsplus.MoltenVentsPlus;
import com.ourcreation.moltenventsplus.VentConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class OilFluids {
    public static final TagKey<Fluid> OUTPUTS = TagKey.create(Registries.FLUID,
            ResourceLocation.fromNamespaceAndPath(MoltenVentsPlus.MODID, "oil_outputs"));

    public static boolean allowed(Fluid fluid) {
        var state = fluid.defaultFluidState();
        // Virtual, flowing and vanilla fluids cannot become an oil outlet.
        return fluid != Fluids.EMPTY && fluid != Fluids.WATER && fluid != Fluids.LAVA
                && state.isSource() && state.is(OUTPUTS)
                && state.createLegacyBlock().getBlock() instanceof LiquidBlock;
    }

    public static List<Fluid> outputs() {
        return BuiltInRegistries.FLUID.stream().filter(OilFluids::allowed)
                .sorted(Comparator.comparing(fluid -> BuiltInRegistries.FLUID.getKey(fluid).toString())).toList();
    }

    public static Optional<Fluid> preferred() {
        for (String value : VentConfig.OIL_PRIORITY.get()) {
            var id = ResourceLocation.tryParse(value);
            if (id == null) continue;
            var fluid = BuiltInRegistries.FLUID.getOptional(id).filter(OilFluids::allowed);
            if (fluid.isPresent()) return fluid;
        }
        return outputs().stream().findFirst();
    }

    private OilFluids() {}
}
