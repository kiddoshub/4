
package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import meteordevelopment.meteorclient.utils.PreInit;

public class FullScreenRenderer {
    public static GpuBuffer vbo;
    public static GpuBuffer ibo;

    @Deprecated(forRemoval = true)
    public static MeshBuilder mesh;

    private FullScreenRenderer() {}

    @PreInit
    public static void init() {
        mesh = new MeshBuilder(MeteorVertexFormats.POS2, VertexFormat.DrawMode.TRIANGLES, 4, 6);

        mesh.begin();

        mesh.quad(
            mesh.vec2(-1, -1).next(),
            mesh.vec2(-1, 1).next(),
            mesh.vec2(1, 1).next(),
            mesh.vec2(1, -1).next()
        );

        mesh.end();

        vbo = mesh.getVertexBuffer();
        ibo = mesh.getIndexBuffer();
    }
}
