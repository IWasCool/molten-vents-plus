package com.ourcreation.moltenventsplus.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public record VentConfiguration(String tier) implements FeatureConfiguration {
    public static final Codec<VentConfiguration> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(Codec.STRING.fieldOf("tier").forGetter(VentConfiguration::tier)).apply(instance, VentConfiguration::new));
}
