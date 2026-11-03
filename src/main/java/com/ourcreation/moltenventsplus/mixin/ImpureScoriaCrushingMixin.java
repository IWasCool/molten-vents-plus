package com.ourcreation.moltenventsplus.mixin;

import com.ourcreation.moltenventsplus.vent.OreStone;
import com.ourcreation.moltenventsplus.vent.VentTier;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Water Scoria quartz belongs to the millstone, including Create's milling fallback. */
@Mixin(value = CrushingWheelControllerBlockEntity.class, remap = false)
abstract class ImpureScoriaCrushingMixin {
    @Shadow public ProcessingInventory inventory;

    @Unique
    private boolean moltenVentsPlus$millstoneOnlyInput() {
        return inventory.getSlots() > 0
                && inventory.getStackInSlot(0).is(OreStone.SCORIA.output(VentTier.WATER).asItem());
    }

    @Inject(method = "findRecipe", at = @At("HEAD"), cancellable = true)
    private void moltenVentsPlus$skipWaterScoria(CallbackInfoReturnable<Optional<?>> callback) {
        if (moltenVentsPlus$millstoneOnlyInput()) callback.setReturnValue(Optional.empty());
    }

    @Inject(method = "applyRecipe", at = @At("HEAD"), cancellable = true)
    private void moltenVentsPlus$returnWaterScoria(CallbackInfo callback) {
        // Keep the original input for Create's normal outbound delivery instead of
        // clearing it through the no-recipe branch or rolling its milling output.
        if (moltenVentsPlus$millstoneOnlyInput()) callback.cancel();
    }
}
