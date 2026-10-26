package com.ourcreation.moltenventsplus.vent;

import com.ourcreation.moltenventsplus.registry.VentRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

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

}
