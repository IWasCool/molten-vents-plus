package com.ourcreation.moltenventsplus.compat.jei;

import com.ourcreation.moltenventsplus.VentConfig;
import com.ourcreation.moltenventsplus.compat.OilFluids;
import com.ourcreation.moltenventsplus.compat.VentResource;
import com.ourcreation.moltenventsplus.registry.ResourceVentRegistry;
import com.ourcreation.moltenventsplus.vent.VentFluids;
import com.ourcreation.moltenventsplus.vent.VentTier;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.List;

import static com.ourcreation.moltenventsplus.MoltenVentsPlus.MODID;

public final class ResourceVentJeiCategory implements IRecipeCategory<ResourceVentJeiCategory.Generation> {
    public static final RecipeType<Generation> TYPE = RecipeType.create(MODID, "resource_vent_generation", Generation.class);
    public record Generation(VentResource resource, VentTier tier, Fluid fluid, ItemStack output) {}
    private final IDrawable icon;
    private final IDrawable arrow;

    public ResourceVentJeiCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemStack(new ItemStack(ResourceVentRegistry.block(VentResource.OIL, VentTier.WATER, true)));
        arrow = gui.getRecipeArrow();
    }

    public static List<Generation> recipes() {
        var recipes = new ArrayList<Generation>();
        for (var resource : VentResource.values()) {
            if (!resource.available()) continue;
            if (resource == VentResource.OIL) {
                for (var fluid : OilFluids.outputs()) recipes.add(new Generation(resource, VentTier.WATER, fluid, ItemStack.EMPTY));
            } else for (var tier : VentTier.values()) for (var fluid : VentFluids.inputs(tier))
                resource.output(tier).ifPresent(output -> recipes.add(new Generation(resource, tier, fluid, new ItemStack(output))));
        }
        return List.copyOf(recipes);
    }

    @Override public RecipeType<Generation> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return text("resources"); }
    @Override public int getWidth() { return 178; }
    @Override public int getHeight() { return 74; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public ResourceLocation getRegistryName(Generation recipe) {
        return ResourceLocation.fromNamespaceAndPath(MODID, "resource_generation/" + recipe.resource.id + "/"
                + recipe.tier.id + "/" + net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(recipe.fluid).toString().replace(':', '/'));
    }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, Generation recipe, IFocusGroup focuses) {
        boolean oil = recipe.resource == VentResource.OIL;
        if (!oil) builder.addInputSlot(20, 8).setStandardSlotBackground().addFluidStack(recipe.fluid, 1000);
        builder.addSlot(RecipeIngredientRole.CATALYST, oil ? 45 : 78, 8).setStandardSlotBackground()
                .addItemStack(new ItemStack(ResourceVentRegistry.block(recipe.resource, recipe.tier, true)))
                .addRichTooltipCallback((slot, tooltip) -> { tooltip.add(text("reusable")); tooltip.add(text("activation")); });
        if (oil) builder.addOutputSlot(135, 8).setOutputSlotBackground().addFluidStack(recipe.fluid, 1000);
        else builder.addOutputSlot(148, 8).setOutputSlotBackground().addItemStack(recipe.output);
    }

    @Override public void draw(Generation recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        if (recipe.resource == VentResource.OIL) {
            arrow.draw(graphics, 85, 16 - arrow.getHeight() / 2);
            graphics.drawString(font, Component.translatable(MODID + ".jei.oil_rate", VentConfig.OIL_INTERVAL.get()), 2, 33, 0xFFFFFFFF, false);
            graphics.drawString(font, text("oil_habitats"), 2, 47, 0xFFFFFFFF, false);
            graphics.drawString(font, text("oil_selection"), 2, 61, 0xFFFFFFFF, false);
        } else {
            graphics.drawString(font, "+", 54, 12, 0xFFFFFFFF, false);
            graphics.drawString(font, "1000 mB", 7, 30, 0xFFFFFFFF, false);
            arrow.draw(graphics, 108, 16 - arrow.getHeight() / 2);
            graphics.drawString(font, text(recipe.tier.id), 2, 49, 0xFFFFFFFF, false);
        }
    }

    private static Component text(String key) { return Component.translatable(MODID + ".jei." + key); }
}
