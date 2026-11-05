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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
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
        int radius = tier == VentTier.WATER ? 2 : MoltenVentDeposit.SUPERHEATED_RADIUS;
        var site = VentSite.find(context, x, z, radius, tier, aquatic, true);
        if (site.isEmpty()) return Optional.empty();
        var origin = site.get().origin();
        x = origin.getX();
        z = origin.getZ();
        int y = origin.getY();
        int depth;
        if (tier == VentTier.MOLTEN) {
            var configured = context.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(moltenFeature(stone, aquatic));
            if (configured == null || !(configured.config() instanceof MoltenVentConfiguration config)) return Optional.empty();
            depth = config.getDepth().sample(random);
            if (aquatic) depth = Math.min(80, depth);
        } else {
            depth = tier == VentTier.WATER ? WaterVentDeposit.MIN_DEPTH + random.nextInt(7) : 12 + random.nextInt(5);
        }
        var pieces = new ArrayList<VentPiece>();
        for (var offset : VentPlacement.clusterOffsets(context.seed(), context.chunkPos(), tier.id, radius)) {
            int px = x + offset.getX(), pz = z + offset.getZ();
            var companion = offset.equals(BlockPos.ZERO) ? site : VentSite.find(context, px, pz, radius, tier, aquatic, false);
            if (companion.isEmpty() || Math.abs(companion.get().origin().getY() - y) > 2) continue;
            pieces.add(new VentPiece(companion.get().origin(), tier, stone, aquatic, companion.get().submerged(), radius, depth,
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
