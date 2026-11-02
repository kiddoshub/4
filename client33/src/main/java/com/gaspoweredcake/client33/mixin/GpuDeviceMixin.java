
package com.gaspoweredcake.client33.mixin;

import com.mojang.renderpearl.frontend.FrontendGpuDevice;
import com.mojang.renderpearl.backend.api.GpuDeviceBackend;
import com.mojang.renderpearl.backend.api.RenderPassBackend;
import com.gaspoweredcake.client33.mixininterface.IGpuDevice;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FrontendGpuDevice.class)
public abstract class GpuDeviceMixin implements IGpuDevice {
    @Shadow
    @Final
    private GpuDeviceBackend backend;

    @Override
    public void client33$pushScissor(int x, int y, int width, int height) {
        ((IGpuDevice) backend).client33$pushScissor(x, y, width, height);
    }

    @Override
    public void client33$popScissor() {
        ((IGpuDevice) backend).client33$popScissor();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void client33$onCreateRenderPass(RenderPassBackend backend) {
        ((IGpuDevice) this.backend).client33$onCreateRenderPass(backend);
    }
}
