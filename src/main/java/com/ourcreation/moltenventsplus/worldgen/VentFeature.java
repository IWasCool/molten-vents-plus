package com.ourcreation.moltenventsplus.worldgen;

import com.ourcreation.moltenventsplus.VentConfig;
import com.ourcreation.moltenventsplus.registry.VentRegistry;
import com.ourcreation.moltenventsplus.vent.*;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

public final class VentFeature extends Feature<VentConfiguration> {
    public VentFeature() { super(VentConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<VentConfiguration> context) {
        VentTier tier = VentTier.byId(context.config().tier());
        if (tier == VentTier.MOLTEN || tier == VentTier.WATER && !VentConfig.WATER_WORLDGEN.get()
                || tier == VentTier.SUPERHEATED && !VentConfig.MEGA_WORLDGEN.get()) return false;
        var level = context.level();
        var random = context.random();
        var origin = context.origin().below();
        if (!terrain(level, origin) || origin.getY() < level.getMinBuildHeight()+8) return false;
        if (tier == VentTier.SUPERHEATED && !level.getFluidState(origin.above()).isEmpty()) return false;
        OreStone stone = OreStone.values()[random.nextInt(OreStone.values().length)];
        int radius = tier == VentTier.WATER ? 2 : MoltenVentDeposit.SUPERHEATED_RADIUS;
        int depth = tier == VentTier.WATER ? 4+random.nextInt(3) : 12+random.nextInt(5);
        boolean underwater = !level.getFluidState(origin.above()).isEmpty();
        placeDeposit(level, random, origin, tier, stone, radius, depth, underwater, BoundingBox.infinite());
        return true;
    }

    static void placeDeposit(WorldGenLevel level, RandomSource random, BlockPos origin, VentTier tier,
                             OreStone stone, int radius, int depth, boolean underwater, BoundingBox bounds) {
        var ore = stone.output(tier).defaultBlockState();
        placeDeposit(level, random, origin, tier, ore, VentRegistry.block(tier, stone, false).defaultBlockState(),
                radius, depth, underwater, bounds);
    }

    static void placeDeposit(WorldGenLevel level, RandomSource random, BlockPos origin, VentTier tier,
                             BlockState ore, BlockState core, int radius, int depth, boolean underwater, BoundingBox bounds) {
        placeDeposit(level, random, origin, tier, ore, core, radius, depth, underwater, bounds,
                3);
    }

    static void placeDeposit(WorldGenLevel level, RandomSource random, BlockPos origin, VentTier tier,
                             BlockState ore, BlockState core, int radius, int depth, boolean underwater,
                             BoundingBox bounds, int shapeVersion) {
        if (shapeVersion >= 3) {
            CompactVentDeposit.place(level, random, origin, ore, core, radius, depth, underwater, bounds);
            return;
        }
        if (tier == VentTier.SUPERHEATED && shapeVersion >= 2) {
            MoltenVentDeposit.placeSuperheated(level, bounds, random, origin, ore, core, depth);
            return;
        }
        if (tier == VentTier.WATER && shapeVersion >= 1) {
            SmallVentDeposit.place(level, random, origin, ore, core, radius, depth, underwater, bounds);
            return;
        }
        for (int y = -depth; y <= 2; y++) {
            double taper = y < -2 ? Math.max(0.15, 1.0+(double)y/(depth+2)) : 1;
            double r = Math.max(1, radius*taper);
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                double distance = (x*x+z*z)/(r*r);
                double edge = 1.1+random.nextDouble()*0.15;
                boolean rim = random.nextFloat() < 0.65F;
                if (distance > edge) continue;
                var pos = origin.offset(x,y,z);
                if (!bounds.isInside(pos) || pos.getY() <= level.getMinBuildHeight() || !level.hasChunkAt(pos)
                        || level.getBlockEntity(pos) != null) continue;
                if (y >= 0 && Math.abs(x) <= (tier == VentTier.WATER ? 0 : 2)
                        && Math.abs(z) <= (tier == VentTier.WATER ? 0 : 2)) {
                    if (!underwater) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                } else if (terrain(level,pos) || y > 0 && level.getBlockState(pos).isAir()) {
                    if (y <= 0 || distance > 0.6 && rim)
                        level.setBlock(pos,ore,2);
                }
            }
        }
        if (bounds.isInside(origin) && level.getBlockEntity(origin) == null)
            level.setBlock(origin, core, 2);
    }

    private static boolean terrain(WorldGenLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return level.getBlockEntity(pos) == null && terrain(state);
    }

    static boolean terrain(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY) || state.is(Blocks.CALCITE) || state.is(Blocks.TUFF);
    }
}
