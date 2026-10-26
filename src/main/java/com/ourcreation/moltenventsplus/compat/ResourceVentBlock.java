package com.ourcreation.moltenventsplus.compat;

import com.ourcreation.moltenventsplus.registry.ResourceVentRegistry;
import com.ourcreation.moltenventsplus.vent.VentTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

public class ResourceVentBlock extends Block {
    public final VentResource resource;
    public final VentTier tier;
    public final boolean active;

    public ResourceVentBlock(VentResource resource, VentTier tier, boolean active, Properties properties) {
        super(properties);
        this.resource = resource;
        this.tier = tier;
        this.active = active;
    }

    @Override public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (!active && !level.isClientSide)
            level.setBlock(pos, ResourceVentRegistry.block(resource, tier, true).defaultBlockState(), UPDATE_ALL);
    }

    @Override public boolean dropFromExplosion(Explosion explosion) { return false; }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                                       BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (resource == VentResource.OIL && active) {
            var contained = FluidUtil.getFluidContained(stack);
            if (contained.isPresent() && OilFluids.allowed(contained.get().getFluid())) {
                if (!level.isClientSide && level.getBlockEntity(pos) instanceof ResourceVentBlockEntity entity) {
                    entity.selectOil(contained.get().getFluid());
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                            "create_water_vents.oil.selected", contained.get().getHoverName()), true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!active || random.nextInt(3) != 0) return;
        level.addParticle(resource == VentResource.OIL ? ParticleTypes.SMOKE
                        : tier == VentTier.WATER ? ParticleTypes.DRIPPING_WATER
                        : tier == VentTier.SUPERHEATED ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
                pos.getX() + random.nextDouble(), pos.getY() + 1.02, pos.getZ() + random.nextDouble(), 0, 0.02, 0);
    }
}
