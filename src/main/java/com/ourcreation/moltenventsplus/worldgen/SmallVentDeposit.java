package com.ourcreation.moltenventsplus.worldgen;

import com.ourcreation.moltenventsplus.compat.ResourceVentBlock;
import com.ourcreation.moltenventsplus.vent.VentBlock;
import com.ourcreation.moltenventsplus.vent.WeatheredVentStoneBlock;
import com.ourcreation.moltenventsplus.vent.WeatheredVentStoneBlock.Profile;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Miniature broken-rim chimneys inspired by Apothicon02's Molten Vents (MIT). */
final class SmallVentDeposit {
    static void place(WorldGenLevel level, RandomSource random, BlockPos origin, BlockState ore,
                      BlockState core, int radius, int depth, boolean underwater, BoundingBox bounds) {
        // Make every random choice before clipping a write, so chunk order cannot change the shape.
        double stretchX = 0.93 + random.nextDouble() * 0.14;
        double stretchZ = 0.93 + random.nextDouble() * 0.14;
        int turn = random.nextInt(4);
        for (int y = -depth; y <= 0; y++) {
            double taper = 0.38 + 0.62 * (1.0 + (double) y / depth);
            double r = Math.max(0.8, (radius + 0.18) * taper);
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                double edge = 0.94 + random.nextDouble() * 0.18;
                if ((x * x / stretchX + z * z / stretchZ) / (r * r) > edge) continue;
                var pos = origin.offset(x, y, z);
                var rock = random.nextInt(5) == 0 ? profile(ore, Profile.CUT, false) : ore;
                set(level, bounds, pos, rock, ore, false);
            }
        }

        // A low, rounded apron and an asymmetric three-block-wide broken chimney.
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            int distance = x * x + z * z;
            boolean shoulder = distance >= 4 && distance <= radius * radius + 1;
            boolean present = random.nextFloat() < 0.64F;
            if (shoulder && present)
                set(level, bounds, origin.offset(x, 1, z), profile(ore, Profile.SLAB,
                        wet(level, origin.offset(x, 1, z), underwater)), ore, true);
        }
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && z == 0) continue;
            var offset = rotate(x, z, turn);
            boolean low = z == 1 || x != 0 && z == 0;
            var shape = low ? Profile.SLAB : random.nextInt(4) == 0 ? Profile.CUT : Profile.NATURAL;
            var pos = origin.offset(offset.getX(), 1, offset.getZ());
            set(level, bounds, pos, profile(ore, shape, wet(level, pos, underwater)), ore, true);
        }
        // A ragged rear crest, with one narrow stone finger and two chipped caps.
        int finger = random.nextBoolean() ? -1 : 1;
        for (int x = -1; x <= 1; x++) {
            var offset = rotate(x, -1, turn);
            var pos = origin.offset(offset.getX(), 2, offset.getZ());
            set(level, bounds, pos, profile(ore, x == finger ? Profile.POST : Profile.SLAB,
                    wet(level, pos, underwater)), ore, true);
        }
        for (int y = 1; y <= 3; y++) {
            var pos = origin.above(y);
            var opening = wet(level, pos, underwater) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
            set(level, bounds, pos, opening, ore, true);
        }
        set(level, bounds, origin, core, ore, true);
    }

    private static BlockPos rotate(int x, int z, int turns) {
        return switch (turns) {
            case 1 -> new BlockPos(-z, 0, x);
            case 2 -> new BlockPos(-x, 0, -z);
            case 3 -> new BlockPos(z, 0, -x);
            default -> new BlockPos(x, 0, z);
        };
    }

    private static boolean wet(WorldGenLevel level, BlockPos pos, boolean underwater) {
        return underwater && level.hasChunkAt(pos) && level.getFluidState(pos).is(FluidTags.WATER);
    }

    private static BlockState profile(BlockState ore, Profile profile, boolean waterlogged) {
        if (ore.getBlock() instanceof WeatheredVentStoneBlock)
            return ore.setValue(WeatheredVentStoneBlock.PROFILE, profile)
                    .setValue(WeatheredVentStoneBlock.WATERLOGGED, profile.partial() && waterlogged);
        // Oil has a barren deepslate chimney rather than an ore-bearing deposit.
        return switch (profile) {
            case SLAB -> Blocks.COBBLED_DEEPSLATE_SLAB.defaultBlockState()
                    .setValue(BlockStateProperties.WATERLOGGED, waterlogged);
            case POST -> Blocks.COBBLED_DEEPSLATE_WALL.defaultBlockState()
                    .setValue(BlockStateProperties.UP, true).setValue(BlockStateProperties.WATERLOGGED, waterlogged);
            default -> ore;
        };
    }

    private static void set(WorldGenLevel level, BoundingBox bounds, BlockPos pos, BlockState state,
                            BlockState ore, boolean aboveGround) {
        if (!bounds.isInside(pos) || level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)
                || level.getBlockEntity(pos) != null) return;
        var previous = level.getBlockState(pos);
        if (previous.getBlock() instanceof VentBlock || previous.getBlock() instanceof ResourceVentBlock) return;
        boolean ownRock = previous.is(ore.getBlock()) || previous.is(Blocks.COBBLED_DEEPSLATE_SLAB)
                || previous.is(Blocks.COBBLED_DEEPSLATE_WALL);
        // The height column is sampled before biome surfacing; badlands replace that stone with terracotta.
        boolean terrain = VentFeature.terrain(previous) || previous.is(BlockTags.TERRACOTTA)
                || previous.is(Blocks.SANDSTONE) || previous.is(Blocks.RED_SANDSTONE);
        if (terrain || ownRock || aboveGround && (previous.canBeReplaced() || previous.isAir()))
            level.setBlock(pos, state, 2);
    }

    private SmallVentDeposit() {}
}
