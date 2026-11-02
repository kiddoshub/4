
package meteordevelopment.meteorclient.mixininterface;

import net.minecraft.client.gl.Framebuffer;

public interface IWorldRenderer {
    void meteor$pushEntityOutlineFramebuffer(Framebuffer framebuffer);

    void meteor$popEntityOutlineFramebuffer();
}
