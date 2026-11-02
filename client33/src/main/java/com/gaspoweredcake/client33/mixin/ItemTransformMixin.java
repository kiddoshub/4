
package com.gaspoweredcake.client33.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.events.render.ApplyTransformationEvent;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemTransform.class)
public abstract class ItemTransformMixin {
    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void onApply(boolean applyLeftHandFix, PoseStack.Pose pose, CallbackInfo ci) {
        ApplyTransformationEvent event = Client33.EVENT_BUS.post(ApplyTransformationEvent.get((ItemTransform) (Object) this, applyLeftHandFix));
        if (event.isCancelled()) ci.cancel();
    }
}
