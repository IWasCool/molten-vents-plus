package com.ourcreation.moltenventsplus.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ourcreation.moltenventsplus.compat.VentResource;
import com.ourcreation.moltenventsplus.registry.ResourceVentRegistry;
import com.ourcreation.moltenventsplus.vent.VentTier;
import net.minecraft.util.StringRepresentable;
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
        // Keep saved registry/codec identity without generating any new resource starts.
        return Optional.empty();
    }

    @Override public StructureType<?> type() { return ResourceVentRegistry.STRUCTURE.get(); }
}
