package com.ourcreation.moltenventsplus.compat.emi;

import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Read the actual Create recipes, preserving output counts, fluids and chances. */
final class VentProcessingEmiRecipe extends BasicEmiRecipe {
    private final RecipeHolder<?> backing;
    private final int duration;

    VentProcessingEmiRecipe(EmiRecipeCategory category, RecipeHolder<?> holder, ProcessingRecipe<?, ?> recipe) {
        super(category, holder.id(), 178, 100);
        backing = holder;
        duration = recipe.getProcessingDuration();
        recipe.getIngredients().forEach(ingredient -> inputs.add(EmiIngredient.of(ingredient)));
        recipe.getFluidIngredients().forEach(ingredient -> inputs.add(EmiIngredient.of(java.util.stream.Stream.of(ingredient.getFluids())
                .map(fluid -> EmiStack.of(fluid.getFluid())).toList(), ingredient.amount())));
        recipe.getRollableResults().forEach(result -> outputs.add(EmiStack.of(result.getStack()).setChance(result.getChance())));
        recipe.getFluidResults().forEach(fluid -> outputs.add(EmiStack.of(fluid.getFluid(), fluid.getAmount())));
    }

    @Override public RecipeHolder<?> getBackingRecipe() { return backing; }
    @Override public void addWidgets(WidgetHolder widgets) {
        for (int i = 0; i < inputs.size(); i++) widgets.addSlot(inputs.get(i), 4 + i % 3 * 18, 8 + i / 3 * 18);
        widgets.addFillingArrow(76, 25, Math.max(1, duration) * 50);
        for (int i = 0; i < outputs.size(); i++) widgets.addSlot(outputs.get(i), 112 + i % 3 * 18, 8 + i / 3 * 18).recipeContext(this);
    }
}
