package com.ourcreation.moltenventsplus.vent;

import com.ourcreation.moltenventsplus.registry.VentRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class VentBlock extends Block {
    public final VentTier tier;
    public final OreStone stone;
    public final boolean active;

    public VentBlock(VentTier tier, OreStone stone, boolean active, Properties properties) {
        super(properties);
        this.tier = tier;
        this.stone = stone;
        this.active = active;
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (!active && !level.isClientSide) {
            level.setBlock(pos, VentRegistry.block(tier, stone, true).defaultBlockState(), UPDATE_ALL);
        }
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!active || random.nextInt(3) != 0) return;
        double x = pos.getX() + random.nextDouble(), z = pos.getZ() + random.nextDouble();
        level.addParticle(tier == VentTier.WATER ? ParticleTypes.DRIPPING_WATER : ParticleTypes.SMALL_FLAME,
                x, pos.getY() + 1.02, z, 0, 0.03, 0);
        if (tier == VentTier.WATER) {
            level.addParticle(ParticleTypes.SPLASH, x, pos.getY() + 1.01, z, 0, 0.08, 0);
        }
    }
}
