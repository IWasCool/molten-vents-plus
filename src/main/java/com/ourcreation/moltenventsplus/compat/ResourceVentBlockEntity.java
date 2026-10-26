package com.ourcreation.moltenventsplus.compat;

import com.ourcreation.moltenventsplus.VentConfig;
import com.ourcreation.moltenventsplus.registry.ResourceVentRegistry;
import com.ourcreation.moltenventsplus.vent.VentBlockEntity;
import com.ourcreation.moltenventsplus.vent.VentTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import java.util.Optional;

public final class ResourceVentBlockEntity extends BlockEntity {
    private String oilId = "";

    public ResourceVentBlockEntity(BlockPos pos, BlockState state) {
        super(ResourceVentRegistry.ENTITY.get(), pos, state);
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel server) || !(getBlockState().getBlock() instanceof ResourceVentBlock vent)
                || !vent.active) return;
        if (vent.resource != VentResource.OIL) return;
        if (Math.floorMod(server.getGameTime() + worldPosition.asLong(), VentConfig.OIL_INTERVAL.get()) == 0)
            refillOil(server);
    }

    /** Saves a provider identity; removing that provider pauses this vent rather than changing its product. */
    public boolean selectOil(Fluid fluid) {
        if (!OilFluids.allowed(fluid)) return false;
        oilId = BuiltInRegistries.FLUID.getKey(fluid).toString();
        setChanged();
        return true;
    }

    public String selectedOilId() { return oilId; }

    public Optional<Fluid> selectedOil() {
        if (oilId.isEmpty()) return Optional.empty();
        var id = ResourceLocation.tryParse(oilId);
        return id == null ? Optional.empty() : BuiltInRegistries.FLUID.getOptional(id).filter(OilFluids::allowed);
    }

    /** At most one source, directly above the core. No traversal or replacement of other liquids/blocks. */
    public boolean refillOil(ServerLevel server) {
        if (oilId.isEmpty()) OilFluids.preferred().ifPresent(this::selectOil);
        var fluid = selectedOil();
        var outlet = worldPosition.above();
        if (fluid.isEmpty() || !server.hasChunkAt(outlet) || server.getBlockEntity(outlet) != null) return false;
        var state = server.getBlockState(outlet);
        var existing = state.getFluidState();
        boolean ownFlow = state.getBlock() instanceof LiquidBlock && !existing.isSource()
                && existing.getType().isSame(fluid.get());
        if (!state.isAir() && !ownFlow) return false;
        return server.setBlock(outlet, fluid.get().defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!oilId.isEmpty()) tag.putString("OilFluid", oilId);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        oilId = tag.getString("OilFluid");
    }
}
