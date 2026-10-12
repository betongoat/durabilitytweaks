package com.betongoat.durabilitytweaks.render;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.world.item.ItemStack;

/**
 * Remembers which armor stacks belong to the local player. Since 1.21.2 the armor renderer only
 * sees copies of the stacks in the entity's render state, so the copies are tagged when the state
 * is extracted. Stacks are compared by identity and forgotten once the game drops them.
 */
public final class OwnArmor {
    private static final Set<ItemStack> OWN = Collections.newSetFromMap(new WeakHashMap<>());

    public static void mark(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            OWN.add(stack);
        }
    }

    public static boolean isOwn(ItemStack stack) {
        return OWN.contains(stack);
    }

    private OwnArmor() {
    }
}
