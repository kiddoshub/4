
package com.gaspoweredcake.client33.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.gaspoweredcake.client33.mixininterface.IRenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RenderPipeline.class)
public abstract class RenderPipelineMixin implements IRenderPipeline {
    @Unique
    private boolean lineSmooth;

    @Override
    public void client33$setLineSmooth(boolean lineSmooth) {
        this.lineSmooth = lineSmooth;
    }

    @Override
    public boolean client33$getLineSmooth() {
        return lineSmooth;
    }
}
