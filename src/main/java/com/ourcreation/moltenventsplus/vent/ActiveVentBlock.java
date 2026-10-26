package com.ourcreation.moltenventsplus.vent;

import com.ourcreation.moltenventsplus.registry.VentRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public final class ActiveVentBlock extends VentBlock implements EntityBlock {
    public ActiveVentBlock(VentTier tier, OreStone stone, Properties properties) {
        super(tier, stone, true, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VentBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != VentRegistry.VENT_ENTITY.get()) return null;
        return (world, pos, blockState, entity) -> ((VentBlockEntity) entity).serverTick();
    }
}
