package com.betongoat.durabilitytweaks.mixin;

//? if >=1.21.2 {
import com.betongoat.durabilitytweaks.render.ArmorTextureManager;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if >=1.21.4 {
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
//?} else {
/*import net.minecraft.world.item.equipment.EquipmentModel.LayerType;
*///?}

// 1.21.2+: every armor layer is drawn by EquipmentLayerRenderer.renderLayers, which looks up
// the layer texture through a memoized Function. Swapping the looked-up texture recolours
// the armor on players, mobs and armor stands alike.
@Mixin(EquipmentLayerRenderer.class)
public abstract class EquipmentLayerRendererMixin {

    @ModifyExpressionValue(
        method = "renderLayers*",
        at = @At(value = "INVOKE", target = "Ljava/util/function/Function;apply(Ljava/lang/Object;)Ljava/lang/Object;")
    )
    private Object durabilitytweaks$swapArmorTexture(Object looked, @Local(argsOnly = true) ItemStack stack) {
        // The trim lookup goes through Function.apply too; it returns sprites, not texture ids
        if (looked instanceof Identifier texture) {
            return ArmorTextureManager.remap(texture, stack);
        }
        return looked;
    }

    @ModifyExpressionValue(
        method = "renderLayers*",
        //? if >=1.21.4 {
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/EquipmentLayerRenderer;getColorForLayer(Lnet/minecraft/client/resources/model/EquipmentClientInfo$Layer;I)I")
        //?} else {
        /*at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/EquipmentLayerRenderer;getColorForLayer(Lnet/minecraft/world/item/equipment/EquipmentModel$Layer;I)I")
        *///?}
    )
    private int durabilitytweaks$dropDyeTint(int color, @Local(argsOnly = true) LayerType layerType, @Local(argsOnly = true) ItemStack stack) {
        // 0 means "skip this layer"; keep that, only replace real tints
        if (color != 0 && isHumanoid(layerType) && ArmorTextureManager.replacesTint(stack)) {
            return -1;
        }
        return color;
    }

    private static boolean isHumanoid(LayerType layerType) {
        String name = layerType.getSerializedName();
        return name.equals("humanoid") || name.equals("humanoid_leggings");
    }
}
//?}
