package com.ourcreation.moltenventsplus;

import com.ourcreation.moltenventsplus.registry.VentRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(MoltenVentsPlus.MODID)
public final class MoltenVentsPlus {
    public static final String MODID = "create_water_vents";

    public MoltenVentsPlus(IEventBus bus, ModContainer container) {
        com.ourcreation.moltenventsplus.registry.SuperheatedFluidRegistry.register(bus);
        VentRegistry.register(bus);
        com.ourcreation.moltenventsplus.registry.ResourceVentRegistry.register(bus);
        com.ourcreation.moltenventsplus.vent.VentSettings.register();
        container.registerConfig(ModConfig.Type.COMMON, VentConfig.SPEC);
    }
}
