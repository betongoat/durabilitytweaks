package com.betongoat.durabilitytweaks.mixin;

import com.betongoat.durabilitytweaks.safety.ServerTextGuard;
import com.google.common.collect.ImmutableList;
import java.util.List;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TranslatableContents.class)
public abstract class TranslatableContentsMixin {
    @Shadow @Final private String key;
    @Shadow @Final private String fallback;
    @Shadow private Language decomposedWith;
    @Shadow private List<FormattedText> decomposedParts;

    /** While text is prepared for a server, our keys resolve like on a client without this mod. */
    @Inject(method = "decompose", at = @At("HEAD"), cancellable = true)
    private void durabilitytweaks$hideOwnTranslations(CallbackInfo ci) {
        if (ServerTextGuard.active() && ServerTextGuard.isOwnKey(this.key)) {
            this.decomposedParts = ImmutableList.of(FormattedText.of(this.fallback != null ? this.fallback : this.key));
            // Not cached: the next normal use translates the key again
            this.decomposedWith = null;
            ci.cancel();
        }
    }
}
