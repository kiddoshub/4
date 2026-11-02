
package com.gaspoweredcake.client33.mixininterface;

import com.mojang.blaze3d.pipeline.RenderTarget;

public interface ILevelRenderer {
    void client33$pushEntityOutlineFramebuffer(RenderTarget framebuffer);

    void client33$popEntityOutlineFramebuffer();
}
