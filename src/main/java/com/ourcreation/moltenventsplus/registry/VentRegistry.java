package com.ourcreation.moltenventsplus.registry;

import com.ourcreation.moltenventsplus.vent.*;
import com.ourcreation.moltenventsplus.worldgen.VentFeature;
import com.ourcreation.moltenventsplus.worldgen.VentPiece;
import com.ourcreation.moltenventsplus.worldgen.VentStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import static com.ourcreation.moltenventsplus.MoltenVentsPlus.MODID;

public final class VentRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MODID);
    private static final DeferredRegister<StructureType<?>> STRUCTURES = DeferredRegister.create(Registries.STRUCTURE_TYPE, MODID);
    private static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final Map<String, DeferredBlock<? extends Block>> ENTRIES = new LinkedHashMap<>();
    public static final DeferredItem<Item> COAL_NUGGET = ITEMS.registerSimpleItem("coal_nugget");
    public static final DeferredItem<Item> NETHERITE_NUGGET = ITEMS.registerSimpleItem("netherite_nugget", new Item.Properties().fireResistant());

    static {
        for (var stone : OreStone.values()) {
            register("impure_" + stone.id, () -> new WeatheredVentStoneBlock(stoneProperties()));
            register("pure_" + stone.id, () -> new WeatheredVentStoneBlock(stoneProperties()));
            for (var tier : new VentTier[] {VentTier.WATER, VentTier.SUPERHEATED}) {
                register("dormant_" + tier.id + "_" + stone.id, () -> new VentBlock(tier, stone, false,
                        stoneProperties().strength(-1, 6).pushReaction(PushReaction.BLOCK)));
                register("active_" + tier.id + "_" + stone.id, () -> new ActiveVentBlock(tier, stone,
                        stoneProperties().strength(-1, 1200).pushReaction(PushReaction.BLOCK)
                                .lightLevel(state -> tier == VentTier.WATER ? 5 : 15)));
            }
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VentBlockEntity>> VENT_ENTITY =
            ENTITIES.register("active_vent", () -> BlockEntityType.Builder.of(VentBlockEntity::new,
                    ENTRIES.entrySet().stream().filter(e -> e.getKey().startsWith("active_"))
                            .map(e -> e.getValue().get()).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<Feature<?>, VentFeature> VENT_FEATURE = FEATURES.register("vent", VentFeature::new);
    public static final DeferredHolder<StructureType<?>, StructureType<VentStructure>> VENT_STRUCTURE =
            STRUCTURES.register("vent", () -> () -> VentStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> VENT_PIECE =
            PIECES.register("vent", () -> (StructurePieceType.ContextlessType) VentPiece::new);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("vents", () ->
            CreativeModeTab.builder().title(Component.translatable("itemGroup." + MODID))
                    .icon(() -> new ItemStack(block(VentTier.WATER, OreStone.ASURINE, true)))
                    .displayItems((parameters, output) -> {
                        for (var stone : OreStone.values()) {
                            output.accept(BuiltInRegistries.ITEM.get(ventId(VentTier.MOLTEN, stone, false)));
                            output.accept(BuiltInRegistries.ITEM.get(ventId(VentTier.MOLTEN, stone, true)));
                        }
                        ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                        if (!SuperheatedFluidRegistry.hasProvider()) output.accept(SuperheatedFluidRegistry.BUCKET.get());
                        ResourceVentRegistry.displayItems(output);
                    })
                    .build());

    private static BlockBehaviour.Properties stoneProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F, 6)
                .sound(SoundType.TUFF).requiresCorrectToolForDrops();
    }

    private static <T extends Block> DeferredBlock<T> register(String name, Supplier<T> factory) {
        var block = BLOCKS.register(name, factory);
        ENTRIES.put(name, block);
        ITEMS.registerSimpleBlockItem(name, block);
        return block;
    }

    public static Block block(VentTier tier, OreStone stone, boolean active) {
        return ENTRIES.get((active ? "active_" : "dormant_") + tier.id + "_" + stone.id).get();
    }

    public static ResourceLocation ventId(VentTier tier, OreStone stone, boolean active) {
        return ResourceLocation.fromNamespaceAndPath(tier == VentTier.MOLTEN ? "molten_vents" : MODID,
                (active ? "active_" : "dormant_") + tier.id + "_" + stone.id);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        FEATURES.register(bus);
        STRUCTURES.register(bus);
        PIECES.register(bus);
        TABS.register(bus);
    }

    private VentRegistry() {}
}
