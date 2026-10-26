package com.ourcreation.moltenventsplus.compat;

import com.ourcreation.moltenventsplus.vent.VentTier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import java.util.Optional;

/** Retired mineral values only decode existing structure-piece NBT. Oil is the sole resource vent. */
public enum VentResource implements StringRepresentable {
    URANIUM("uranium"), THORIUM("thorium"), LEAD("lead"), OIL("oil");

    public final String id;
    public final int color = 0x30272B;

    VentResource(String id) { this.id = id; }

    public boolean available() { return this == OIL && !OilFluids.outputs().isEmpty(); }

    /** Pending pieces for removed mineral integrations intentionally place nothing. */
    public Optional<Block> output(VentTier tier) { return Optional.empty(); }

    @Override public String getSerializedName() { return id; }
}
