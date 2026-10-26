package com.ourcreation.moltenventsplus.worldgen;

import com.ourcreation.moltenventsplus.compat.VentResource;
import com.ourcreation.moltenventsplus.registry.ResourceVentRegistry;
import com.ourcreation.moltenventsplus.vent.VentTier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Original piece IDs/NBT remain unchanged; optional resources have their own saved piece type. */
public final class ResourceVentPiece extends StructurePiece {
    private final BlockPos origin;
    private final VentResource resource;
    private final VentTier tier;
    private final boolean submerged;
    private final int radius;
    private final int depth;
    private final long seed;
    private final int shapeVersion;

    public ResourceVentPiece(BlockPos origin, VentResource resource, VentTier tier, boolean submerged,
                             int radius, int depth, int minY, long seed) {
        super(ResourceVentRegistry.PIECE.get(), 0, new BoundingBox(origin.getX() - radius,
                Math.max(minY, origin.getY() - depth - 2), origin.getZ() - radius,
                origin.getX() + radius, origin.getY() + 4, origin.getZ() + radius));
        this.origin = origin.immutable();
        this.resource = resource;
        this.tier = tier;
        this.submerged = submerged;
        this.radius = radius;
        this.depth = depth;
        this.seed = seed;
        this.shapeVersion = 3;
    }

    public ResourceVentPiece(CompoundTag tag) {
        super(ResourceVentRegistry.PIECE.get(), tag);
        origin = BlockPos.of(tag.getLong("Origin"));
        resource = VentResource.valueOf(tag.getString("Resource"));
        tier = VentTier.byId(tag.getString("Tier"));
        submerged = tag.getBoolean("Submerged");
        radius = tag.getInt("Radius");
        depth = tag.getInt("Depth");
        seed = tag.getLong("Seed");
        shapeVersion = tag.getInt("ShapeVersion");
    }

    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong("Origin", origin.asLong());
        tag.putString("Resource", resource.name());
        tag.putString("Tier", tier.id);
        tag.putBoolean("Submerged", submerged);
        tag.putInt("Radius", radius);
        tag.putInt("Depth", depth);
        tag.putLong("Seed", seed);
        if (shapeVersion > 0) tag.putInt("ShapeVersion", shapeVersion);
    }

    @Override public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator,
                                      RandomSource unused, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        var output = resource == VentResource.OIL ? java.util.Optional.of(Blocks.DEEPSLATE) : resource.output(tier);
        if (output.isEmpty()) return;
        VentFeature.placeDeposit(level, RandomSource.create(seed), origin, resource == VentResource.OIL ? VentTier.WATER : tier,
                output.get().defaultBlockState(), ResourceVentRegistry.block(resource, tier, false).defaultBlockState(),
                radius, depth, submerged, chunkBox, shapeVersion);
    }
}
