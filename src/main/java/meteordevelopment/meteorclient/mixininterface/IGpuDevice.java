
package meteordevelopment.meteorclient.mixininterface;

import com.mojang.blaze3d.systems.RenderPass;

public interface IGpuDevice {
    void meteor$pushScissor(int x, int y, int width, int height);

    void meteor$popScissor();

    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    void meteor$onCreateRenderPass(RenderPass pass);
}
