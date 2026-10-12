package com.betongoat.durabilitytweaks.mixin;

import com.betongoat.durabilitytweaks.render.LegMirror;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if >=1.21.9 {
import net.minecraft.client.model.HumanoidModel;
//?} else {
/*import net.minecraft.client.model.HumanoidArmorModel;
*///?}

/** Player and mob armor: the only mirrored cube in the armor mesh is the left leg (see {@link LegMirror}). */
//? if >=1.21.9 {
@Mixin(HumanoidModel.class)
//?} else {
/*@Mixin(HumanoidArmorModel.class)
*///?}
public abstract class ArmorLegMirrorMixin {
    //? if >=1.21.9 {
    @WrapOperation(method = "createBaseArmorMesh", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/builders/CubeListBuilder;mirror()Lnet/minecraft/client/model/geom/builders/CubeListBuilder;"))
    //?} else {
    /*@WrapOperation(method = "createBodyLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/builders/CubeListBuilder;mirror()Lnet/minecraft/client/model/geom/builders/CubeListBuilder;"))
    *///?}
    private static CubeListBuilder durabilitytweaks$unmirrorLeftLeg(CubeListBuilder builder, Operation<CubeListBuilder> original) {
        return LegMirror.unmirrorWhileBuilding() ? builder : original.call(builder);
    }
}
