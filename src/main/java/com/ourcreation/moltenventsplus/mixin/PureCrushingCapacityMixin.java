package com.ourcreation.moltenventsplus.mixin;

import com.ourcreation.moltenventsplus.MoltenVentsPlus;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

/** A full pure-stone batch needs 32 output stacks in addition to Create's input slot. */
@Mixin(value = CrushingWheelControllerBlockEntity.class, remap = false)
abstract class PureCrushingCapacityMixin {
    @Shadow public ProcessingInventory inventory;

    @Inject(method = "applyRecipe", at = @At("HEAD"))
    private void moltenVentsPlus$expandPureOutputCapacity(CallbackInfo callback) {
        int slots = inventory.getSlots();
        if (slots == 0 || slots >= 33) return;
        var input = inventory.getStackInSlot(0);
        if (input.isEmpty()) return;
        var id = BuiltInRegistries.ITEM.getKey(input.getItem());
        if (!id.getNamespace().equals(MoltenVentsPlus.MODID)) return;
        boolean pure = switch (id.getPath()) {
            case "pure_asurine", "pure_veridium", "pure_crimsite", "pure_ochrum",
                    "pure_scorchia", "pure_scoria" -> true;
            default -> false;
        };
        if (!pure) return;

        // setSize clears stacks. Retain the same handler and restore every existing
        // stack so the RecipeWrapper, callbacks and processing metadata stay intact.
        var previous = new ArrayList<ItemStack>(slots);
        for (int slot = 0; slot < slots; slot++) previous.add(inventory.getStackInSlot(slot).copy());
        inventory.setSize(33);
        for (int slot = 0; slot < slots; slot++) inventory.setStackInSlot(slot, previous.get(slot));
        ((CrushingWheelControllerBlockEntity) (Object) this).setChanged();
    }
}
