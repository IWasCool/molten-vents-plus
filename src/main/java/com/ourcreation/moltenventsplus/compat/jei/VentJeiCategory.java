package com.ourcreation.moltenventsplus.compat.jei;

import com.ourcreation.moltenventsplus.vent.*;
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
import net.minecraft.world.level.material.Fluid;
import java.util.ArrayList;
import java.util.List;
import static com.ourcreation.moltenventsplus.MoltenVentsPlus.MODID;

/** Fluid contacts are JEI views of block behavior, never server crafting recipes. */
public final class VentJeiCategory implements IRecipeCategory<VentJeiCategory.Generation> {
    public static final RecipeType<Generation> TYPE=RecipeType.create(MODID,"vent_generation",Generation.class);
    public record Generation(VentTier tier,OreStone stone,Fluid fluid,ItemStack vent,ItemStack output) {}
    private final IDrawable icon,arrow;

    public VentJeiCategory(IGuiHelper gui) {
        icon=gui.createDrawableItemStack(new ItemStack(BuiltInRegistries.ITEM.get(ventId(VentTier.WATER,OreStone.ASURINE,true))));
        arrow=gui.getRecipeArrow();
    }

    public static ResourceLocation ventId(VentTier tier,OreStone stone,boolean active) {
        return VentRegistry.ventId(tier,stone,active);
    }

    public static List<Generation> recipes() {
        List<Generation> recipes=new ArrayList<>();
        for(var tier:VentTier.values())for(var stone:OreStone.values())for(var fluid:VentFluids.inputs(tier)) {
            var item=BuiltInRegistries.ITEM.getOptional(ventId(tier,stone,true));
            if(item.isPresent())recipes.add(new Generation(tier,stone,fluid,item.get().getDefaultInstance(),new ItemStack(stone.output(tier))));
        }
        return List.copyOf(recipes);
    }

    @Override public RecipeType<Generation> getRecipeType(){return TYPE;}
    @Override public Component getTitle(){return text("title");}
    @Override public int getWidth(){return 178;}
    @Override public int getHeight(){return 54;}
    @Override public IDrawable getIcon(){return icon;}
    @Override public ResourceLocation getRegistryName(Generation recipe){return ResourceLocation.fromNamespaceAndPath(MODID,"generation/"+recipe.tier.id+"/"+recipe.stone.id+"/"+BuiltInRegistries.FLUID.getKey(recipe.fluid).toString().replace(':','/'));}

    @Override public void setRecipe(IRecipeLayoutBuilder builder,Generation recipe,IFocusGroup focuses) {
        builder.addInputSlot(20,8).setStandardSlotBackground().addFluidStack(recipe.fluid,1000);
        builder.addSlot(RecipeIngredientRole.CATALYST,78,8).setStandardSlotBackground().addItemStack(recipe.vent)
                .addRichTooltipCallback((slot,tooltip)->{tooltip.add(text("reusable"));tooltip.add(text("activation"));});
        builder.addOutputSlot(148,8).setOutputSlotBackground().addItemStack(recipe.output);
    }

    @Override public void draw(Generation recipe,IRecipeSlotsView slots,GuiGraphics graphics,double mouseX,double mouseY) {
        var font=Minecraft.getInstance().font;
        graphics.drawString(font,"+",54,12,0xFFFFFFFF,false);
        String amount="1000 mB";
        graphics.drawString(font,amount,28-font.width(amount)/2,28,0xFFFFFFFF,false);
        arrow.draw(graphics,108,16-arrow.getHeight()/2);
        graphics.drawString(font,text(recipe.tier.id),2,42,0xFFFFFFFF,false);
    }

    private static Component text(String key){return Component.translatable(MODID+".jei."+key).withStyle(ChatFormatting.WHITE);}
}
