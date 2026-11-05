package com.ourcreation.moltenventsplus.compat.jade;

import com.ourcreation.moltenventsplus.MoltenVentsPlus;
import com.ourcreation.moltenventsplus.VentConfig;
import com.ourcreation.moltenventsplus.vent.VentBlock;
import com.ourcreation.moltenventsplus.vent.VentBlockEntity;
import com.ourcreation.moltenventsplus.vent.VentSettings;
import com.ourcreation.moltenventsplus.vent.VentTier;
import com.ourcreation.moltenventsplus.vent.MoltenVentReserve;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Uses Jade's bounded look-at requests rather than broadcasting every vent's NBT. */
public enum VentUsesProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final String REMAINING = "MvpVentUses";
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(MoltenVentsPlus.MODID, "vent_uses");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    private static boolean isWater(BlockAccessor accessor) {
        return accessor.getBlock() instanceof VentBlock vent && vent.tier == VentTier.WATER;
    }

    @Override
    public boolean shouldRequestData(BlockAccessor accessor) {
        return VentSettings.isVent(accessor.getBlockState());
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!VentSettings.isVent(accessor.getBlockState())) return;
        int remaining;
        if (isWater(accessor)) {
            remaining = accessor.getBlockEntity() instanceof VentBlockEntity entity
                    ? entity.remainingWaterUses() : VentConfig.WATER_CAPACITY.get();
        } else if (accessor.getBlock() instanceof VentBlock) {
            remaining = -1; // Superheated cores stay renewable.
        } else {
            remaining = accessor.getBlockEntity() instanceof MoltenVentReserve reserve
                    ? reserve.moltenVentsPlus$remainingUses() : VentConfig.MOLTEN_CAPACITY.get();
        }
        data.putInt(REMAINING, remaining);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!VentSettings.isVent(accessor.getBlockState())) return;
        if (accessor.getServerData().contains(REMAINING, Tag.TAG_INT)) {
            int remaining = accessor.getServerData().getInt(REMAINING);
            tooltip.add(Component.translatable("create_water_vents.vent.uses_remaining",
                    remaining < 0 ? Component.translatable("create_water_vents.vent.infinite")
                            : Component.literal(Integer.toString(remaining))));
        } else {
            // Never display a guessed count from the client's potentially different config.
            tooltip.add(Component.translatable("create_water_vents.vent.uses_pending"));
        }
    }
}
