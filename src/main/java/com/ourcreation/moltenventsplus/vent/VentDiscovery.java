package com.ourcreation.moltenventsplus.vent;

import com.ourcreation.moltenventsplus.MoltenVentsPlus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Native advancements retain discovery through death, reconnects and world saves. */
@EventBusSubscriber(modid=MoltenVentsPlus.MODID)
public final class VentDiscovery {
    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if(!(event.getEntity() instanceof ServerPlayer player)||player.tickCount%20!=0)return;
        if(!(player.pick(6,0,false) instanceof BlockHitResult hit))return;
        var block=player.level().getBlockState(hit.getBlockPos()).getBlock();
        String tier=null;
        if(block instanceof VentBlock vent)tier=vent.tier.id;
        else if(block instanceof com.ourcreation.moltenventsplus.compat.ResourceVentBlock vent)
            tier=vent.resource==com.ourcreation.moltenventsplus.compat.VentResource.OIL ? "oil" : vent.tier.id;
        else if(BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("molten_vents"))tier="molten";
        if(tier==null)return;
        for(String path:new String[]{"root",tier}) {
            var advancement=player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath(MoltenVentsPlus.MODID,"discovery/"+path));
            if(advancement!=null&&!player.getAdvancements().getOrStartProgress(advancement).isDone())
                player.getAdvancements().award(advancement,"discover");
        }
    }

    private VentDiscovery() {}
}
