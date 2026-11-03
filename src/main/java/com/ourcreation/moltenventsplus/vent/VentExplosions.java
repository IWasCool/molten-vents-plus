package com.ourcreation.moltenventsplus.vent;

import com.ourcreation.moltenventsplus.MoltenVentsPlus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/** Let the blast reach exposed dormant cores through water without damaging extra terrain. */
@EventBusSubscriber(modid = MoltenVentsPlus.MODID)
public final class VentExplosions {
    private VentExplosions() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void detonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var explosion = event.getExplosion();
        float power = explosion.radius();
        if (!Float.isFinite(power) || power <= 0 || !explosion.interactsWithBlocks()
                || explosion.getBlockInteraction() == Explosion.BlockInteraction.TRIGGER_BLOCK) return;

        // Bound the search even when another mod creates an unusually large explosion.
        double reach = Math.min(power, 8);
        var center = explosion.center();
        var sourcePos = BlockPos.containing(center);
        for (var pos : BlockPos.betweenClosed(
                Mth.floor(center.x - reach), Mth.floor(center.y - reach), Mth.floor(center.z - reach),
                Mth.floor(center.x + reach), Mth.floor(center.y + reach), Mth.floor(center.z + reach))) {
            if (pos.distToCenterSqr(center) > reach * reach || level.isOutsideBuildHeight(pos)
                    || !level.hasChunkAt(pos)) continue;
            var state = level.getBlockState(pos);
            if (!VentSettings.isVent(state)
                    || !BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().startsWith("dormant_")
                    || event.getAffectedBlocks().contains(pos)) continue;

            if (!level.hasChunksAt(Math.min(sourcePos.getX(), pos.getX()), Math.min(sourcePos.getZ(), pos.getZ()),
                    Math.max(sourcePos.getX(), pos.getX()), Math.max(sourcePos.getZ(), pos.getZ()))) continue;

            var hit = level.clip(new ClipContext(center, Vec3.atCenterOf(pos),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, explosion.getDirectSourceEntity()));
            if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos)) {
                // Keep native activation callbacks and let later protection listeners veto this block.
                event.getAffectedBlocks().add(pos.immutable());
            }
        }
    }
}
