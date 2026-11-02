
package com.gaspoweredcake.client33.mixin;

import com.mojang.renderpearl.backend.opengl.GlDevice;
import com.mojang.renderpearl.backend.api.RenderPassBackend;
import com.gaspoweredcake.client33.mixininterface.IGpuDevice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GlDevice.class)
public abstract class GlDeviceMixin implements IGpuDevice {
    @Unique
    private int x, y, width, height;

    @Unique
    private boolean set;

    @Override
    public void client33$pushScissor(int x, int y, int width, int height) {
        if (set)
            throw new IllegalStateException("Currently there can only be one global scissor pushed");

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        set = true;
    }

    @Override
    public void client33$popScissor() {
        if (!set)
            throw new IllegalStateException("No scissor pushed");

        set = false;
    }

    @Deprecated
    @Override
    public void client33$onCreateRenderPass(RenderPassBackend backend) {
        if (set) {
            backend.enableScissor(x, y, width, height);
        }
    }
}
