package com.ourcreation.moltenventsplus.worldgen;

import com.Apothic0n.MoltenVents.api.biome.features.configurations.MoltenVentConfiguration;
import com.ourcreation.moltenventsplus.compat.ResourceVentBlock;
import com.ourcreation.moltenventsplus.vent.VentBlock;
import com.ourcreation.moltenventsplus.vent.WeatheredVentStoneBlock;
import com.ourcreation.moltenventsplus.vent.WeatheredVentStoneBlock.Profile;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Molten Vents formation by Apothicon02 (MIT; see LICENSES/Molten-Vents-MIT.txt).
 * Adapted from MoltenVentFeature to replay a saved seed and clip every write to the current chunk.
 * Original configured-feature providers and depth ranges remain authoritative.
 */
final class MoltenVentDeposit {
    // The original rim reaches four blocks from its center including decorations; this reaches five.
    static final int SUPERHEATED_RADIUS = 5;
    private final WorldGenLevel level;
    private final BoundingBox bounds;
    private final RandomSource random;
    private final BlockState protectedRock;

    private MoltenVentDeposit(WorldGenLevel level, BoundingBox bounds, RandomSource random, BlockState protectedRock) {
        this.level = level;
        this.bounds = bounds;
        this.random = random;
        this.protectedRock = protectedRock;
    }

    static void place(WorldGenLevel level, BoundingBox bounds, RandomSource random, BlockPos origin,
                      MoltenVentConfiguration config, int depth) {
        place(level, bounds, random, origin, config, depth, 3, null);
    }

