
package com.gaspoweredcake.client33.events.render;

import com.gaspoweredcake.client33.renderer.Renderer3D;
import com.gaspoweredcake.client33.utils.Utils;
import com.mojang.blaze3d.vertex.PoseStack;

public class Render3DEvent {
    private static final Render3DEvent INSTANCE = new Render3DEvent();

    public PoseStack matrices;
    public Renderer3D renderer;
    public Renderer3D depthRenderer;
    public double frameTime;
    public float tickDelta;
    public double offsetX, offsetY, offsetZ;

    public static Render3DEvent get(PoseStack matrices, Renderer3D renderer, Renderer3D depthRenderer, float tickDelta, double offsetX, double offsetY, double offsetZ) {
        INSTANCE.matrices = matrices;
        INSTANCE.renderer = renderer;
        INSTANCE.depthRenderer = depthRenderer;
        INSTANCE.frameTime = Utils.frameTime;
        INSTANCE.tickDelta = tickDelta;
        INSTANCE.offsetX = offsetX;
        INSTANCE.offsetY = offsetY;
        INSTANCE.offsetZ = offsetZ;
        return INSTANCE;
    }
}
