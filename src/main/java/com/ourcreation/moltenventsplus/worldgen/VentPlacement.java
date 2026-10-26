package com.ourcreation.moltenventsplus.worldgen;

import com.ourcreation.moltenventsplus.VentConfig;
import com.ourcreation.moltenventsplus.compat.VentResource;
import com.ourcreation.moltenventsplus.vent.OreStone;
import com.ourcreation.moltenventsplus.vent.VentTier;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;

/** All materials in a tier share one placement grid and one mineral choice. */
public final class VentPlacement {
    public static boolean selects(Structure.GenerationContext context, VentTier tier, String material) {
        var materials = new ArrayList<String>();
        for (var stone : OreStone.values()) materials.add(stone.id);
        if (VentConfig.INTEGRATION_WORLDGEN.get()) {
            for (var resource : VentResource.values()) {
                if (resource != VentResource.OIL && resource.available()) materials.add(resource.id);
            }
        }
        var random = new WorldgenRandom(new LegacyRandomSource(0));
        random.setLargeFeatureWithSalt(context.seed(), context.chunkPos().x, context.chunkPos().z,
                0x4D565000 + tier.ordinal());
        return material.equals(materials.get(random.nextInt(materials.size())));
    }

    private VentPlacement() {}
}
