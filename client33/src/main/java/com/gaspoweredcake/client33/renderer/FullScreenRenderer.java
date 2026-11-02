
package com.gaspoweredcake.client33.renderer;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.gaspoweredcake.client33.utils.PreInit;

public class FullScreenRenderer {
    public static GpuBuffer vbo;
    public static GpuBuffer ibo;

    /**
     * Deprecated for performance reasons, use {@link MeshRenderer#fullscreen()} or the {@link FullScreenRenderer#vbo}
     * and {@link FullScreenRenderer#ibo} buffer objects instead.
     */
    @Deprecated(forRemoval = true)
    public static MeshBuilder mesh;

    private FullScreenRenderer() {
    }

    @PreInit
    public static void init() {
        mesh = new MeshBuilder(Client33VertexFormats.POS2, PrimitiveTopology.TRIANGLES, 4, 6);

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
