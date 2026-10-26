package com.ourcreation.moltenventsplus.worldgen;

import com.Apothic0n.MoltenVents.api.biome.features.configurations.MoltenVentConfiguration;
import com.Apothic0n.MoltenVents.config.CommonConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ourcreation.moltenventsplus.VentConfig;
import com.ourcreation.moltenventsplus.registry.VentRegistry;
import com.ourcreation.moltenventsplus.vent.OreStone;
import com.ourcreation.moltenventsplus.vent.VentTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.ArrayList;
import java.util.Optional;

public final class VentStructure extends Structure {
    private static final Codec<VentTier> TIERS = Codec.STRING.xmap(VentTier::byId, tier -> tier.id);
    private static final Codec<OreStone> STONES = StringRepresentable.fromEnum(OreStone::values);
    public static final MapCodec<VentStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            TIERS.fieldOf("tier").forGetter(vent -> vent.tier),
            STONES.fieldOf("stone").forGetter(vent -> vent.stone),
            Codec.BOOL.optionalFieldOf("aquatic", false).forGetter(vent -> vent.aquatic)
    ).apply(instance, VentStructure::new));

    private final VentTier tier;
    private final OreStone stone;
    private final boolean aquatic;

    public VentStructure(StructureSettings settings, VentTier tier, OreStone stone, boolean aquatic) {
        super(settings);
        this.tier = tier;
        this.stone = stone;
        this.aquatic = aquatic;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (tier == VentTier.WATER && !VentConfig.WATER_WORLDGEN.get()
                || tier == VentTier.SUPERHEATED && !VentConfig.MEGA_WORLDGEN.get()
                || tier == VentTier.MOLTEN && !(aquatic
                    ? CommonConfig.generateUnderwaterVents.get() : CommonConfig.generateLandVents.get())) {
            return Optional.empty();
        }

        if (!VentPlacement.selects(context, tier, stone.id)) return Optional.empty();

        // Land and aquatic variants share an origin, so a shoreline cannot generate both.
        var random = new WorldgenRandom(new LegacyRandomSource(0));
        random.setLargeFeatureWithSalt(context.seed(), context.chunkPos().x, context.chunkPos().z,
                (tier.id + ":" + stone.id).hashCode() & Integer.MAX_VALUE);
        int x = context.chunkPos().getMinBlockX() + random.nextInt(16);
        int z = context.chunkPos().getMinBlockZ() + random.nextInt(16);
        var heightmap = tier == VentTier.MOLTEN && !aquatic
                ? Heightmap.Types.WORLD_SURFACE_WG : Heightmap.Types.OCEAN_FLOOR_WG;
        int y = context.chunkGenerator().getFirstFreeHeight(x, z, heightmap, context.heightAccessor(), context.randomState()) - 1;
        if (y < context.heightAccessor().getMinBuildHeight() + 8) return Optional.empty();
        var column = context.chunkGenerator().getBaseColumn(x, z, context.heightAccessor(), context.randomState());
        var ground = column.getBlock(y);
        boolean submerged = !column.getBlock(y + 1).getFluidState().isEmpty();

        int radius;
        int depth;
        if (tier == VentTier.MOLTEN) {
            if (!ground.isSolid() || aquatic && !column.getBlock(y + 3).is(Blocks.WATER)
                    || !aquatic && ground.is(Blocks.WATER)) return Optional.empty();
            var configured = context.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(moltenFeature(stone, aquatic));
            if (configured == null || !(configured.config() instanceof MoltenVentConfiguration config)) return Optional.empty();
            depth = config.getDepth().sample(random);
            if (aquatic) depth = Math.min(80, depth);
            radius = 5;
        } else {
            if (!VentFeature.terrain(ground) || tier == VentTier.SUPERHEATED && submerged) return Optional.empty();
            radius = tier == VentTier.WATER ? 2 : MoltenVentDeposit.SUPERHEATED_RADIUS;
            depth = tier == VentTier.WATER ? 4 + random.nextInt(3) : 12 + random.nextInt(5);
        }

        var origin = new BlockPos(x, y, z);
        var pieces = new ArrayList<VentPiece>();
        for (var offset : VentPlacement.clusterOffsets(context.seed(), context.chunkPos(), tier.id, radius)) {
            int px = x + offset.getX(), pz = z + offset.getZ();
            int py = context.chunkGenerator().getFirstFreeHeight(px, pz, heightmap,
                    context.heightAccessor(), context.randomState()) - 1;
            if (Math.abs(py - y) > 2 || py < context.heightAccessor().getMinBuildHeight() + 8) continue;
            var base = context.chunkGenerator().getBaseColumn(px, pz, context.heightAccessor(), context.randomState());
            boolean wet = !base.getBlock(py + 1).getFluidState().isEmpty();
            if (tier == VentTier.MOLTEN) {
                if (!base.getBlock(py).isSolid() || aquatic && !base.getBlock(py + 3).is(Blocks.WATER)
                        || !aquatic && wet) continue;
            } else if (!VentFeature.terrain(base.getBlock(py)) || tier == VentTier.SUPERHEATED && wet) continue;
            var biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(px), QuartPos.fromBlock(py),
                    QuartPos.fromBlock(pz), context.randomState().sampler());
            if (!context.validBiome().test(biome)) continue;
            pieces.add(new VentPiece(new BlockPos(px, py, pz), tier, stone, aquatic, wet, radius, depth,
                    context.heightAccessor().getMinBuildHeight(), random.nextLong()));
        }
        if (pieces.isEmpty()) return Optional.empty();
        return Optional.of(new GenerationStub(origin, builder -> pieces.forEach(builder::addPiece)));
    }

    static ResourceLocation moltenFeature(OreStone stone, boolean aquatic) {
        return ResourceLocation.fromNamespaceAndPath("molten_vents", (aquatic ? "aquatic_" : "") + stone.id + "_vent");
    }

    @Override
    public StructureType<?> type() {
        return VentRegistry.VENT_STRUCTURE.get();
    }
}
