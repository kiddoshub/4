
package com.gaspoweredcake.client33.mixininterface;

import com.mojang.renderpearl.backend.api.RenderPassBackend;

public interface IGpuDevice {
    /**
     * Currently there can only be a single scissor pushed at once.
     */
    void client33$pushScissor(int x, int y, int width, int height);

    void client33$popScissor();

    /**
     * This is an *INTERNAL* method, it shouldn't be called.
     */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    void client33$onCreateRenderPass(RenderPassBackend backend);
}
