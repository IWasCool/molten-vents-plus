package com.ourcreation.moltenventsplus;

import com.ourcreation.moltenventsplus.registry.VentRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(MoltenVentsPlus.MODID)
public final class MoltenVentsPlus {
    public static final String MODID = "create_water_vents";

    public MoltenVentsPlus(IEventBus bus, ModContainer container) {
        bus.addListener(MoltenVentsPlus::removedContent);
        com.ourcreation.moltenventsplus.registry.SuperheatedFluidRegistry.register(bus);
        VentRegistry.register(bus);
        com.ourcreation.moltenventsplus.registry.ResourceVentRegistry.register(bus);
        com.ourcreation.moltenventsplus.vent.VentSettings.register();
        container.registerConfig(ModConfig.Type.COMMON, VentConfig.SPEC);
    }

    /** Retired stacks become empty and retired mineral blocks become ordinary, inert stone. */
    private static void removedContent(RegisterEvent event) {
        var items = event.getRegistry(Registries.ITEM);
        var blocks = event.getRegistry(Registries.BLOCK);
        if (items != null) {
            for (var name : new String[]{"coal_nugget", "netherite_nugget", "uranium_nugget"})
                items.addAlias(ResourceLocation.fromNamespaceAndPath(MODID, name),
                        ResourceLocation.withDefaultNamespace("air"));
        }
        if (items == null && blocks == null) return;
        var retired = new java.util.ArrayList<String>();
        for (var mineral : new String[]{"uranium", "thorium", "lead"}) {
            retired.add("impure_" + mineral);
            retired.add("pure_" + mineral);
            for (var tier : new String[]{"water", "molten", "superheated"}) {
                retired.add("active_" + tier + "_" + mineral);
                retired.add("dormant_" + tier + "_" + mineral);
            }
        }
        for (var name : retired) {
            var id = ResourceLocation.fromNamespaceAndPath(MODID, name);
            if (items != null) items.addAlias(id, ResourceLocation.withDefaultNamespace("air"));
            if (blocks != null) blocks.addAlias(id, ResourceLocation.withDefaultNamespace("stone"));
        }
    }
}
