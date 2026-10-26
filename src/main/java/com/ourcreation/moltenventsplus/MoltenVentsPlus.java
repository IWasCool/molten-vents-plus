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
        bus.addListener(MoltenVentsPlus::removedNuggets);
        com.ourcreation.moltenventsplus.registry.SuperheatedFluidRegistry.register(bus);
        VentRegistry.register(bus);
        com.ourcreation.moltenventsplus.registry.ResourceVentRegistry.register(bus);
        com.ourcreation.moltenventsplus.vent.VentSettings.register();
        container.registerConfig(ModConfig.Type.COMMON, VentConfig.SPEC);
    }

    /** Removed nugget stacks become empty without leaving unresolved saved registry entries. */
    private static void removedNuggets(RegisterEvent event) {
        var items = event.getRegistry(Registries.ITEM);
        if (items == null) return;
        for (var name : new String[]{"coal_nugget", "netherite_nugget", "uranium_nugget"}) {
            items.addAlias(ResourceLocation.fromNamespaceAndPath(MODID, name),
                    ResourceLocation.withDefaultNamespace("air"));
        }
    }
}
