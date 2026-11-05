package com.ourcreation.moltenventsplus.vent;

import com.ourcreation.moltenventsplus.VentConfig;
import com.ourcreation.moltenventsplus.registry.VentRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
    private static final int WATER_RESERVE_VERSION = 1;
    private int waterRemaining = -1;
    private boolean waterReserveUpgradePending;

    public VentBlockEntity(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        super(VentRegistry.VENT_ENTITY.get(), pos, state);
    }

    /** Read-only: inspecting an unused or legacy core must not initialize its reserve. */
    public int remainingWaterUses() {
        return waterRemaining < 0 ? VentConfig.WATER_CAPACITY.get() : waterRemaining;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        waterRemaining = tag.contains("WaterRemaining", Tag.TAG_INT)
                ? Math.max(0, tag.getInt("WaterRemaining")) : -1;
        waterReserveUpgradePending = waterRemaining >= 0
                && tag.getInt("WaterReserveVersion") < WATER_RESERVE_VERSION;
        if (waterReserveUpgradePending) {
            // Upgrade old remaining uses once, without refilling exhausted cores.
            waterRemaining = (int) Math.min((long) waterRemaining * 4, Integer.MAX_VALUE);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (waterRemaining >= 0) {
            tag.putInt("WaterRemaining", waterRemaining);
            tag.putInt("WaterReserveVersion", WATER_RESERVE_VERSION);
        }
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel server) || !(getBlockState().getBlock() instanceof VentBlock vent)) return;
        if (waterReserveUpgradePending) {
            setChanged();
            waterReserveUpgradePending = false;
        }
        int interval = vent.tier == VentTier.WATER ? VentConfig.WATER_INTERVAL.get() : VentConfig.MEGA_INTERVAL.get();
        if (Math.floorMod(server.getGameTime() + worldPosition.asLong(), interval) != 0) return;
        convert(server, worldPosition, vent.tier, vent.stone);
    }

    /** A source becomes one block. Only matching output conducts, with explicit visit and distance limits. */
    public static int convert(ServerLevel level, BlockPos core, VentTier tier, OreStone stone) {
        return convert(level, core, tier, stone.output(tier));
    }

    public static int convert(ServerLevel level, BlockPos core, VentTier tier, Block output) {
        if (tier == VentTier.WATER) {
            if (level.getBlockEntity(core) instanceof VentBlockEntity entity
                    && entity.getBlockState().getBlock() instanceof VentBlock vent
                    && vent.active && vent.tier == VentTier.WATER) {
                return entity.convertWater(level, vent, output);
            }
            return 0;
        }
        return convert(level, core, tier, output, Integer.MAX_VALUE);
    }

    private int convertWater(ServerLevel server, VentBlock vent, Block output) {
        if (waterRemaining < 0) {
            // Legacy active vents receive their initial reserve on first use.
            // Saving it also preserves the reserve when Create moves the core.
            waterRemaining = VentConfig.WATER_CAPACITY.get();
            setChanged();
        }
        int converted = convert(server, worldPosition, VentTier.WATER, output, waterRemaining);
        if (converted > 0) {
            waterRemaining -= converted;
            setChanged();
        }
        if (waterRemaining == 0) {
            // A dormant core could be reactivated by another explosion. Spent
            // cores instead become ordinary mineable impure stone permanently.
            server.setBlock(worldPosition, vent.stone.output(VentTier.WATER).defaultBlockState(), Block.UPDATE_ALL);
        }
        return converted;
    }

    private static int convert(ServerLevel level, BlockPos core, VentTier tier, Block output, int limit) {
        if (limit <= 0) return 0;
        var queue = new ArrayDeque<BlockPos>();
        var visited = new HashSet<BlockPos>();
        queue.add(core);
        visited.add(core);
        int converted = 0;
        while (!queue.isEmpty() && visited.size() < tier.budget && converted < limit) {
            var current = queue.removeFirst();
            for (var direction : Direction.values()) {
                var pos = current.relative(direction);
                if (pos.distManhattan(core) > tier.range || !level.hasChunkAt(pos) || !visited.add(pos)) continue;
                if (level.getBlockEntity(pos) != null) continue;
                var state = level.getBlockState(pos);
                var fluid = state.getFluidState();
                if (state.getBlock() instanceof LiquidBlock && fluid.isSource() && fluid.is(tier.input)) {
                    if (!level.setBlock(pos, output.defaultBlockState(), Block.UPDATE_ALL)) continue;
                    converted++;
                    queue.addLast(pos);
                    level.sendParticles(tier == VentTier.WATER ? ParticleTypes.SPLASH : ParticleTypes.LARGE_SMOKE,
                            pos.getX()+0.5, pos.getY()+0.6, pos.getZ()+0.5, 4, 0.25, 0.2, 0.25, 0.02);
                } else if (state.is(output)) {
                    queue.addLast(pos);
                }
                if (visited.size() >= tier.budget || converted >= limit) break;
            }
        }
        if (converted > 0) level.playSound(null, core,
                tier == VentTier.WATER ? SoundEvents.BUCKET_EMPTY : SoundEvents.LAVA_EXTINGUISH,
                SoundSource.BLOCKS, 0.5F, 1.0F);
        return converted;
    }
}
