package com.ourcreation.moltenventsplus.compat;

import com.ourcreation.moltenventsplus.registry.ResourceVentRegistry;
import com.ourcreation.moltenventsplus.vent.VentTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Dormant cores have no block entity; activation installs this ticking block. */
public final class ActiveResourceVentBlock extends ResourceVentBlock implements EntityBlock {
    public ActiveResourceVentBlock(VentResource resource, VentTier tier, Properties properties) {
        super(resource, tier, true, properties);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResourceVentBlockEntity(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ResourceVentRegistry.ENTITY.get()) return null;
        return (world, pos, blockState, entity) -> ((ResourceVentBlockEntity) entity).serverTick();
    }
}
