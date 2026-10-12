package com.betongoat.durabilitytweaks.mixin;

import com.betongoat.durabilitytweaks.safety.ServerTextGuard;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {

    /** Key binding components ({"keybind": ...}) of this mod show the raw key while text is prepared for a server. */
    @ModifyReturnValue(method = "createNameSupplier", at = @At("RETURN"))
    private static Supplier<Component> durabilitytweaks$hideOwnKeyBinding(Supplier<Component> original, String name) {
        if (!ServerTextGuard.isOwnKey(name)) {
            return original;
        }
        return () -> ServerTextGuard.active() ? Component.translatable(name) : original.get();
    }
}
