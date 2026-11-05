package com.ourcreation.moltenventsplus.compat.jade;

import com.ourcreation.moltenventsplus.MoltenVentsPlus;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Optional Jade integration; the main mod never loads these classes without Jade. */
@WailaPlugin(MoltenVentsPlus.MODID)
public final class VentJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(VentUsesProvider.INSTANCE, Block.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // Original Molten Vents cores do not extend our VentBlock class.
        // The provider filters the shared base registration to exact vent cores.
        registration.registerBlockComponent(VentUsesProvider.INSTANCE, Block.class);
    }
}
