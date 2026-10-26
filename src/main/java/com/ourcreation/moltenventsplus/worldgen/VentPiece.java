package com.ourcreation.moltenventsplus.worldgen;

import com.Apothic0n.MoltenVents.api.biome.features.configurations.MoltenVentConfiguration;
import com.ourcreation.moltenventsplus.registry.VentRegistry;
import com.ourcreation.moltenventsplus.vent.OreStone;
import com.ourcreation.moltenventsplus.vent.VentTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Replays the saved deposit seed for each intersecting chunk, writing only inside that chunk. */
public final class VentPiece extends StructurePiece {
    private final BlockPos origin;
    private final VentTier tier;
    private final OreStone stone;
    private final boolean aquatic;
    private final boolean submerged;
    private final int radius;
    private final int depth;
    private final long seed;
    private final int shapeVersion;

    public VentPiece(BlockPos origin, VentTier tier, OreStone stone, boolean aquatic, boolean submerged,
                     int radius, int depth, int minY, long seed) {
        super(VentRegistry.VENT_PIECE.get(), 0, new BoundingBox(origin.getX() - radius,
                Math.max(minY, origin.getY() - depth - 2), origin.getZ() - radius,
                origin.getX() + radius, origin.getY() + 4, origin.getZ() + radius));
        this.origin = origin.immutable();
        this.tier = tier;
        this.stone = stone;
        this.aquatic = aquatic;
        this.submerged = submerged;
        this.radius = radius;
        this.depth = depth;
        this.seed = seed;
        this.shapeVersion = tier == VentTier.MOLTEN ? 0 : 3;
    }

    public VentPiece(CompoundTag tag) {
        super(VentRegistry.VENT_PIECE.get(), tag);
        origin = BlockPos.of(tag.getLong("Origin"));
        tier = VentTier.byId(tag.getString("Tier"));
        stone = OreStone.valueOf(tag.getString("Stone"));
        aquatic = tag.getBoolean("Aquatic");
        submerged = tag.getBoolean("Submerged");
        radius = tag.getInt("Radius");
        depth = tag.getInt("Depth");
        seed = tag.getLong("Seed");
        shapeVersion = tag.getInt("ShapeVersion");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong("Origin", origin.asLong());
        tag.putString("Tier", tier.id);
        tag.putString("Stone", stone.name());
        tag.putBoolean("Aquatic", aquatic);
        tag.putBoolean("Submerged", submerged);
        tag.putInt("Radius", radius);
        tag.putInt("Depth", depth);
        tag.putLong("Seed", seed);
        if (shapeVersion > 0) tag.putInt("ShapeVersion", shapeVersion);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator,
                            RandomSource unused, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        var random = RandomSource.create(seed);
        if (tier == VentTier.MOLTEN) {
            var configured = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .get(VentStructure.moltenFeature(stone, aquatic));
            if (configured != null && configured.config() instanceof MoltenVentConfiguration config) {
                MoltenVentDeposit.place(level, chunkBox, random, origin.above(), config, depth);
            }
        } else {
            VentFeature.placeDeposit(level, random, origin, tier, stone.output(tier).defaultBlockState(),
                    VentRegistry.block(tier, stone, false).defaultBlockState(), radius, depth, submerged, chunkBox, shapeVersion);
        }
    }
}
