package com.ourcreation.moltenventsplus.compat.jei;

import com.ourcreation.moltenventsplus.vent.*;
import com.ourcreation.moltenventsplus.compat.VentGeneration;
import com.ourcreation.moltenventsplus.registry.VentRegistry;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import static com.ourcreation.moltenventsplus.MoltenVentsPlus.MODID;

/** Fluid contacts are JEI views of block behavior, never server crafting recipes. */
public final class VentJeiCategory implements IRecipeCategory<VentGeneration> {
    public static final RecipeType<VentGeneration> TYPE=RecipeType.create(MODID,"vent_generation",VentGeneration.class);
    private final IDrawable icon,arrow;

    public VentJeiCategory(IGuiHelper gui) {
        icon=gui.createDrawableItemStack(new ItemStack(BuiltInRegistries.ITEM.get(ventId(VentTier.WATER,OreStone.ASURINE,true))));
        arrow=gui.getRecipeArrow();
    }

    public static ResourceLocation ventId(VentTier tier,OreStone stone,boolean active) {
        return VentRegistry.ventId(tier,stone,active);
    }

    public static List<VentGeneration> recipes() { return VentGeneration.recipes(); }

    @Override public RecipeType<VentGeneration> getRecipeType(){return TYPE;}
    @Override public Component getTitle(){return text("title");}
    @Override public int getWidth(){return 178;}
    @Override public int getHeight(){return 96;}
    @Override public IDrawable getIcon(){return icon;}
    @Override public ResourceLocation getRegistryName(VentGeneration recipe){return recipe.id();}

    @Override public void setRecipe(IRecipeLayoutBuilder builder,VentGeneration recipe,IFocusGroup focuses) {
        for(int i=0;i<recipe.rows().size();i++) {
            var row=recipe.rows().get(i);int y=8+i*30;
            var input=builder.addInputSlot(20,y).setStandardSlotBackground();
            row.fluids().forEach(fluid -> input.addFluidStack(fluid,1000));
            builder.addSlot(RecipeIngredientRole.CATALYST,78,y).setStandardSlotBackground().addItemStack(row.vent());
            builder.addOutputSlot(148,y).setOutputSlotBackground().addItemStack(row.output());
        }
    }

    @Override public void draw(VentGeneration recipe,IRecipeSlotsView slots,GuiGraphics graphics,double mouseX,double mouseY) {
        var font=Minecraft.getInstance().font;
        for(int i=0;i<recipe.rows().size();i++) {
            int y=8+i*30;
            graphics.drawString(font,"+",54,y+4,0xFFFFFFFF,false);
            arrow.draw(graphics,108,y+8-arrow.getHeight()/2);
        }
    }

    private static Component text(String key){return Component.translatable(MODID+".jei."+key).withStyle(ChatFormatting.WHITE);}
}
