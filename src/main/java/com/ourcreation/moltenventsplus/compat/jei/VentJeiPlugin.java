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
        var manager = runtime.getRecipeManager();
        manager.getRecipeType(ResourceLocation.fromNamespaceAndPath("create", "crushing"))
                .ifPresent(type -> hideMillstoneOnlyScoria(manager, type));
        var hidden=new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
        if(com.ourcreation.moltenventsplus.registry.SuperheatedFluidRegistry.hasProvider()) {
            hidden.add(com.ourcreation.moltenventsplus.registry.SuperheatedFluidRegistry.BUCKET.get().getDefaultInstance());
            var fluid=new net.neoforged.neoforge.fluids.FluidStack(
                    com.ourcreation.moltenventsplus.registry.SuperheatedFluidRegistry.SOURCE.get(),1000);
            runtime.getIngredientManager().getIngredientTypeChecked(fluid).ifPresent(type ->
                    runtime.getIngredientManager().removeIngredientsAtRuntime(type,java.util.List.of(fluid)));
        }
        if(!hidden.isEmpty())runtime.getIngredientManager().removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK,hidden);
    }
    private static <T> void hideMillstoneOnlyScoria(mezz.jei.api.recipe.IRecipeManager manager,
                                                   mezz.jei.api.recipe.RecipeType<T> type) {
        var recipeId = ResourceLocation.fromNamespaceAndPath(MODID, "milling/impure_scoria");
        var fallback = manager.createRecipeLookup(type).get()
                .filter(recipe -> recipe instanceof net.minecraft.world.item.crafting.RecipeHolder<?> holder
                        && holder.id().equals(recipeId)).toList();
        manager.hideRecipes(type, fallback);
    }
    @Override public ResourceLocation getPluginUid(){return ResourceLocation.fromNamespaceAndPath(MODID,"vents");}
    @Override public void registerCategories(IRecipeCategoryRegistration registration){
        if(!enabled())return;
        var gui=registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new VentJeiCategory(gui));
    }
    @Override public void registerRecipes(IRecipeRegistration registration){
        if(!enabled())return;
        registration.addRecipes(VentJeiCategory.TYPE,VentJeiCategory.recipes());
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration){
        if(!enabled())return;
        for(var tier:VentTier.values()) {
            if(VentFluids.inputs(tier).isEmpty())continue;
            for(var stone:OreStone.values())for(boolean active:new boolean[]{false,true})
                BuiltInRegistries.ITEM.getOptional(VentJeiCategory.ventId(tier,stone,active))
                        .ifPresent(item->registration.addRecipeCatalyst(item.getDefaultInstance(),VentJeiCategory.TYPE));
        }
    }
}
