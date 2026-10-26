package com.ourcreation.moltenventsplus.mixin;

import com.ourcreation.moltenventsplus.client.VentRecipeAccess;
import com.ourcreation.moltenventsplus.compat.VentGeneration;
import mezz.jei.api.recipe.category.IRecipeCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A comparison page remains useful while any one of its independent tiers is accessible. */
@Pseudo
@Mixin(targets = "com.ourcreation.createnewworldcore.compat.jei.JeiLockedRecipes", remap = false)
abstract class VentDiscoveryMixin {
    @Inject(method = "visible", at = @At("HEAD"), cancellable = true, require = 0)
    private static void moltenVentsPlus$independentTierAccess(IRecipeCategory<?> category, Object recipe,
                                                            long version, CallbackInfoReturnable<Boolean> callback) {
        if (recipe instanceof VentGeneration page)
            callback.setReturnValue(page.rows().stream().anyMatch(row -> !VentRecipeAccess.isLocked(row.output())));
    }
}
