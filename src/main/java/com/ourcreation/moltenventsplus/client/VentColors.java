package com.ourcreation.moltenventsplus.client;

import com.ourcreation.moltenventsplus.MoltenVentsPlus;
import com.ourcreation.moltenventsplus.registry.VentRegistry;
import com.ourcreation.moltenventsplus.registry.ResourceVentRegistry;
import com.ourcreation.moltenventsplus.compat.VentResource;
import com.ourcreation.moltenventsplus.compat.ResourceVentBlock;
import com.ourcreation.moltenventsplus.vent.VentBlock;
import com.ourcreation.moltenventsplus.vent.VentTier;
import com.ourcreation.moltenventsplus.vent.VentFluids;
import net.minecraft.world.item.BlockItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

@EventBusSubscriber(modid=MoltenVentsPlus.MODID, bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class VentColors {
    private static int core(VentTier tier, boolean active) {
        int color = switch (tier) {
            case WATER -> 0x3F76E4;
            case MOLTEN -> 0xFFFFFF;
            case SUPERHEATED -> VentFluids.primary(tier)
                    .map(fluid -> IClientFluidTypeExtensions.of(fluid).getTintColor() & 0xFFFFFF).orElse(0x5191EF);
        };
        return active ? color : (color >> 1) & 0x7F7F7F;
    }

    @SubscribeEvent
    public static void blocks(RegisterColorHandlersEvent.Block event) {
        ResourceVentRegistry.VENTS.values().forEach(block -> event.register((state, level, pos, index) -> {
            var vent = (ResourceVentBlock) state.getBlock();
            return vent.resource == VentResource.OIL ? vent.resource.color : core(vent.tier, vent.active);
        }, block.get()));
        VentRegistry.ENTRIES.forEach((name, block) -> {
            if (block.get() instanceof VentBlock)
                event.register((state, level, pos, index) -> {
                    var vent = (VentBlock) state.getBlock();
                    return core(vent.tier, vent.active);
                }, block.get());
        });
    }

    @SubscribeEvent
    public static void items(RegisterColorHandlersEvent.Item event) {
        ResourceVentRegistry.VENTS.values().forEach(block -> event.register((stack,index) ->
                event.getBlockColors().getColor(((BlockItem)stack.getItem()).getBlock().defaultBlockState(),null,null,index), block.get()));
        event.register((stack,index) -> VentResource.URANIUM.color, ResourceVentRegistry.URANIUM_NUGGET.get());
        VentRegistry.ENTRIES.forEach((name, block) -> {
            if (block.get() instanceof VentBlock)
                event.register((stack,index) -> event.getBlockColors().getColor(
                        ((BlockItem)stack.getItem()).getBlock().defaultBlockState(),null,null,index), block.get());
        });
        event.register((stack,index) -> 0x333333, VentRegistry.COAL_NUGGET.get());
        event.register((stack,index) -> 0x554343, VentRegistry.NETHERITE_NUGGET.get());
    }

    private VentColors() {}
}
