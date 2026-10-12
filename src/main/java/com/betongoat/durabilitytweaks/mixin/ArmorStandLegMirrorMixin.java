package com.betongoat.durabilitytweaks.mixin;

import com.betongoat.durabilitytweaks.render.LegMirror;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if >=1.21.11 {
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
//?} else {
/*import net.minecraft.client.model.ArmorStandArmorModel;
*///?}

/** Armor stand armor: same left leg as {@link ArmorLegMirrorMixin}. */
@Mixin(ArmorStandArmorModel.class)
public abstract class ArmorStandLegMirrorMixin {
    //? if >=1.21.9 {
    @WrapOperation(method = "createBaseMesh", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/builders/CubeListBuilder;mirror()Lnet/minecraft/client/model/geom/builders/CubeListBuilder;"))
    //?} else {
    /*@WrapOperation(method = "createBodyLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/builders/CubeListBuilder;mirror()Lnet/minecraft/client/model/geom/builders/CubeListBuilder;"))
    *///?}
    private static CubeListBuilder durabilitytweaks$unmirrorLeftLeg(CubeListBuilder builder, Operation<CubeListBuilder> original) {
        return LegMirror.unmirrorWhileBuilding() ? builder : original.call(builder);
    }
}
