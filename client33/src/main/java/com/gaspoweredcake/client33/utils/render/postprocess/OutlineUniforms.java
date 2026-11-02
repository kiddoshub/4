
package com.gaspoweredcake.client33.utils.render.postprocess;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.gaspoweredcake.client33.renderer.DynamicUniformStorage;
import org.jspecify.annotations.NonNull;

import java.nio.ByteBuffer;

public class OutlineUniforms {
    private static final int UNIFORM_SIZE = new Std140SizeCalculator()
        .putInt()
        .putFloat()
        .putInt()
        .putFloat()
        .get();

    private static final DynamicUniformStorage<Data> STORAGE = new DynamicUniformStorage<>("Client33 - Outline UBO", UNIFORM_SIZE, 16);

    public static void flipFrame() {
        STORAGE.endFrame();
    }

    public static GpuBufferSlice write(int width, float fillOpacity, int shapeMode, float glowMultiplier) {
        return STORAGE.writeUniform(new Data(width, fillOpacity, shapeMode, glowMultiplier));
    }

    private record Data(int width, float fillOpacity, int shapeMode,
                        float glowMultiplier) implements DynamicUniformStorage.DynamicUniform {
        @Override
        public void write(@NonNull ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putInt(width)
                .putFloat(fillOpacity)
                .putInt(shapeMode)
                .putFloat(glowMultiplier);
        }
    }
}
