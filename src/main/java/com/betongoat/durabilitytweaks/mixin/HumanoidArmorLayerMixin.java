package com.betongoat.durabilitytweaks.mixin;

//? if <1.21.2 {
/*import com.betongoat.durabilitytweaks.render.ArmorTextureManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// 1.21 - 1.21.1: armor is drawn by HumanoidArmorLayer.renderModel, once per armor material layer.
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {

    @WrapOperation(
        method = "renderArmorPiece",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/HumanoidArmorLayer;renderModel(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/model/HumanoidModel;ILnet/minecraft/resources/Identifier;)V")
    )
    private void durabilitytweaks$recolorArmor(
        HumanoidArmorLayer<?, ?, ?> layer, PoseStack poseStack, MultiBufferSource buffers, int light,
        HumanoidModel<?> model, int color, Identifier texture, Operation<Void> original,
        @Local(argsOnly = true) LivingEntity entity, @Local(argsOnly = true) EquipmentSlot slot
    ) {
        ItemStack stack = entity.getItemBySlot(slot);
        boolean own = entity == Minecraft.getInstance().player;
        Identifier swapped = ArmorTextureManager.remap(texture, stack, own);
        if (swapped != texture && ArmorTextureManager.replacesTint(stack, own)) {
            color = -1;
        }
        original.call(layer, poseStack, buffers, light, model, color, swapped);
    }
}
*///?}
