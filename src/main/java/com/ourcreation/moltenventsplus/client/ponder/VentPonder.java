package com.ourcreation.moltenventsplus.client.ponder;

import com.ourcreation.moltenventsplus.MoltenVentsPlus;
import com.ourcreation.moltenventsplus.registry.VentRegistry;
import com.ourcreation.moltenventsplus.vent.*;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid=MoltenVentsPlus.MODID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class VentPonder implements PonderPlugin {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){PonderIndex.addPlugin(new VentPonder());}
    @Override public String getModId(){return MoltenVentsPlus.MODID;}
    @Override public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper){
        for(var tier:VentTier.values())for(var stone:OreStone.values()){
            var scenes = helper.forComponents(VentRegistry.ventId(tier,stone,false),VentRegistry.ventId(tier,stone,true),stone.outputId(tier));
            scenes.addStoryBoard("vent/"+tier.id+"_activation",(scene,util)->VentScenes.activation(scene,util,tier,stone.id));
            if (tier == VentTier.WATER)
                scenes.addStoryBoard("vent/water_activation_variant",(scene,util)->VentScenes.activation(scene,util,tier,stone.id,true));
            scenes.addStoryBoard("vent/"+tier.id+"_automation",(scene,util)->VentScenes.automation(scene,util,tier,stone.id));
        }
    }
}
