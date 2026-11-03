package com.ourcreation.moltenventsplus.compat.emi;

import com.ourcreation.moltenventsplus.compat.*;
import com.ourcreation.moltenventsplus.registry.*;
import com.ourcreation.moltenventsplus.vent.*;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import dev.emi.emi.api.*;
import dev.emi.emi.api.recipe.*;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.fml.ModList;

import static com.ourcreation.moltenventsplus.MoltenVentsPlus.MODID;

/** Native EMI integration; Reliable EMI consumes the same categories and index. */
@EmiEntrypoint
public final class VentEmiPlugin implements EmiPlugin {
    public static final EmiRecipeCategory VENTS = category("vent_generation", ".jei.title",
            EmiStack.of(VentRegistry.block(VentTier.WATER, OreStone.ASURINE, true)));


    static EmiRecipeCategory category(String id, String translation, EmiStack icon) {
        return new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath(MODID, id), icon) {
            @Override public Component getName() {
                return Component.translatable(translation.startsWith(".") ? MODID + translation : translation);
            }
        };
    }

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(VENTS);
        VentGeneration.recipes().forEach(page -> registry.addRecipe(new MaterialRecipe(page)));
        for (var stone : OreStone.values()) for (var tier : VentTier.values()) for (boolean active : new boolean[]{false, true})
            registry.addWorkstation(VENTS, EmiStack.of(BuiltInRegistries.ITEM.get(VentRegistry.ventId(tier, stone, active))));
        registry.removeEmiStacks(stack -> {
            if (stack.getKey() instanceof Fluid fluid)
                return SuperheatedFluidRegistry.hasProvider() && SuperheatedFluidRegistry.isFallback(fluid);
            var item = stack.getItemStack();
            if (item.isEmpty()) return false;
            if (SuperheatedFluidRegistry.hasProvider() && item.is(SuperheatedFluidRegistry.BUCKET.get())) return true;
            return false;
        });
        // With JEI installed Create already supplies its real processing recipes through JEMI.
        if (!ModList.get().isLoaded("jei")) registerProcessing(registry);
    }

    private static void registerProcessing(EmiRegistry registry) {
        var milling = category("milling", "block.create.millstone", workstation("millstone"));
        var crushing = category("crushing", "block.create.crushing_wheel", workstation("crushing_wheel"));
        var compacting = category("compacting", "block.create.mechanical_press", workstation("mechanical_press"));
        registry.addCategory(milling); registry.addCategory(crushing); registry.addCategory(compacting);
        registry.addWorkstation(milling, workstation("millstone"));
        registry.addWorkstation(crushing, workstation("crushing_wheel"));
        registry.addWorkstation(compacting, workstation("mechanical_press"));
        for (RecipeHolder<?> holder : registry.getRecipeManager().getRecipes()) {
            if (!holder.id().getNamespace().equals(MODID)
                    && !holder.id().equals(ResourceLocation.parse("createqol:compacting/superheated_lava"))) continue;
            if (!(holder.value() instanceof ProcessingRecipe<?, ?> recipe)) continue;
            EmiRecipeCategory category = recipe.getType() == AllRecipeTypes.MILLING.getType() ? milling
                    : recipe.getType() == AllRecipeTypes.CRUSHING.getType() ? crushing
                    : recipe.getType() == AllRecipeTypes.COMPACTING.getType() ? compacting : null;
            if (category != null) registry.addRecipe(new VentProcessingEmiRecipe(category, holder, recipe));
        }
    }

    private static EmiStack workstation(String name) {
        return EmiStack.of(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", name)));
    }

    public static final class MaterialRecipe extends BasicEmiRecipe {
        public final VentGeneration page;

        private MaterialRecipe(VentGeneration page) {
            super(VENTS, ResourceLocation.fromNamespaceAndPath(MODID, "/" + page.id().getPath()), 178, 96);
            this.page = page;
            for (var row : page.rows()) {
                inputs.add(EmiIngredient.of(row.fluids().stream().map(EmiStack::of).toList(), 1000));
                catalysts.add(EmiStack.of(row.vent()));
                outputs.add(EmiStack.of(row.output()));
            }
        }

        @Override public boolean supportsRecipeTree() { return false; }
        @Override public void addWidgets(WidgetHolder widgets) {
            for (int i = 0; i < page.rows().size(); i++) {
                int y = 7 + i * 30;
                widgets.addSlot(inputs.get(i), 19, y);
                widgets.addText(Component.literal("+"), 54, y + 5, 0xFFFFFF, false);
                widgets.addSlot(catalysts.get(i), 77, y).catalyst(true);
                widgets.addTexture(EmiTexture.EMPTY_ARROW, 107, y + 1);
                widgets.addSlot(outputs.get(i), 147, y).recipeContext(this);
            }
        }
    }

}
