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
import java.util.HashSet;
import java.util.Set;

/** Ambient effects on exposed stones, including native Create/Molten Vents blocks. */
public final class VentParticles {
    private static Map<Block, VentTier> stones;
    private static Set<Block> moltenCores;

    private static void initialize() {
        var stoneTypes = new HashMap<Block, VentTier>();
        var cores = new HashSet<Block>();
        for (var stone : OreStone.values()) {
            for (var tier : VentTier.values()) stoneTypes.put(stone.output(tier), tier);
            for (boolean active : new boolean[]{false, true})
                BuiltInRegistries.BLOCK.getOptional(VentRegistry.ventId(VentTier.MOLTEN, stone, active))
                        .ifPresent(cores::add);
        }
        stones = Map.copyOf(stoneTypes);
        moltenCores = Set.copyOf(cores);
    }

    /** Returns true only when replacing the selected native block's ambient tick. */
    public static boolean animate(Block block, BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (stones == null) initialize();
        if (moltenCores.contains(block)) return true;
        var tier = stones.get(block);
        if (tier == null) return false;
        if (random.nextInt(6) != 0) return true;
        var shape = state.getShape(level, pos);
        if (shape.isEmpty()) return true;
        var bounds = shape.bounds();
        var faces = Direction.values();
        int first = random.nextInt(faces.length);
        for (int i = 0; i < faces.length; i++) {
            var face = faces[(first + i) % faces.length];
            boolean positive = face.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            double plane = positive ? bounds.max(face.getAxis()) : bounds.min(face.getAxis());
            boolean boundary = positive ? plane >= 1 : plane <= 0;
            if (boundary && !Block.shouldRenderFace(state, level, pos, face, pos.relative(face))) continue;
            double x = bounds.minX + random.nextDouble() * bounds.getXsize();
            double y = bounds.minY + random.nextDouble() * bounds.getYsize();
            double z = bounds.minZ + random.nextDouble() * bounds.getZsize();
            switch (face.getAxis()) {
                case X -> x = plane + face.getStepX() * 0.02;
                case Y -> y = plane + face.getStepY() * 0.02;
                case Z -> z = plane + face.getStepZ() * 0.02;
            }
            level.addParticle(switch (tier) {
                case WATER -> ParticleTypes.DRIPPING_WATER;
                case MOLTEN -> ParticleTypes.FLAME;
                case SUPERHEATED -> ParticleTypes.SOUL_FIRE_FLAME;
            }, pos.getX() + x, pos.getY() + y, pos.getZ() + z, 0, 0, 0);
            break;
        }
        return true;
    }

    private VentParticles() {}
}
