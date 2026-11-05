package com.ourcreation.moltenventsplus.worldgen;

import com.ourcreation.moltenventsplus.vent.VentBlock;
import com.ourcreation.moltenventsplus.vent.WeatheredVentStoneBlock.Profile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;

/** Two asymmetric small outcrops, with native stone profiles and deep, tapered roots. */
final class WaterVentDeposit {
    static final int SHAPE_VERSION = 5;
    static final int MIN_DEPTH = 12;
    static final int MAX_DEPTH = 18;

    private record Rock(int x, int z, int height, Profile body, Profile cap) {}
    private record Column(BlockPos offset, Rock rock, int depth, long detail) {}

    // Surface arrangements relative to the exposed core. Foundations follow this
    // irregular footprint, rather than adding a square under the whole formation.
    private static final Rock[][] VARIANTS = {
            {
                    slab(-2, 0), slab(-1, -1), slab(-1, 0), slab(0, -1),
                    slab(0, 1), slab(1, 0), slab(1, 1), slab(2, 1),
                    new Rock(-1, 1, 1, Profile.NATURAL, Profile.POST)
            },
            {
                    slab(-1, -1), slab(-1, 0), slab(0, -1), slab(0, 1), slab(0, 2),
                    slab(1, -2), slab(1, -1), slab(1, 0), slab(1, 1), slab(2, -1), slab(2, 0),
                    new Rock(-1, 1, 1, Profile.NATURAL, Profile.POST),
                    new Rock(0, -2, 1, Profile.CUT, Profile.POST)
            }
    };

    static void place(WorldGenLevel level, RandomSource random, BlockPos origin, BlockState ore,
                      BlockState core, int depth, boolean underwater, BoundingBox bounds) {
        if (GroundedVentDeposit.anchor(level, origin, ore, core, true) == null
                || !GroundedVentDeposit.clearance(level, origin, 4, ore, core, true)) return;
        var layout = VARIANTS[random.nextInt(VARIANTS.length)];
        int turn = random.nextInt(4);
        boolean mirror = random.nextBoolean();
        depth = Math.clamp(depth, MIN_DEPTH, MAX_DEPTH);
        var columns = new ArrayList<Column>();
        columns.add(new Column(BlockPos.ZERO, null, depth, random.nextLong()));
        // Finish all seed-dependent choices before any chunk clipping or terrain reads.
        for (var rock : layout) {
            long detail = random.nextLong();
            int buried = Math.max(7, depth - 2 * (Math.abs(rock.x) + Math.abs(rock.z)) - (int) (detail & 1));
            columns.add(new Column(rotate(mirror ? -rock.x : rock.x, rock.z, turn), rock, buried, detail));
        }
        for (var column : columns) {
            var base = origin.offset(column.offset);
            if (base.getX() < bounds.minX() || base.getX() > bounds.maxX()
                    || base.getZ() < bounds.minZ() || base.getZ() > bounds.maxZ()) continue;
            boolean center = column.rock == null;
            int height = center ? -1 : column.rock.height;
            var support = GroundedVentDeposit.anchor(level, base, ore, core, center);
            if (support == null || !GroundedVentDeposit.clearance(level, base, height + 1, ore, core, center)) continue;
            int bottom = Math.min(support.getY() + 1, origin.getY() - column.depth);
            for (int y = bottom; y <= origin.getY() + height; y++) {
                var pos = new BlockPos(base.getX(), y, base.getZ());
                var profile = !center && y > origin.getY() ? column.rock.body
                        : ((column.detail ^ y * 0x9E3779B97F4A7C15L) & 7) == 0 ? Profile.CUT : Profile.NATURAL;
                // Fill short gaps above solid support; do not bridge caves below it.
                set(level, bounds, pos, VentFeature.profile(ore, profile, false), ore, y > support.getY());
            }
            if (!center) {
                var cap = base.above(height + 1);
                var previous = level.getBlockState(cap);
                boolean buried = (VentSite.ground(previous) || VentFeature.sameStone(previous, ore))
                        && previous.isFaceSturdy(level, cap, Direction.UP);
                boolean wet = underwater && level.getFluidState(cap).is(FluidTags.WATER);
                set(level, bounds, cap, buried ? ore : VentFeature.profile(ore, column.rock.cap, wet), ore, true);
            }
        }
        for (int y = 1; y <= 4; y++) {
            var pos = origin.above(y);
            boolean wet = underwater && level.hasChunkAt(pos) && level.getFluidState(pos).is(FluidTags.WATER);
            set(level, bounds, pos, (wet ? Blocks.WATER : Blocks.AIR).defaultBlockState(), ore, true);
        }
        set(level, bounds, origin, core, ore, true);
    }

    private static Rock slab(int x, int z) {
        return new Rock(x, z, 0, Profile.NATURAL, Profile.SLAB);
    }

    private static BlockPos rotate(int x, int z, int turn) {
        return switch (turn) {
            case 1 -> new BlockPos(-z, 0, x);
            case 2 -> new BlockPos(-x, 0, -z);
            case 3 -> new BlockPos(z, 0, -x);
            default -> new BlockPos(x, 0, z);
        };
    }

    private static void set(WorldGenLevel level, BoundingBox bounds, BlockPos pos, BlockState state,
                            BlockState ore, boolean fillAir) {
        if (!bounds.isInside(pos) || level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)
                || level.getBlockEntity(pos) != null) return;
        var previous = level.getBlockState(pos);
        if (previous.getBlock() instanceof VentBlock) return;
        if (VentSite.ground(previous) || VentFeature.sameStone(previous, ore)
                || fillAir && (previous.isAir() || previous.canBeReplaced() || previous.is(BlockTags.LEAVES)))
            level.setBlock(pos, state, 2);
    }

    private WaterVentDeposit() {}
}
