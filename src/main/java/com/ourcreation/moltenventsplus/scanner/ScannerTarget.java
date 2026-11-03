package com.ourcreation.moltenventsplus.scanner;

import com.ourcreation.moltenventsplus.registry.VentRegistry;
import com.ourcreation.moltenventsplus.vent.OreStone;
import com.ourcreation.moltenventsplus.vent.VentTier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** One whitelisted material in one vent tier, including its active and dormant cores. */
public enum ScannerTarget {
    WATER_ASURINE(VentTier.WATER, OreStone.ASURINE),
    WATER_VERIDIUM(VentTier.WATER, OreStone.VERIDIUM),
    WATER_CRIMSITE(VentTier.WATER, OreStone.CRIMSITE),
    WATER_OCHRUM(VentTier.WATER, OreStone.OCHRUM),
    WATER_SCORCHIA(VentTier.WATER, OreStone.SCORCHIA),
    WATER_SCORIA(VentTier.WATER, OreStone.SCORIA),
    MOLTEN_ASURINE(VentTier.MOLTEN, OreStone.ASURINE),
    MOLTEN_VERIDIUM(VentTier.MOLTEN, OreStone.VERIDIUM),
    MOLTEN_CRIMSITE(VentTier.MOLTEN, OreStone.CRIMSITE),
    MOLTEN_OCHRUM(VentTier.MOLTEN, OreStone.OCHRUM),
    MOLTEN_SCORCHIA(VentTier.MOLTEN, OreStone.SCORCHIA),
    MOLTEN_SCORIA(VentTier.MOLTEN, OreStone.SCORIA),
    SUPERHEATED_ASURINE(VentTier.SUPERHEATED, OreStone.ASURINE),
    SUPERHEATED_VERIDIUM(VentTier.SUPERHEATED, OreStone.VERIDIUM),
    SUPERHEATED_CRIMSITE(VentTier.SUPERHEATED, OreStone.CRIMSITE),
    SUPERHEATED_OCHRUM(VentTier.SUPERHEATED, OreStone.OCHRUM),
    SUPERHEATED_SCORCHIA(VentTier.SUPERHEATED, OreStone.SCORCHIA),
    SUPERHEATED_SCORIA(VentTier.SUPERHEATED, OreStone.SCORIA);

    private final VentTier tier;
    private final OreStone stone;
    private final List<ResourceKey<Structure>> structures;
    private final Set<ResourceLocation> cores;

    ScannerTarget(VentTier tier, OreStone stone) {
        this.tier = tier;
        this.stone = stone;
        structures = tier == VentTier.MOLTEN
                ? List.of(structure("molten_vents:" + stone.id + "_vent"),
                          structure("molten_vents:aquatic_" + stone.id + "_vent"))
                : List.of(structure("create_water_vents:" + tier.id + "_" + stone.id + "_vent"));
        cores = Set.of(VentRegistry.ventId(tier, stone, false), VentRegistry.ventId(tier, stone, true));
    }

    private static ResourceKey<Structure> structure(String id) {
        return ResourceKey.create(Registries.STRUCTURE, ResourceLocation.parse(id));
    }

    public String id() { return tier.id + "_" + stone.id; }
    public VentTier tier() { return tier; }
    public OreStone stone() { return stone; }
    public int radius() { return radius(tier); }
    public int maxResults() { return maxResults(tier); }
    public int warmupTicks() {
        return switch (tier) { case WATER -> 40; case MOLTEN -> 30; case SUPERHEATED -> 4; };
    }
    public int signalIntervalTicks() {
        return switch (tier) { case WATER -> 24; case MOLTEN -> 20; case SUPERHEATED -> 2; };
    }
    public static int maxResults(VentTier tier) {
        return switch (tier) { case WATER -> 10; case MOLTEN -> 6; case SUPERHEATED -> 3; };
    }
    public static int radius(VentTier tier) {
        return switch (tier) { case WATER -> 1000; case MOLTEN -> 2000; case SUPERHEATED -> 3000; };
    }
    public List<ResourceKey<Structure>> structures() { return structures; }
    public Component label() { return Component.translatable("block." + VentRegistry.ventId(tier, stone, false).toLanguageKey()); }
    public ItemStack icon() { return new ItemStack(BuiltInRegistries.ITEM.get(VentRegistry.ventId(tier, stone, false))); }
    public boolean matches(BlockState state) { return cores.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock())); }
    public static List<ScannerTarget> forTier(VentTier tier) {
        return Arrays.stream(values()).filter(target -> target.tier == tier).toList();
    }
    public static ScannerTarget of(VentTier tier, OreStone stone) {
        return Arrays.stream(values()).filter(target -> target.tier == tier && target.stone == stone).findFirst().orElseThrow();
    }
    public static Optional<ScannerTarget> byId(String id) {
        return Arrays.stream(values()).filter(target -> target.id().equals(id)).findFirst();
    }
}
