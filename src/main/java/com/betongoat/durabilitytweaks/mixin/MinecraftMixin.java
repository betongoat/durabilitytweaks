package com.betongoat.durabilitytweaks.mixin;

import com.betongoat.durabilitytweaks.render.ArmorTextureManager;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    /** Resource packs changed (or F3+T): regenerate armor textures from the new pack's textures. */
    @Inject(method = "reloadResourcePacks()Ljava/util/concurrent/CompletableFuture;", at = @At("RETURN"))
    private void durabilitytweaks$clearArmorTextures(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        Minecraft minecraft = (Minecraft) (Object) this;
        cir.getReturnValue().thenRun(() -> minecraft.execute(ArmorTextureManager::clear));
    }
}
