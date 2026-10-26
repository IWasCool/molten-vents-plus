package com.ourcreation.moltenventsplus.compat.jei;

import com.ourcreation.moltenventsplus.vent.*;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import static com.ourcreation.moltenventsplus.MoltenVentsPlus.MODID;

@JeiPlugin
public final class VentJeiPlugin implements IModPlugin {
    private static boolean enabled() { return !net.neoforged.fml.ModList.get().isLoaded("emi"); }
    @Override public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime){
        var hidden=new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
        if(com.ourcreation.moltenventsplus.registry.SuperheatedFluidRegistry.hasProvider()) {
            hidden.add(com.ourcreation.moltenventsplus.registry.SuperheatedFluidRegistry.BUCKET.get().getDefaultInstance());
            var fluid=new net.neoforged.neoforge.fluids.FluidStack(
                    com.ourcreation.moltenventsplus.registry.SuperheatedFluidRegistry.SOURCE.get(),1000);
            runtime.getIngredientManager().getIngredientTypeChecked(fluid).ifPresent(type ->
                    runtime.getIngredientManager().removeIngredientsAtRuntime(type,java.util.List.of(fluid)));
        }
        com.ourcreation.moltenventsplus.registry.ResourceVentRegistry.VENTS.values().forEach(entry->{
            if(!entry.get().resource.available())hidden.add(entry.get().asItem().getDefaultInstance());
        });
        for(var resource:com.ourcreation.moltenventsplus.compat.VentResource.values()){
            if(resource==com.ourcreation.moltenventsplus.compat.VentResource.OIL||resource.available())continue;
            hidden.add(new net.minecraft.world.item.ItemStack(com.ourcreation.moltenventsplus.registry.ResourceVentRegistry.stone(resource,true)));
            hidden.add(new net.minecraft.world.item.ItemStack(com.ourcreation.moltenventsplus.registry.ResourceVentRegistry.stone(resource,false)));
            if(resource==com.ourcreation.moltenventsplus.compat.VentResource.URANIUM)
                hidden.add(com.ourcreation.moltenventsplus.registry.ResourceVentRegistry.URANIUM_NUGGET.get().getDefaultInstance());
        }
        if(!hidden.isEmpty())runtime.getIngredientManager().removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK,hidden);
    }
    @Override public ResourceLocation getPluginUid(){return ResourceLocation.fromNamespaceAndPath(MODID,"vents");}
    @Override public void registerCategories(IRecipeCategoryRegistration registration){
        if(!enabled())return;
        var gui=registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new VentJeiCategory(gui),new ResourceVentJeiCategory(gui));
    }
    @Override public void registerRecipes(IRecipeRegistration registration){
        if(!enabled())return;
        registration.addRecipes(VentJeiCategory.TYPE,VentJeiCategory.recipes());
        registration.addRecipes(ResourceVentJeiCategory.TYPE,ResourceVentJeiCategory.recipes());
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration){
        if(!enabled())return;
        com.ourcreation.moltenventsplus.registry.ResourceVentRegistry.VENTS.values().forEach(entry->{
            var vent=entry.get();
            if(vent.resource.available()&&(vent.resource==com.ourcreation.moltenventsplus.compat.VentResource.OIL||!VentFluids.inputs(vent.tier).isEmpty()))
                registration.addRecipeCatalyst(vent.asItem().getDefaultInstance(),
                        vent.resource==com.ourcreation.moltenventsplus.compat.VentResource.OIL?ResourceVentJeiCategory.TYPE:VentJeiCategory.TYPE);
        });
        for(var tier:VentTier.values()) {
            if(VentFluids.inputs(tier).isEmpty())continue;
            for(var stone:OreStone.values())for(boolean active:new boolean[]{false,true})
                BuiltInRegistries.ITEM.getOptional(VentJeiCategory.ventId(tier,stone,active))
                        .ifPresent(item->registration.addRecipeCatalyst(item.getDefaultInstance(),VentJeiCategory.TYPE));
        }
    }
}
