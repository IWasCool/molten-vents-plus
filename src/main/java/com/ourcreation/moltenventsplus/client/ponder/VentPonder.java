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
        for(var resource:com.ourcreation.moltenventsplus.compat.VentResource.values()){
            if(resource==com.ourcreation.moltenventsplus.compat.VentResource.OIL){
                // Ponder indexes before fluid tags are bound. Resolve the native oil when the scene plays.
                helper.forComponents(com.ourcreation.moltenventsplus.registry.ResourceVentRegistry.id(resource,VentTier.WATER,false),
                                com.ourcreation.moltenventsplus.registry.ResourceVentRegistry.id(resource,VentTier.WATER,true))
                        .addStoryBoard("vent/oil_automation",OilVentScene::build);
                continue;
            }
            if(!resource.available())continue;
            for(var tier:VentTier.values()){
                helper.forComponents(com.ourcreation.moltenventsplus.registry.ResourceVentRegistry.id(resource,tier,false),
                                com.ourcreation.moltenventsplus.registry.ResourceVentRegistry.id(resource,tier,true),
                                net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(resource.output(tier).orElseThrow()))
                        .addStoryBoard("vent/"+tier.id+"_activation",(scene,util)->VentScenes.activation(scene,util,tier,resource.id))
                        .addStoryBoard("vent/"+tier.id+"_automation",(scene,util)->VentScenes.automation(scene,util,tier,resource.id));
            }
        }
        for(var tier:VentTier.values())for(var stone:OreStone.values()){
            helper.forComponents(VentRegistry.ventId(tier,stone,false),VentRegistry.ventId(tier,stone,true),stone.outputId(tier))
                    .addStoryBoard("vent/"+tier.id+"_activation",(scene,util)->VentScenes.activation(scene,util,tier,stone.id))
                    .addStoryBoard("vent/"+tier.id+"_automation",(scene,util)->VentScenes.automation(scene,util,tier,stone.id));
        }
    }
}
