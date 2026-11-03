package com.ourcreation.moltenventsplus.worldgen;

import com.ourcreation.moltenventsplus.vent.VentTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.HashMap;
import java.util.Optional;

/** Find a supported footprint without adding another placement roll or mineral grid. */
final class VentSite {
    record Site(BlockPos origin, boolean submerged) {}
    private record Column(int height, BlockState ground, boolean wet, boolean water, boolean supported) {}
    private static final int[][] SEARCH = {{0, 0}, {4, 0}, {-4, 0}, {0, 4}, {0, -4},
            {4, 4}, {-4, -4}, {-4, 4}, {4, -4}};

    static Optional<Site> find(Structure.GenerationContext context, int x, int z, int radius,
                               VentTier tier, boolean aquatic, boolean search) {
        var columns = new HashMap<Long, Column>();
        // Land/aquatic molten IDs must choose the same candidate before filtering its
        // environment, otherwise a shoreline search could produce both in one chunk.
        var heightmap = Heightmap.Types.OCEAN_FLOOR_WG;
        for (int[] offset : search ? SEARCH : new int[][]{{0, 0}}) {
            int px = search ? Math.clamp(x + offset[0], context.chunkPos().getMinBlockX(), context.chunkPos().getMaxBlockX()) : x;
            int pz = search ? Math.clamp(z + offset[1], context.chunkPos().getMinBlockZ(), context.chunkPos().getMaxBlockZ()) : z;
            Column center = column(context, columns, px, pz, heightmap);
            int y = center.height;
            if (y < context.heightAccessor().getMinBuildHeight() + GroundedVentDeposit.SUPPORT_DEPTH
                    || y > context.heightAccessor().getMaxBuildHeight() - 5 || !ground(center.ground) || !center.supported) continue;
            if (center.wet && !center.water || tier == VentTier.SUPERHEATED && center.wet) continue;
            if (tier == VentTier.MOLTEN && center.wet) {
                var base = context.chunkGenerator().getBaseColumn(px, pz, context.heightAccessor(), context.randomState());
                if (!base.getBlock(y + 3).is(Blocks.WATER)) continue;
            }
            var biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(px), QuartPos.fromBlock(y),
                    QuartPos.fromBlock(pz), context.randomState().sampler());
            if (!context.validBiome().test(biome)) continue;
            int tolerance = tier == VentTier.SUPERHEATED ? 3 : 2;
            boolean supported = true;
            for (int dx = -radius; dx <= radius && supported; dx++) for (int dz = -radius; dz <= radius; dz++) {
                Column edge = column(context, columns, px + dx, pz + dz, heightmap);
                if (!ground(edge.ground) || !edge.supported || Math.abs(edge.height - y) > tolerance
                        || edge.wet && !edge.water
                        || tier == VentTier.SUPERHEATED && edge.wet
                        || tier == VentTier.MOLTEN && edge.wet != center.wet) {
                    supported = false;
                    break;
                }
            }
            if (supported) return tier == VentTier.MOLTEN && aquatic != center.wet ? Optional.empty()
                    : Optional.of(new Site(new BlockPos(px, y, pz), center.wet));
        }
        return Optional.empty();
    }

    private static Column column(Structure.GenerationContext context, HashMap<Long, Column> cache,
                                  int x, int z, Heightmap.Types heightmap) {
        long key = BlockPos.asLong(x, 0, z);
        return cache.computeIfAbsent(key, ignored -> {
            var column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
            int y = context.heightAccessor().getMaxBuildHeight() - 1;
            int min = context.heightAccessor().getMinBuildHeight();
            while (y > min && !heightmap.isOpaque().test(column.getBlock(y))) y--;
            var fluid = column.getBlock(y + 1).getFluidState();
            boolean supported = false;
            for (int down = 1; down <= GroundedVentDeposit.SUPPORT_DEPTH && y - down >= min; down++)
                if (ground(column.getBlock(y - down))) { supported = true; break; }
            return new Column(y, column.getBlock(y), !fluid.isEmpty(), fluid.is(FluidTags.WATER), supported);
        });
    }

    static boolean ground(BlockState state) {
        return VentFeature.terrain(state) || state.is(BlockTags.TERRACOTTA)
                || state.is(Blocks.SANDSTONE) || state.is(Blocks.RED_SANDSTONE)
                || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE);
    }

    private VentSite() {}
}