    static void placeSuperheated(WorldGenLevel level, BoundingBox bounds, RandomSource random, BlockPos corePos,
                                 BlockState ore, BlockState core, int depth) {
        var outer = new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                .add(ore, 8).add(ore.setValue(WeatheredVentStoneBlock.PROFILE, Profile.CUT), 2));
        var decoration = new WeightedStateProvider(SimpleWeightedRandomList.<BlockState>builder()
                .add(ore.setValue(WeatheredVentStoneBlock.PROFILE, Profile.POST), 3)
                .add(ore.setValue(WeatheredVentStoneBlock.PROFILE, Profile.SLAB), 7));
        // Keep the original silhouette and recessed core. The compact root contains pure stone,
        // rather than supplying free superheated source fluid below a dormant vent.
        var config = new MoltenVentConfiguration(decoration, outer, BlockStateProvider.simple(core),
                BlockStateProvider.simple(ore), ConstantInt.of(depth), false);
        place(level, bounds, random, corePos.above(), config, depth, 4, ore);
    }

    private static void place(WorldGenLevel level, BoundingBox bounds, RandomSource random, BlockPos origin,
                              MoltenVentConfiguration config, int depth, int rim, BlockState protectedRock) {
        var deposit = new MoltenVentDeposit(level, bounds, random, protectedRock);
        var outer = config.getOuterBlock();
        var decoration = config.getDecorativeBlock();
        deposit.partial(origin.above(), outer, rim, 0.8, decoration);
        deposit.diagonal(origin.above(), outer, rim, rim, 0.66, decoration);
        deposit.partial(origin, outer, rim, 0.7, decoration);
        deposit.straight(origin, outer, rim, rim, 1, decoration);
        deposit.diagonal(origin, outer, 0, rim, 1.6, null);
        deposit.straight(origin.below(), outer, rim, rim, 1, null);
        deposit.diagonal(origin.below(), outer, 0, rim, 1.6, null);
        if (depth > 10) {
            for (int y = 1; y <= depth / 2 + 1; y++) {
                deposit.straight(origin.below(y), outer, 0, 2, 0.8, null);
                deposit.diagonal(origin.below(y), outer, 0, 2, 0.75, null);
                deposit.block(origin.below(y), config.getLiquidBlock(), 1, null);
            }
            for (int y = depth / 2 + 2; y <= depth + 1; y++) {
                deposit.straight(origin.below(y), outer, 0, 1, 0.33, null);
                deposit.diagonal(origin.below(y), outer, 0, 1, 0.275, null);
                deposit.block(origin.below(y), config.getLiquidBlock(), 1, null);
            }
        }
        deposit.set(origin, (config.underwater ? Blocks.WATER : Blocks.AIR).defaultBlockState());
        deposit.set(origin.below(), config.getInnerBlock().getState(random, origin.below()));
    }

    private void straight(BlockPos pos, BlockStateProvider block, int min, int max,
                          double chance, BlockStateProvider decoration) {
        for (int radius = min - 1; radius < max; radius++) {
            block(pos.north(radius), block, chance, decoration);
            block(pos.east(radius), block, chance, decoration);
            block(pos.south(radius), block, chance, decoration);
            block(pos.west(radius), block, chance, decoration);
        }
    }

    private void diagonal(BlockPos pos, BlockStateProvider block, int min, int max,
                          double chance, BlockStateProvider decoration) {
        for (int radius = min - 1; radius < max; radius++) {
            partial(pos, block, radius, chance, decoration);
            // The upstream formation intentionally visits these four corners twice.
            for (int pass = 0; pass < 2; pass++) {
                block(pos.north(radius).east(radius), block, chance, decoration);
                block(pos.north(radius).west(radius), block, chance, decoration);
                block(pos.south(radius).west(radius), block, chance, decoration);
                block(pos.south(radius).east(radius), block, chance, decoration);
            }
        }
    }

    private void partial(BlockPos pos, BlockStateProvider block, int radius,
                         double chance, BlockStateProvider decoration) {
        block(pos.north(radius).east(), block, chance, decoration);
        block(pos.north(radius).west(), block, chance, decoration);
        block(pos.south(radius).west(), block, chance, decoration);
        block(pos.south(radius).east(), block, chance, decoration);
        block(pos.north().east(radius), block, chance, decoration);
        block(pos.north().west(radius), block, chance, decoration);
        block(pos.south().west(radius), block, chance, decoration);
        block(pos.south().east(radius), block, chance, decoration);
    }

    private void block(BlockPos pos, BlockStateProvider block, double chance, BlockStateProvider decoration) {
        if (!roll(chance)) return;
        var state = block.getState(random, pos);
        set(pos, state);
        if (decoration != null && roll(chance / 3)) {
            int direction = random.nextInt(15) + 1;
            var offset = direction <= 7 ? pos.above() : switch (direction) {
                case 8 -> pos.north();
                case 9 -> pos.east();
                case 10 -> pos.south();
                case 11 -> pos.west();
                case 12 -> pos.north().east();
                case 13 -> pos.north().west();
                case 14 -> pos.south().east();
                default -> pos.south().west();
            };
            set(offset, decoration.getState(random, offset));
            if (bounds.isInside(offset.below()) && level.getBlockState(offset.below()).isAir()) set(offset.below(), state);
        }
    }

    private boolean roll(double chance) {
        return chance >= 1 || random.nextInt(100) + 1 < chance * 100;
    }

    private void set(BlockPos pos, BlockState state) {
        if (bounds.isInside(pos) && !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) == null) {
            if (protectedRock != null) {
                var previous = level.getBlockState(pos);
                if (previous.getBlock() instanceof VentBlock || previous.getBlock() instanceof ResourceVentBlock) return;
                // Height columns precede biome surfacing, which can replace stone with snow or ice.
                if (!previous.is(protectedRock.getBlock()) && !VentFeature.terrain(previous)
                        && !previous.is(BlockTags.TERRACOTTA) && !previous.is(Blocks.SANDSTONE)
                        && !previous.is(Blocks.RED_SANDSTONE) && !previous.is(Blocks.SNOW_BLOCK)
                        && !previous.is(Blocks.ICE) && !previous.is(Blocks.PACKED_ICE) && !previous.is(Blocks.BLUE_ICE)
                        && !previous.canBeReplaced() && !previous.isAir()) return;
            }
            level.setBlock(pos, state, 2);
        }
    }
}
