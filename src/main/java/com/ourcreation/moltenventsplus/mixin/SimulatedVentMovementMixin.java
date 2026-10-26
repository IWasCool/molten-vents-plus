package com.ourcreation.moltenventsplus.mixin;

import com.ourcreation.moltenventsplus.vent.VentSettings;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Simulated assembles Sable sublevels without consulting Create's movement checks. */
@Pseudo
@Mixin(targets = "dev.simulated_team.simulated.util.assembly.SimAssemblyContraption", remap = false)
abstract class SimulatedVentMovementMixin {
    @Inject(method = "movementAllowed", at = @At("HEAD"), cancellable = true)
    private void moltenVentsPlus$applyVentMovementRule(
            BlockState state, Level level, BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        if (VentSettings.isVent(state)) {
            callback.setReturnValue(VentSettings.movementEnabled(level));
        }
    }
}
