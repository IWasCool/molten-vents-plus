package com.ourcreation.moltenventsplus.mixin;

import com.ourcreation.moltenventsplus.vent.VentSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Also stop machines that call destroyBlock directly instead of checking hardness. */
@Mixin(Level.class)
abstract class VentDestructionMixin {
    @Inject(method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",
            at = @At("HEAD"), cancellable = true)
    private void moltenVentsPlus$protectCore(BlockPos pos, boolean drops, Entity entity, int recursion,
                                            CallbackInfoReturnable<Boolean> callback) {
        if (VentSettings.isVent(((Level) (Object) this).getBlockState(pos))) callback.setReturnValue(false);
    }
}
