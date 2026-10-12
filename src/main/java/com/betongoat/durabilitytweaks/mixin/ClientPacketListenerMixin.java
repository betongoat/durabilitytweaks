package com.betongoat.durabilitytweaks.mixin;

import com.betongoat.durabilitytweaks.safety.ServerTextGuard;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    /** The sign editor copies the sign's (server supplied) text and sends it back: keep our keys vanilla there. */
    @WrapMethod(method = "handleOpenSignEditor")
    private void durabilitytweaks$guardSignEditor(ClientboundOpenSignEditorPacket packet, Operation<Void> original) {
        ServerTextGuard.enter();
        try {
            original.call(packet);
        } finally {
            ServerTextGuard.exit();
        }
    }
}
