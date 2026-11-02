package com.gaspoweredcake.client33.mixin;

import com.gaspoweredcake.client33.renderer.LineSmoothing;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.backend.opengl.GlDevice;
import com.mojang.renderpearl.backend.opengl.GlProgram;
import com.mojang.renderpearl.backend.opengl.GlRenderPipeline;
import com.mojang.renderpearl.backend.opengl.VertexArray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL11C.*;

@Mixin(GlRenderPipeline.class)
public abstract class GlRenderPipelineMixin {
    @Unique private boolean client33$lineSmooth;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void client33$rememberSmoothing(GlDevice device, BackendRenderPipeline.CreateInfo info,
                                           GlProgram program, VertexArray vertexArray, CallbackInfo ci) {
        client33$lineSmooth = LineSmoothing.enabled(info.name());
    }

    @Inject(method = "bind", at = @At("TAIL"))
    private void client33$applySmoothing(CallbackInfo ci) {
        if (client33$lineSmooth) {
            glEnable(GL_LINE_SMOOTH);
            glLineWidth(1);
        } else {
            glDisable(GL_LINE_SMOOTH);
        }
    }
}
