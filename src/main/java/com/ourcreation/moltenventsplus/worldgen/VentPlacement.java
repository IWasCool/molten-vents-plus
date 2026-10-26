package com.ourcreation.moltenventsplus.worldgen;

import com.ourcreation.moltenventsplus.vent.OreStone;
import com.ourcreation.moltenventsplus.vent.VentTier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;

/** All materials in a tier share one placement grid and one mineral choice. */
public final class VentPlacement {
    public static boolean selects(Structure.GenerationContext context, VentTier tier, String material) {
        var random = new WorldgenRandom(new LegacyRandomSource(0));
        random.setLargeFeatureWithSalt(context.seed(), context.chunkPos().x, context.chunkPos().z,
                0x4D565000 + tier.ordinal());
        return material.equals(OreStone.values()[random.nextInt(OreStone.values().length)].id);
    }

    /** One shared roll per region candidate: 95% single, 4.9% pair, 0.1% triple. */
    public static List<BlockPos> clusterOffsets(long seed, ChunkPos chunk, String family, int radius) {
        var random = new WorldgenRandom(new LegacyRandomSource(0));
        random.setLargeFeatureWithSalt(seed, chunk.x, chunk.z, 0x4D564300 ^ family.hashCode());
        int roll = random.nextInt(1000);
        int count = roll == 0 ? 3 : roll < 50 ? 2 : 1;
        int distance = radius * 2 + 1;
        int turn = random.nextInt(4);
        var next = switch (turn) {
            case 1 -> new BlockPos(0, 0, distance);
            case 2 -> new BlockPos(-distance, 0, 0);
            case 3 -> new BlockPos(0, 0, -distance);
            default -> new BlockPos(distance, 0, 0);
        };
        if (count == 1) return List.of(BlockPos.ZERO);
        if (count == 2) return List.of(BlockPos.ZERO, next);
        return List.of(BlockPos.ZERO, next, new BlockPos(-next.getZ(), 0, next.getX()));
    }

    private VentPlacement() {}
}
