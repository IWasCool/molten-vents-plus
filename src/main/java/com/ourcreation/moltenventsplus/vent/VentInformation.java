package com.ourcreation.moltenventsplus.vent;

import com.ourcreation.moltenventsplus.MoltenVentsPlus;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid=MoltenVentsPlus.MODID)
public final class VentInformation {
    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        var item=event.getItemStack().getItem();
        var id=BuiltInRegistries.ITEM.getKey(item);
        if(!id.getNamespace().equals(MoltenVentsPlus.MODID))return;
        String key=null;
        if(id.getPath().startsWith("impure_"))key="impure";
        else if(id.getPath().startsWith("pure_"))key="pure";
        else if(item instanceof BlockItem block&&block.getBlock() instanceof VentBlock vent)key=vent.tier.id;
        else if(item instanceof BlockItem block&&block.getBlock() instanceof com.ourcreation.moltenventsplus.compat.ResourceVentBlock vent)
            key=vent.resource==com.ourcreation.moltenventsplus.compat.VentResource.OIL ? "oil" : vent.tier.id;
        if(key!=null)event.getToolTip().add(Component.translatable(MoltenVentsPlus.MODID+".tooltip."+key).withStyle(ChatFormatting.GRAY));
    }

    private VentInformation() {}
}
