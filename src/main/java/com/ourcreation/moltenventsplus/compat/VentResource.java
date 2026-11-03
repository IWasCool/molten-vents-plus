package com.ourcreation.moltenventsplus.compat;

import net.minecraft.util.StringRepresentable;

/** Serialized names retained solely to decode retired structure settings and piece NBT. */
public enum VentResource implements StringRepresentable {
    URANIUM("uranium"), THORIUM("thorium"), LEAD("lead"), OIL("oil");

    public final String id;
    VentResource(String id) { this.id = id; }
    @Override public String getSerializedName() { return id; }
}
