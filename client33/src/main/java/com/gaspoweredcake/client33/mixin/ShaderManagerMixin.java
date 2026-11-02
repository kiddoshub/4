
package com.gaspoweredcake.client33.mixin;

import com.gaspoweredcake.client33.renderer.Client33RenderPipelines;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.PipelineCache;
import net.minecraft.client.renderer.ShaderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderManager.class)
public abstract class ShaderManagerMixin {
    @Inject(method = "apply(Lcom/mojang/renderpearl/api/device/GpuDevice;Lnet/minecraft/client/renderer/ShaderManager$PendingResults;)V", at = @At("TAIL"))
    private void client33$reloadPipelines(CallbackInfo ci, @Local(ordinal = 0) PipelineCache pipelineCache) {
        Client33RenderPipelines.precompile(pipelineCache);
    }
}
