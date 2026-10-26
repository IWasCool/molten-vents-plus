package com.ourcreation.moltenventsplus.compat;

import com.ourcreation.moltenventsplus.registry.ResourceVentRegistry;
import com.ourcreation.moltenventsplus.registry.VentRegistry;
import com.ourcreation.moltenventsplus.vent.OreStone;
import com.ourcreation.moltenventsplus.vent.VentFluids;
import com.ourcreation.moltenventsplus.vent.VentTier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import java.util.ArrayList;
import java.util.List;
import static com.ourcreation.moltenventsplus.MoltenVentsPlus.MODID;

/** A material's independent fluid conversions, displayed together by recipe viewers. */
public record VentGeneration(ResourceLocation id, List<Row> rows) {
    public record Row(VentTier tier, List<Fluid> fluids, ItemStack vent, ItemStack output) {}

    public static List<VentGeneration> recipes() {
        var pages = new ArrayList<VentGeneration>();
        for (var stone : OreStone.values()) {
            var rows = new ArrayList<Row>();
            for (var tier : VentTier.values()) {
                var fluids = VentFluids.inputs(tier);
                if (fluids.isEmpty()) continue;
                BuiltInRegistries.ITEM.getOptional(VentRegistry.ventId(tier, stone, true))
                        .ifPresent(vent -> rows.add(new Row(tier, fluids, vent.getDefaultInstance(), new ItemStack(stone.output(tier)))));
            }
            if (!rows.isEmpty()) pages.add(page(stone.id, rows));
        }
        for (var resource : VentResource.values()) {
            if (resource == VentResource.OIL || !resource.available()) continue;
            var rows = new ArrayList<Row>();
            for (var tier : VentTier.values()) {
                var fluids = VentFluids.inputs(tier);
                if (fluids.isEmpty()) continue;
                resource.output(tier).ifPresent(output -> rows.add(new Row(tier, fluids,
                        new ItemStack(ResourceVentRegistry.block(resource, tier, true)), new ItemStack(output))));
            }
            if (!rows.isEmpty()) pages.add(page(resource.id, rows));
        }
        return List.copyOf(pages);
    }

    private static VentGeneration page(String material, List<Row> rows) {
        return new VentGeneration(ResourceLocation.fromNamespaceAndPath(MODID, "generation/" + material), List.copyOf(rows));
    }
}
