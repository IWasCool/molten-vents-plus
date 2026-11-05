package com.ourcreation.moltenventsplus.mixin;

import com.Apothic0n.MoltenVents.core.objects.ActiveMoltenBlock;
import com.Apothic0n.MoltenVents.core.objects.DormantMoltenBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Apply bedrock properties to the original provider's cores without changing its JAR. */
@Mixin(value = {ActiveMoltenBlock.class, DormantMoltenBlock.class}, remap = false)
abstract class MoltenVentProtectionMixin extends Block {
    protected MoltenVentProtectionMixin(Properties properties) {
        super(properties);
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;<init>(Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V"), index = 0)
    private static BlockBehaviour.Properties moltenVentsPlus$bedrockProperties(BlockBehaviour.Properties properties) {
        return properties.strength(-1, 3600000).pushReaction(PushReaction.BLOCK);
    }

    @Override
    public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        wasExploded(level, pos, explosion);
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }
}
