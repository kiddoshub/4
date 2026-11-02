
package com.gaspoweredcake.client33.utils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.gaspoweredcake.client33.events.render.Render3DEvent;
import com.gaspoweredcake.client33.renderer.Renderer3D;
import com.gaspoweredcake.client33.renderer.ShapeMode;
import com.gaspoweredcake.client33.utils.render.color.Color;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static com.gaspoweredcake.client33.Client33.mc;

public class WireframeEntityRenderer {
    private static final PoseStack matrices = new PoseStack();
    private static final InterceptingStorage interceptStorage = new InterceptingStorage();

    private static Renderer3D renderer;
    private static Color sideColor;
    private static Color lineColor;
    private static ShapeMode shapeMode;
    private static double offsetX, offsetY, offsetZ;
    private static java.util.List<SurfaceMesh.Face> captured;

    public static SurfaceMesh capture(Render3DEvent event, Entity entity) { return capture(event,entity,null); }

    public static SurfaceMesh capture(Render3DEvent event,Entity entity,@Nullable EntityRenderState state) {
        captured = new java.util.ArrayList<>();
        try {
            renderModel(event, entity, 1, Color.WHITE, Color.WHITE, ShapeMode.Sides,state);
            return new SurfaceMesh(captured);
        } finally { captured = null; }
    }

    private WireframeEntityRenderer() {
    }

    public static void render(Render3DEvent event, Entity entity, double scale, Color sideColor, Color lineColor, ShapeMode shapeMode) {
        renderModel(event,entity,scale,sideColor,lineColor,shapeMode,null);
    }

    @SuppressWarnings("unchecked")
    private static void renderModel(Render3DEvent event,Entity entity,double scale,Color sideColor,Color lineColor,ShapeMode shapeMode,@Nullable EntityRenderState suppliedState) {
        WireframeEntityRenderer.renderer = event.renderer;
        WireframeEntityRenderer.sideColor = sideColor;
        WireframeEntityRenderer.lineColor = lineColor;
        WireframeEntityRenderer.shapeMode = shapeMode;

        float tickDelta = mc.level.tickRateManager().isFrozen() ? 1 : event.tickDelta;

        offsetX = Mth.lerp(tickDelta, entity.xOld, entity.getX());
        offsetY = Mth.lerp(tickDelta, entity.yOld, entity.getY());
        offsetZ = Mth.lerp(tickDelta, entity.zOld, entity.getZ());

        var entityRenderer = (EntityRenderer<Entity, EntityRenderState>) mc.getEntityRenderDispatcher().getRenderer(entity);
        var state = suppliedState==null?entityRenderer.createRenderState(entity,tickDelta):suppliedState;
        if(suppliedState!=null) {offsetX=state.x;offsetY=state.y;offsetZ=state.z;}

        Vec3 entityOffset = entityRenderer.getRenderOffset(state);
        offsetX += entityOffset.x;
        offsetY += entityOffset.y;
        offsetZ += entityOffset.z;

        interceptStorage.vertexConsumer.i = 0;
        matrices.pushPose();
        try {
            matrices.scale((float) scale, (float) scale, (float) scale);
            entityRenderer.submit(state, matrices, interceptStorage, mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState);
        } finally {
            matrices.popPose();
            interceptStorage.vertexConsumer.i = 0;
            interceptStorage.clear();
        }
    }

    private static class InterceptingStorage extends SubmitNodeStorage {
        private final MyVertexConsumer vertexConsumer = new MyVertexConsumer();

        @Override
        public <S> void submitModel(
            @NonNull Model<? super S> model,
            @NonNull S state,
            @NonNull PoseStack poseStack,
            RenderType renderType,
            int lightCoords,
            int overlayCoords,
            int tintedColor,
            UvMapping sprite,
            int outlineColor
        ) {
            if (renderType.isOutline()) return;

            model.setupAnim(state);
            model.renderToBuffer(poseStack, vertexConsumer, lightCoords, overlayCoords, tintedColor);
        }

        @Override
        public void submitCustomGeometry(@NonNull PoseStack poseStack, RenderType renderType, SubmitNodeCollector.@NonNull CustomGeometryRenderer customGeometryRenderer) {
            if (renderType.isOutline()) return;
            customGeometryRenderer.render(poseStack.last(), vertexConsumer);
        }

        public void clear() {
            submitsPerOrder.clear();
        }
    }

    private static class MyVertexConsumer implements VertexConsumer {
        private final float[] xs = new float[4];
        private final float[] ys = new float[4];
        private final float[] zs = new float[4];
        private int i = 0;

        @Override
        public @NonNull VertexConsumer addVertex(float x, float y, float z) {
            xs[i] = x;
            ys[i] = y;
            zs[i] = z;
            if (++i == 4) {
                if (captured != null) {
                    if (captured.size() < SurfaceMesh.MAX_FACES) captured.add(new SurfaceMesh.Face(
                        new SurfaceMesh.Point(offsetX+xs[0], offsetY+ys[0], offsetZ+zs[0]),
                        new SurfaceMesh.Point(offsetX+xs[1], offsetY+ys[1], offsetZ+zs[1]),
                        new SurfaceMesh.Point(offsetX+xs[2], offsetY+ys[2], offsetZ+zs[2]),
                        new SurfaceMesh.Point(offsetX+xs[3], offsetY+ys[3], offsetZ+zs[3])));
                } else renderer.side(
                    offsetX + xs[0], offsetY + ys[0], offsetZ + zs[0],
                    offsetX + xs[1], offsetY + ys[1], offsetZ + zs[1],
                    offsetX + xs[2], offsetY + ys[2], offsetZ + zs[2],
                    offsetX + xs[3], offsetY + ys[3], offsetZ + zs[3],
                    sideColor,
                    lineColor,
                    shapeMode
                );
                i = 0;
            }
            return this;
        }

        @Override
        public @NonNull VertexConsumer setColor(int r, int g, int b, int a) {
            return this;
        }

        @Override
        public @NonNull VertexConsumer setColor(int argb) {
            return this;
        }

        @Override
        public @NonNull VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public @NonNull VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public @NonNull VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public @NonNull VertexConsumer setUv3(float u, float v) { return this; }

        @Override
        public @NonNull VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }

        @Override
        public @NonNull VertexConsumer setLineWidth(float w) {
            return this;
        }
    }
}
