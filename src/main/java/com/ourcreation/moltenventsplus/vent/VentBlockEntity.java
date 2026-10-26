package com.ourcreation.moltenventsplus.vent;

import com.ourcreation.moltenventsplus.VentConfig;
import com.ourcreation.moltenventsplus.registry.VentRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.ArrayDeque;
import java.util.HashSet;

public final class VentBlockEntity extends BlockEntity {
    public VentBlockEntity(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        super(VentRegistry.VENT_ENTITY.get(), pos, state);
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel server) || !(getBlockState().getBlock() instanceof VentBlock vent)) return;
        int interval = vent.tier == VentTier.WATER ? VentConfig.WATER_INTERVAL.get() : VentConfig.MEGA_INTERVAL.get();
        if (Math.floorMod(server.getGameTime() + worldPosition.asLong(), interval) != 0) return;
        convert(server, worldPosition, vent.tier, vent.stone);
    }

    /** A source becomes one block. Only matching output conducts, with explicit visit and distance limits. */
    public static int convert(ServerLevel level, BlockPos core, VentTier tier, OreStone stone) {
        return convert(level, core, tier, stone.output(tier));
    }

    public static int convert(ServerLevel level, BlockPos core, VentTier tier, Block output) {
        var queue = new ArrayDeque<BlockPos>();
        var visited = new HashSet<BlockPos>();
        queue.add(core);
        visited.add(core);
        int converted = 0;
        while (!queue.isEmpty() && visited.size() < tier.budget) {
            var current = queue.removeFirst();
            for (var direction : Direction.values()) {
                var pos = current.relative(direction);
                if (pos.distManhattan(core) > tier.range || !level.hasChunkAt(pos) || !visited.add(pos)) continue;
                if (level.getBlockEntity(pos) != null) continue;
                var state = level.getBlockState(pos);
                var fluid = state.getFluidState();
                if (state.getBlock() instanceof LiquidBlock && fluid.isSource() && fluid.is(tier.input)) {
                    level.setBlock(pos, output.defaultBlockState(), Block.UPDATE_ALL);
                    converted++;
                    queue.addLast(pos);
                    level.sendParticles(tier == VentTier.WATER ? ParticleTypes.SPLASH : ParticleTypes.LARGE_SMOKE,
                            pos.getX()+0.5, pos.getY()+0.6, pos.getZ()+0.5, 4, 0.25, 0.2, 0.25, 0.02);
                } else if (state.is(output)) {
                    queue.addLast(pos);
                }
                if (visited.size() >= tier.budget) break;
            }
        }
        if (converted > 0) level.playSound(null, core,
                tier == VentTier.WATER ? SoundEvents.BUCKET_EMPTY : SoundEvents.LAVA_EXTINGUISH,
                SoundSource.BLOCKS, 0.5F, 1.0F);
        return converted;
    }
}
