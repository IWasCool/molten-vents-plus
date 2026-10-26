package com.ourcreation.moltenventsplus.registry;

import com.ourcreation.moltenventsplus.compat.ResourceVentBlock;
import com.ourcreation.moltenventsplus.compat.ResourceVentBlockEntity;
import com.ourcreation.moltenventsplus.compat.VentResource;
import com.ourcreation.moltenventsplus.vent.VentTier;
import com.ourcreation.moltenventsplus.worldgen.ResourceVentPiece;
import com.ourcreation.moltenventsplus.worldgen.ResourceVentStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.ourcreation.moltenventsplus.MoltenVentsPlus.MODID;

public final class ResourceVentRegistry {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    private static final DeferredRegister<StructureType<?>> STRUCTURES = DeferredRegister.create(Registries.STRUCTURE_TYPE, MODID);
    private static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, MODID);
    public static final Map<String, DeferredBlock<ResourceVentBlock>> VENTS = new LinkedHashMap<>();

    static {
        registerVent(VentResource.OIL, VentTier.WATER, false);
        registerVent(VentResource.OIL, VentTier.WATER, true);
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResourceVentBlockEntity>> ENTITY =
            ENTITIES.register("resource_vent", () -> BlockEntityType.Builder.of(ResourceVentBlockEntity::new,
                    VENTS.values().stream().map(DeferredBlock::get).filter(vent -> vent.active).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<StructureType<?>, StructureType<ResourceVentStructure>> STRUCTURE =
            STRUCTURES.register("resource_vent", () -> () -> ResourceVentStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> PIECE =
            PIECES.register("resource_vent", () -> (StructurePieceType.ContextlessType) ResourceVentPiece::new);

    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5F, 6)
                .sound(SoundType.TUFF).requiresCorrectToolForDrops();
    }

    private static void registerVent(VentResource resource, VentTier tier, boolean active) {
        String name = name(resource, tier, active);
        var block = BLOCKS.register(name, () -> {
            var properties = properties().strength(-1, active ? 1200 : 6).pushReaction(PushReaction.BLOCK)
                    .lightLevel(state -> active && resource != VentResource.OIL ? tier == VentTier.WATER ? 5 : 15 : 0);
            return active ? new com.ourcreation.moltenventsplus.compat.ActiveResourceVentBlock(resource, tier, properties)
                    : new ResourceVentBlock(resource, tier, false, properties);
        });
        VENTS.put(name, block);
        ITEMS.registerSimpleBlockItem(name, block);
    }

    public static String name(VentResource resource, VentTier tier, boolean active) {
        return (active ? "active_" : "dormant_") + (resource == VentResource.OIL ? "oil" : tier.id + "_" + resource.id);
    }

    public static ResourceLocation id(VentResource resource, VentTier tier, boolean active) {
        return ResourceLocation.fromNamespaceAndPath(MODID, name(resource, tier, active));
    }

    public static ResourceVentBlock block(VentResource resource, VentTier tier, boolean active) {
        return VENTS.get(name(resource, tier, active)).get();
    }

    public static void displayItems(net.minecraft.world.item.CreativeModeTab.Output output) {
        if (!VentResource.OIL.available()) return;
        output.accept(block(VentResource.OIL, VentTier.WATER, false));
        output.accept(block(VentResource.OIL, VentTier.WATER, true));
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        STRUCTURES.register(bus);
        PIECES.register(bus);
    }

    private ResourceVentRegistry() {}
}
