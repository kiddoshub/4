
package com.gaspoweredcake.client33.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.events.render.RenderBlockEntityEvent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private <S extends BlockEntityRenderState> void onRenderEntity(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        RenderBlockEntityEvent event = Client33.EVENT_BUS.post(RenderBlockEntityEvent.get(state));
        if (event.isCancelled()) ci.cancel();
    }
}
