package com.betongoat.durabilitytweaks.mixin;

//? if >=1.21.2 {
import com.betongoat.durabilitytweaks.render.OwnArmor;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Tags the armor copies in the local player's render state, so the "whose armor" option can tell them apart.
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    // The (entity, partialTick) overload; there is also an abstract no-argument one
    @Inject(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", at = @At("RETURN"))
    private void durabilitytweaks$tagOwnArmor(CallbackInfoReturnable<EntityRenderState> cir, @Local(argsOnly = true) Entity entity) {
        if (entity != Minecraft.getInstance().player || !(cir.getReturnValue() instanceof HumanoidRenderState state)) {
            return;
        }
        //? if >=1.21.4 {
        OwnArmor.mark(state.headEquipment);
        OwnArmor.mark(state.chestEquipment);
        OwnArmor.mark(state.legsEquipment);
        OwnArmor.mark(state.feetEquipment);
        //?} else {
        /*OwnArmor.mark(state.headItem);
        OwnArmor.mark(state.chestItem);
        OwnArmor.mark(state.legsItem);
        OwnArmor.mark(state.feetItem);
        *///?}
    }
}
//?}
