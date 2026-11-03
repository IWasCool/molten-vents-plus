package com.ourcreation.moltenventsplus.registry;

import com.ourcreation.moltenventsplus.worldgen.ResourceVentPiece;
import com.ourcreation.moltenventsplus.worldgen.ResourceVentStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

import static com.ourcreation.moltenventsplus.MoltenVentsPlus.MODID;

/** Retired resource IDs decode saved starts; no resource block, item or generator is active. */
public final class ResourceVentRegistry {
    private static final DeferredRegister<StructureType<?>> STRUCTURES = DeferredRegister.create(Registries.STRUCTURE_TYPE, MODID);
    private static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, MODID);
    public static final DeferredHolder<StructureType<?>, StructureType<ResourceVentStructure>> STRUCTURE =
            STRUCTURES.register("resource_vent", () -> () -> ResourceVentStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> PIECE =
            PIECES.register("resource_vent", () -> (StructurePieceType.ContextlessType) ResourceVentPiece::new);

    public static void register(IEventBus bus) {
        STRUCTURES.register(bus);
        PIECES.register(bus);
        bus.addListener(ResourceVentRegistry::removedOil);
    }

    private static void removedOil(RegisterEvent event) {
        var items = event.getRegistry(Registries.ITEM);
        var blocks = event.getRegistry(Registries.BLOCK);
        for (var name : new String[]{"dormant_oil", "active_oil"}) {
            var id = ResourceLocation.fromNamespaceAndPath(MODID, name);
            if (items != null) items.addAlias(id, ResourceLocation.withDefaultNamespace("air"));
            if (blocks != null) blocks.addAlias(id, ResourceLocation.withDefaultNamespace("stone"));
        }
    }

    private ResourceVentRegistry() {}
}
