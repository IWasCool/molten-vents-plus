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

/** Seeded small chimneys with complete, bounded foundations; saved older shapes retain their own generators. */
final class GroundedVentDeposit {
    static final int SHAPE_VERSION = 4;
    static final int SUPPORT_DEPTH = 8;
    private record Column(BlockPos offset, int height, Profile cap, int buried, long detail) {}

    static void place(WorldGenLevel level, RandomSource random, BlockPos origin, BlockState ore,
                      BlockState core, int radius, int depth, boolean underwater, BoundingBox bounds) {
        if (anchor(level, origin, ore, core, true) == null || !clearance(level, origin, 4, ore, core, true)) return;
        int variant = random.nextInt(5);
        int turn = random.nextInt(4);
        int finger = random.nextBoolean() ? -1 : 1;
        double stretchX = 0.85 + random.nextDouble() * 0.3;
        double stretchZ = 0.85 + random.nextDouble() * 0.3;
        var columns = new ArrayList<Column>();
        // Choose all geometry before reading a chunk clip or writing terrain.
        for (int z = -radius; z <= radius; z++) for (int x = -radius; x <= radius; x++) {
            double edge = 0.92 + random.nextDouble() * 0.2;
            boolean present = random.nextFloat() < 0.82F;
            long detail = random.nextLong();
            int distance = Math.max(Math.abs(x), Math.abs(z));
            if (distance > 1 && (!present || (x * x / stretchX + z * z / stretchZ) > (radius + 0.2) * (radius + 0.2) * edge)) continue;
            int height = distance > 1 ? ((detail & 3) != 0 ? 0 : -1) : switch (variant) {
                case 0 -> z == -1 ? 1 : distance <= 1 ? 0 : -1;
                case 1 -> x == finger && z == -1 ? 2 : distance <= 1 ? 0 : -1;
                case 2 -> z == -1 && x != 0 ? 1 : distance <= 1 ? 0 : -1;
                case 3 -> distance <= 1 ? 0 : -1;
                default -> z < 0 && distance <= 1 ? 1 : x == finger && z == 0 ? 1 : 0;
            };
            Profile cap = distance <= 1 && (variant == 0 && x == finger && z == -1
                    || variant == 1 && x == finger && z == -1
                    || variant == 2 && z == -1 && x != 0) ? Profile.POST : Profile.SLAB;
            if (x == 0 && z == 0) height = -1;
            var offset = rotate(x, z, turn);
            int buried = Math.max(1, depth * (radius + 1 - Math.min(radius, distance)) / (radius + 1));
            columns.add(new Column(offset, height, cap, buried, detail));
        }
        for (var column : columns) {
            var base = origin.offset(column.offset);
            if (base.getX() < bounds.minX() || base.getX() > bounds.maxX()
                    || base.getZ() < bounds.minZ() || base.getZ() > bounds.maxZ()) continue;
            var support = anchor(level, base, ore, core, base.equals(origin));
            if (support == null || !clearance(level, base, column.height + 1, ore, core, base.equals(origin))) continue;
            int bottom = Math.min(support.getY() + 1, origin.getY() - column.buried);
            // Fill every gap from actual solid ground to the visible rock body.
            // Buried ore only replaces terrain; it does not bridge deeper caves.
            for (int y = bottom; y <= origin.getY() + column.height; y++) {
                var pos = new BlockPos(base.getX(), y, base.getZ());
                boolean foundation = y > support.getY();
                var state = ((column.detail ^ y * 0x9E3779B97F4A7C15L) & 7) == 0
                        ? profile(ore, Profile.CUT, false) : ore;
                set(level, bounds, pos, state, ore, foundation);
            }
            if (!base.equals(origin) && column.height >= 0) {
                var cap = base.above(column.height + 1);
                boolean wet = underwater && level.getFluidState(cap).is(FluidTags.WATER);
                var existing = level.getBlockState(cap);
                boolean buriedCap = (VentSite.ground(existing) || VentFeature.sameStone(existing, ore))
                        && existing.isFaceSturdy(level, cap, Direction.UP);
                set(level, bounds, cap, buriedCap ? ore : profile(ore, column.cap, wet), ore, true);
            }
        }
        for (int y = 1; y <= 4; y++) {
            var pos = origin.above(y);
            var fluid = underwater && level.hasChunkAt(pos) && level.getFluidState(pos).is(FluidTags.WATER)
                    ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
            set(level, bounds, pos, fluid, ore, true);
        }
        set(level, bounds, origin, core, ore, true);
    }

    static BlockPos anchor(WorldGenLevel level, BlockPos base, BlockState ore, BlockState core, boolean center) {
        if (level.isOutsideBuildHeight(base) || !level.hasChunkAt(base)) return null;
        var atBase = level.getBlockState(base);
        if (level.getBlockEntity(base) != null || (atBase.getBlock() instanceof VentBlock) && !(center && atBase.is(core.getBlock()))) return null;
        for (int down = 1; down <= SUPPORT_DEPTH; down++) {
            var pos = base.below(down);
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return null;
            var state = level.getBlockState(pos);
            if (level.getBlockEntity(pos) != null || state.getBlock() instanceof VentBlock) return null;
            boolean rock = VentSite.ground(state) || VentFeature.sameStone(state, ore);
            if (rock && state.isFaceSturdy(level, pos, Direction.UP)) return pos;
            if (!state.isAir() && !state.canBeReplaced() && !rock) return null;
        }
        return null;
    }

    private static void set(WorldGenLevel level, BoundingBox bounds, BlockPos pos, BlockState state,
                            BlockState ore, boolean fillAir) {
        if (!bounds.isInside(pos) || level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)
                || level.getBlockEntity(pos) != null) return;
        var previous = level.getBlockState(pos);
        if (previous.getBlock() instanceof VentBlock) return;
        boolean own = VentFeature.sameStone(previous, ore) || previous.is(Blocks.COBBLED_DEEPSLATE_SLAB)
                || previous.is(Blocks.COBBLED_DEEPSLATE_WALL);
        if (VentSite.ground(previous) || own || fillAir && (previous.isAir() || previous.canBeReplaced()
                || previous.is(BlockTags.LEAVES)))
            level.setBlock(pos, state, 2);
    }

    static boolean clearance(WorldGenLevel level, BlockPos base, int top, BlockState ore,
                                      BlockState core, boolean center) {
        for (int y = 0; y <= top; y++) {
            var pos = base.above(y);
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) return false;
            var state = level.getBlockState(pos);
            if (center && y == 0 && state.is(core.getBlock())) continue;
            if (level.getBlockEntity(pos) != null || state.getBlock() instanceof VentBlock) return false;
            if (!state.isAir() && !state.canBeReplaced() && !state.is(BlockTags.LEAVES) && !VentSite.ground(state)
                    && !VentFeature.sameStone(state, ore) && !state.is(Blocks.COBBLED_DEEPSLATE_SLAB)
                    && !state.is(Blocks.COBBLED_DEEPSLATE_WALL)) return false;
        }
        return true;
    }

    private static BlockState profile(BlockState ore, Profile shape, boolean wet) {
        return VentFeature.profile(ore, shape, wet);
    }

    private static BlockPos rotate(int x, int z, int turn) {
        return switch (turn) {
            case 1 -> new BlockPos(-z, 0, x);
            case 2 -> new BlockPos(-x, 0, -z);
            case 3 -> new BlockPos(z, 0, -x);
            default -> new BlockPos(x, 0, z);
        };
    }

    private GroundedVentDeposit() {}
}
