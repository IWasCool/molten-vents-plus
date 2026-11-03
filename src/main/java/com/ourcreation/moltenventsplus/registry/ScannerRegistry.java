package com.ourcreation.moltenventsplus.registry;

import com.ourcreation.moltenventsplus.MoltenVentsPlus;
import com.ourcreation.moltenventsplus.vent.VentTier;
import com.ourcreation.moltenventsplus.scanner.VentSurveyorItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ScannerRegistry {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MoltenVentsPlus.MODID);
    public static final DeferredItem<VentSurveyorItem> VENT_SURVEYOR = ITEMS.registerItem("vent_surveyor",
            properties -> new VentSurveyorItem(properties, VentTier.WATER));
    public static final DeferredItem<VentSurveyorItem> MOLTEN_VENT_SURVEYOR = ITEMS.registerItem("molten_vent_surveyor",
            properties -> new VentSurveyorItem(properties, VentTier.MOLTEN));
    public static final DeferredItem<VentSurveyorItem> SUPERHEATED_VENT_SURVEYOR = ITEMS.registerItem("superheated_vent_surveyor",
            properties -> new VentSurveyorItem(properties, VentTier.SUPERHEATED));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    private ScannerRegistry() {}
}
