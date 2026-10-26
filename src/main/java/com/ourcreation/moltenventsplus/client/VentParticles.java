package com.ourcreation.moltenventsplus.client;

import com.ourcreation.moltenventsplus.compat.VentResource;
import com.ourcreation.moltenventsplus.registry.VentRegistry;
import com.ourcreation.moltenventsplus.vent.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.HashMap;
import java.util.Map;

/** Ambient effects on exposed stones, including native Create/Molten Vents blocks. */
public final class VentParticles {
    private static Map<Block, VentTier> stones;
    private static Map<Block, Boolean> moltenCores;

    private static void initialize() {
        var stoneTypes = new HashMap<Block, VentTier>();
        var cores = new HashMap<Block, Boolean>();
        for (var stone : OreStone.values()) {
            for (var tier : VentTier.values()) stoneTypes.put(stone.output(tier), tier);
            for (boolean active : new boolean[]{false, true})
                BuiltInRegistries.BLOCK.getOptional(VentRegistry.ventId(VentTier.MOLTEN, stone, active))
                        .ifPresent(block -> cores.put(block, active));
        }
        for (var resource : VentResource.values()) if (resource != VentResource.OIL && resource.available())
            for (var tier : VentTier.values()) resource.output(tier).ifPresent(block -> stoneTypes.put(block, tier));
        stones = Map.copyOf(stoneTypes);
        moltenCores = Map.copyOf(cores);
    }

    /** Returns true only when replacing the selected native block's ambient tick. */
    public static boolean animate(Block block, BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (stones == null) initialize();
        var active = moltenCores.get(block);
        var tier = active != null ? VentTier.MOLTEN : stones.get(block);
        if (tier == null) return false;
        if (Boolean.FALSE.equals(active) || random.nextInt(active != null ? 3 : 6) != 0) return true;
        if (level.getBlockState(pos.above()).isSolidRender(level, pos.above())) return true;
        var shape = state.getShape(level, pos);
        double height = shape.isEmpty() ? 1 : shape.max(Direction.Axis.Y);
        level.addParticle(switch (tier) {
            case WATER -> ParticleTypes.DRIPPING_WATER;
            case MOLTEN -> ParticleTypes.FLAME;
            case SUPERHEATED -> ParticleTypes.SOUL_FIRE_FLAME;
        }, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + height + 0.02,
                pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, tier == VentTier.WATER ? 0 : 0.02, 0);
        return true;
    }

    private VentParticles() {}
}
