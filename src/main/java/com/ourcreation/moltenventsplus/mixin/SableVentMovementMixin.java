package com.ourcreation.moltenventsplus.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.ourcreation.moltenventsplus.vent.VentSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/** Sable 2 assemblies gather independently of Create and the older Simulated helper. */
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.api.SubLevelAssemblyHelper", remap = false)
abstract class SableVentMovementMixin {
    @WrapOperation(method = "gatherConnectedBlocks", at = @At(value = "INVOKE",
            target = "Ldev/ryanhcode/sable/util/LevelAccelerator;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState moltenVentsPlus$excludeCores(@Coerce Object accelerator, BlockPos pos,
                                                          Operation<BlockState> original,
                                                          BlockPos origin, ServerLevel level, int maxBlocks,
                                                          @Coerce Object predicate) {
        var state = original.call(accelerator, pos);
        // Air is only a search result here: the real world core is never edited.
        return VentSettings.isVent(state) && !VentSettings.movementEnabled(level)
                ? Blocks.AIR.defaultBlockState() : state;
    }
}
