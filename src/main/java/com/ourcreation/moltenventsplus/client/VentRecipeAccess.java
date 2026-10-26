package com.ourcreation.moltenventsplus.client;

import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import java.lang.reflect.Method;

/** Optional companion-core access check; standalone clients have no stage restrictions. */
public final class VentRecipeAccess {
    private static boolean initialized;
    private static Method locked;

    public static boolean isLocked(ItemStack stack) {
        if (!initialized) {
            initialized = true;
            if (ModList.get().isLoaded("create_new_world_core")) try {
                locked = Class.forName("com.ourcreation.createnewworldcore.client.JeiLockedItems")
                        .getMethod("isLocked", ItemStack.class);
            } catch (ReflectiveOperationException | LinkageError ignored) {}
        }
        if (locked != null) try {
            return Boolean.TRUE.equals(locked.invoke(null, stack));
        } catch (ReflectiveOperationException | LinkageError ignored) {}
        return false;
    }

    private VentRecipeAccess() {}
}
