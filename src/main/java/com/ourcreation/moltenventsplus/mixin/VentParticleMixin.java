package com.ourcreation.moltenventsplus.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.ourcreation.moltenventsplus.client.VentParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientLevel.class)
abstract class VentParticleMixin {
    @WrapWithCondition(method = "doAnimateTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;animateTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private boolean moltenVentsPlus$allowNativeParticles(Block block, BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Preserve other mods' animate-tick hooks, including Blueprint's redirect.
        return !VentParticles.animate(block, state, level, pos, random);
    }
}
