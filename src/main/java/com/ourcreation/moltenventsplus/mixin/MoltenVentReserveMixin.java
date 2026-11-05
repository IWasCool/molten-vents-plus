package com.ourcreation.moltenventsplus.mixin;

import com.Apothic0n.MoltenVents.core.objects.ActiveMoltenBlockEntity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.ourcreation.moltenventsplus.VentConfig;
import com.ourcreation.moltenventsplus.vent.MoltenVentReserve;
import com.ourcreation.moltenventsplus.vent.OreStone;
import com.ourcreation.moltenventsplus.vent.VentSettings;
import com.ourcreation.moltenventsplus.vent.VentTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Keep the provider's configured conversion rules, charging only successful block writes. */
@Mixin(value = ActiveMoltenBlockEntity.class, remap = false)
abstract class MoltenVentReserveMixin extends BlockEntity implements MoltenVentReserve {
    @Unique private static final ThreadLocal<MoltenVentReserveMixin> moltenVentsPlus$producing = new ThreadLocal<>();
    @Unique private int moltenVentsPlus$remaining = -1;

    protected MoltenVentReserveMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public int moltenVentsPlus$remainingUses() {
        return moltenVentsPlus$remaining < 0 ? VentConfig.MOLTEN_CAPACITY.get() : moltenVentsPlus$remaining;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        moltenVentsPlus$remaining = tag.contains("MvpMoltenRemaining", Tag.TAG_INT)
                ? Math.max(0, tag.getInt("MvpMoltenRemaining")) : -1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (moltenVentsPlus$remaining >= 0) tag.putInt("MvpMoltenRemaining", moltenVentsPlus$remaining);
    }

    @WrapMethod(method = "tick")
    private static void moltenVentsPlus$trackProduction(Level level, BlockPos pos, BlockState state,
                                                       BlockEntity entity, Operation<Void> original) {
        if (level.isClientSide || !VentSettings.isVent(state)) {
            original.call(level, pos, state, entity);
            return;
        }
        var reserve = (MoltenVentReserveMixin) (Object) entity;
        if (reserve.moltenVentsPlus$remaining < 0 && VentConfig.MOLTEN_CAPACITY.get() >= 0) {
            reserve.moltenVentsPlus$remaining = VentConfig.MOLTEN_CAPACITY.get();
            reserve.setChanged();
        }
        var previous = moltenVentsPlus$producing.get();
        moltenVentsPlus$producing.set(reserve);
        try {
            if (reserve.moltenVentsPlus$remaining != 0) original.call(level, pos, state, entity);
        } finally {
            if (previous == null) moltenVentsPlus$producing.remove();
            else moltenVentsPlus$producing.set(previous);
            if (reserve.moltenVentsPlus$remaining == 0 && level.getBlockEntity(pos) == entity) {
                String material = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath()
                        .substring("active_molten_".length());
                for (var stone : OreStone.values()) {
                    if (stone.id.equals(material)) {
                        level.setBlock(pos, stone.output(VentTier.MOLTEN).defaultBlockState(), Block.UPDATE_ALL);
                        break;
                    }
                }
            }
        }
    }

    @WrapOperation(method = {"spreadBlock", "convertTouching"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/WorldGenLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static boolean moltenVentsPlus$chargeConversion(WorldGenLevel level, BlockPos pos, BlockState output,
                                                           int flags, Operation<Boolean> original) {
        var reserve = moltenVentsPlus$producing.get();
        if (reserve == null) return original.call(level, pos, output, flags);
        if (reserve.moltenVentsPlus$remaining == 0 || !level.hasChunkAt(pos)
                || level.getBlockEntity(pos) != null || VentSettings.isVent(level.getBlockState(pos))) return false;
        boolean changed = original.call(level, pos, output, flags);
        if (changed && reserve.moltenVentsPlus$remaining > 0) {
            reserve.moltenVentsPlus$remaining--;
            reserve.setChanged();
        }
        return changed;
    }
}
