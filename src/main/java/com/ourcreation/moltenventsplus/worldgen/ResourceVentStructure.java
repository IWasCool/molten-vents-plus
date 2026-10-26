package com.ourcreation.moltenventsplus.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ourcreation.moltenventsplus.VentConfig;
import com.ourcreation.moltenventsplus.compat.VentResource;
import com.ourcreation.moltenventsplus.registry.ResourceVentRegistry;
import com.ourcreation.moltenventsplus.vent.VentTier;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

public final class ResourceVentStructure extends Structure {
    public static final MapCodec<ResourceVentStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            StringRepresentable.fromEnum(VentResource::values).fieldOf("resource").forGetter(vent -> vent.resource),
            Codec.STRING.xmap(VentTier::byId, tier -> tier.id).optionalFieldOf("tier", VentTier.WATER).forGetter(vent -> vent.tier)
    ).apply(instance, ResourceVentStructure::new));

    private final VentResource resource;
    private final VentTier tier;

    public ResourceVentStructure(StructureSettings settings, VentResource resource, VentTier tier) {
        super(settings);
        this.resource = resource;
        this.tier = tier;
    }

    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!resource.available()) return Optional.empty();
        if (resource == VentResource.OIL) {
            if (!VentConfig.OIL_WORLDGEN.get()) return Optional.empty();
        } else if (!VentConfig.INTEGRATION_WORLDGEN.get()
                || tier == VentTier.WATER && !VentConfig.WATER_WORLDGEN.get()
                || tier == VentTier.SUPERHEATED && !VentConfig.MEGA_WORLDGEN.get()
                || tier == VentTier.MOLTEN && !com.Apothic0n.MoltenVents.config.CommonConfig.generateLandVents.get()) {
            return Optional.empty();
        }
        if (resource != VentResource.OIL && !VentPlacement.selects(context, tier, resource.id))
            return Optional.empty();

        var random = new WorldgenRandom(new LegacyRandomSource(0));
        random.setLargeFeatureWithSalt(context.seed(), context.chunkPos().x, context.chunkPos().z,
                (resource.id + ":" + tier.id).hashCode() & Integer.MAX_VALUE);
        int x = context.chunkPos().getMinBlockX() + random.nextInt(16);
        int z = context.chunkPos().getMinBlockZ() + random.nextInt(16);
        int y = context.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                context.heightAccessor(), context.randomState()) - 1;
        if (y < context.heightAccessor().getMinBuildHeight() + 8) return Optional.empty();
        var column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
        if (!VentFeature.terrain(column.getBlock(y))) return Optional.empty();
        boolean submerged = !column.getBlock(y + 1).getFluidState().isEmpty();
        if (resource != VentResource.OIL && tier != VentTier.WATER && submerged) return Optional.empty();
        int radius = resource == VentResource.OIL ? 3 : tier == VentTier.WATER ? 2
                : tier == VentTier.MOLTEN ? 5 : MoltenVentDeposit.SUPERHEATED_RADIUS;
        int depth = resource == VentResource.OIL ? 6 : tier == VentTier.WATER ? 4 + random.nextInt(3)
                : tier == VentTier.MOLTEN ? 10 + random.nextInt(5) : 12 + random.nextInt(5);
        var origin = new BlockPos(x, y, z);
        var piece = new ResourceVentPiece(origin, resource, tier, submerged, radius, depth,
                context.heightAccessor().getMinBuildHeight(), random.nextLong());
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(piece)));
    }

    @Override public StructureType<?> type() { return ResourceVentRegistry.STRUCTURE.get(); }
}
