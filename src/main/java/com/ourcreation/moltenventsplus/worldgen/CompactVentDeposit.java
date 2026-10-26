package com.ourcreation.moltenventsplus.worldgen;

import com.ourcreation.moltenventsplus.compat.ResourceVentBlock;
import com.ourcreation.moltenventsplus.vent.VentBlock;
import com.ourcreation.moltenventsplus.vent.WeatheredVentStoneBlock;
import com.ourcreation.moltenventsplus.vent.WeatheredVentStoneBlock.Profile;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Solid, stepped rock columns with a single recessed outlet; derived vent design under MIT. */
final class CompactVentDeposit {
    private static final int[] SMALL_LEFT = {-1, -2, -2, -1, 0};
    private static final int[] SMALL_RIGHT = {0, 1, 2, 2, 1};
    private static final int[] LARGE_LEFT = {-2, -4, -4, -5, -4, -4, -3, -3, -1};
    private static final int[] LARGE_RIGHT = {0, 2, 3, 4, 5, 4, 4, 2, 1};

    static void place(WorldGenLevel level, RandomSource random, BlockPos origin, BlockState ore,
                      BlockState core, int radius, int depth, boolean underwater, BoundingBox bounds) {
        int turn = random.nextInt(4);
        boolean mirror = random.nextBoolean();
        boolean large = radius >= MoltenVentDeposit.SUPERHEATED_RADIUS;
        int reach = large ? 4 : radius;
        // Plan complete columns before clipping any writes. Neither chunk order nor existing
        // decorations decide whether a supporting block is placed.
        for (int z = -reach; z <= reach; z++) {
            int row = large ? z + 4 : Math.round((float) z * 2 / radius) + 2;
            int left = large ? LARGE_LEFT[row] : Math.round(SMALL_LEFT[row] * radius / 2F);
            int right = large ? LARGE_RIGHT[row] : Math.round(SMALL_RIGHT[row] * radius / 2F);
            for (int x = left; x <= right; x++) {
                var offset = rotate(mirror ? -x : x, z, turn);
                int distance = Math.max(Math.abs(x), Math.abs(z));
                int height;
                boolean cap;
                if (large) {
                    // Every column fills the entire mantle. Randomness only chips the top edge.
                    height = distance <= 1 ? 2 : distance <= 3 ? 1 : 0;
                    if (z < -1 && distance <= 3) height++;
                    cap = random.nextInt(3) != 0;
                } else {
                    // A short diagonal rock ridge, an uneven shoulder and a low front lip.
                    height = z == -1 && x <= 0 ? 2 : distance <= 1 ? 1 : 0;
                    cap = z < 1 || random.nextBoolean();
                }
                int bottom = -Math.max(1, depth * (radius + 1 - Math.min(radius, distance)) / (radius + 1));
                for (int y = bottom; y <= height; y++) {
                    var pos = origin.offset(offset.getX(), y, offset.getZ());
                    var state = random.nextInt(5) == 0 ? profile(ore, Profile.CUT, false) : ore;
                    if (x == 0 && z == 0 && y >= 0) continue;
                    set(level, bounds, pos, state, ore);
                }
                if (cap && (x != 0 || z != 0)) {
                    var pos = origin.offset(offset.getX(), height + 1, offset.getZ());
                    set(level, bounds, pos, profile(ore, Profile.SLAB, wet(level, pos, underwater)), ore);
                }
            }
        }
        // This opening is intentional: matching source fluid must reach the protected core.
        for (int y = 1; y <= 4; y++) {
            var pos = origin.above(y);
            set(level, bounds, pos, (wet(level, pos, underwater) ? Blocks.WATER : Blocks.AIR).defaultBlockState(), ore);
        }
        set(level, bounds, origin, core, ore);
    }

    private static BlockPos rotate(int x, int z, int turn) {
        return switch (turn) {
            case 1 -> new BlockPos(-z, 0, x);
            case 2 -> new BlockPos(-x, 0, -z);
            case 3 -> new BlockPos(z, 0, -x);
            default -> new BlockPos(x, 0, z);
        };
    }

    private static BlockState profile(BlockState ore, Profile profile, boolean waterlogged) {
        if (ore.getBlock() instanceof WeatheredVentStoneBlock)
            return ore.setValue(WeatheredVentStoneBlock.PROFILE, profile)
                    .setValue(WeatheredVentStoneBlock.WATERLOGGED, profile.partial() && waterlogged);
        return profile == Profile.SLAB ? Blocks.COBBLED_DEEPSLATE_SLAB.defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, waterlogged) : ore;
    }

    private static boolean wet(WorldGenLevel level, BlockPos pos, boolean underwater) {
        return underwater && level.hasChunkAt(pos) && level.getFluidState(pos).is(FluidTags.WATER);
    }

    private static void set(WorldGenLevel level, BoundingBox bounds, BlockPos pos, BlockState state, BlockState ore) {
        if (!bounds.isInside(pos) || level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)
                || level.getBlockEntity(pos) != null) return;
        var previous = level.getBlockState(pos);
        if (previous.getBlock() instanceof VentBlock || previous.getBlock() instanceof ResourceVentBlock) return;
        boolean terrain = VentFeature.terrain(previous) || previous.is(BlockTags.TERRACOTTA)
                || previous.is(Blocks.SANDSTONE) || previous.is(Blocks.RED_SANDSTONE)
                || previous.is(Blocks.SNOW_BLOCK) || previous.is(Blocks.ICE)
                || previous.is(Blocks.PACKED_ICE) || previous.is(Blocks.BLUE_ICE);
        if (terrain || previous.is(ore.getBlock()) || previous.is(Blocks.COBBLED_DEEPSLATE_SLAB)
                || previous.canBeReplaced() || previous.isAir()) level.setBlock(pos, state, 2);
    }

    private CompactVentDeposit() {}
}
