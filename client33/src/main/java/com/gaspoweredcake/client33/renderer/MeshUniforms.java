
package com.gaspoweredcake.client33.renderer;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.gaspoweredcake.client33.renderer.DynamicUniformStorage;
import org.joml.Matrix4f;
import org.jspecify.annotations.NonNull;

import java.nio.ByteBuffer;

public class MeshUniforms {
    public static final int SIZE = new Std140SizeCalculator()
        .putMat4f()
        .putMat4f()
        .get();

    private static final DynamicUniformStorage<Data> STORAGE = new DynamicUniformStorage<>("Client33 - Mesh UBO", SIZE, 16);

    public static void flipFrame() {
        STORAGE.endFrame();
    }

    public static GpuBufferSlice write(Matrix4f proj, Matrix4f modelView) {
        // Uniform storage may retain its input for reuse: never hand it a mutated singleton.
        return STORAGE.writeUniform(new Data(new Matrix4f(proj), new Matrix4f(modelView)));
    }

    private record Data(Matrix4f proj, Matrix4f modelView) implements DynamicUniformStorage.DynamicUniform {
        @Override
        public void write(@NonNull ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putMat4f(proj)
                .putMat4f(modelView);
        }
    }
}
