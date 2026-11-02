package com.gaspoweredcake.client33.utils.render.postprocess;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.gaspoweredcake.client33.mixininterface.ILevelRenderer;
import net.minecraft.world.entity.Entity;

import static com.gaspoweredcake.client33.Client33.mc;

public abstract class EntityShader extends PostProcessShader {
    protected EntityShader(RenderPipeline pipeline) {
        super(pipeline);
    }

    public abstract boolean shouldDraw(Entity entity);

    @Override
    protected void preDraw() {
        ((ILevelRenderer) mc.levelRenderer).client33$pushEntityOutlineFramebuffer(framebuffer);
    }

    @Override
    protected void postDraw() {
        ((ILevelRenderer) mc.levelRenderer).client33$popEntityOutlineFramebuffer();
    }

    public void submitVertices() {
    }
}
