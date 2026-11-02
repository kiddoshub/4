
package com.gaspoweredcake.client33.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import com.gaspoweredcake.client33.mixininterface.IEntityRenderState;
import com.gaspoweredcake.client33.mixininterface.ILevelRenderer;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.render.BlockSelection;
import com.gaspoweredcake.client33.systems.modules.render.ESP;
import com.gaspoweredcake.client33.systems.modules.render.NoRender;
import com.gaspoweredcake.client33.utils.OutlineRenderCommandQueue;
import com.gaspoweredcake.client33.utils.render.color.Color;
import com.gaspoweredcake.client33.utils.render.postprocess.EntityShader;
import com.gaspoweredcake.client33.utils.render.postprocess.PostProcessShaders;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

import static com.gaspoweredcake.client33.Client33.mc;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin implements ILevelRenderer {
    @Unique
    private FeatureRenderDispatcher renderDispatcher;
    @Unique
    private final OutlineRenderCommandQueue outlineRenderCommandQueue = new OutlineRenderCommandQueue();

    @Inject(method = "checkPoseStack", at = @At("HEAD"), cancellable = true)
    private void onCheckPoseStack(PoseStack poseStack, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "submitBlockOutline", at = @At("HEAD"), cancellable = true)
    private void onDrawHighlightedHitOutline(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, LevelRenderState levelRenderState, CallbackInfo ci) {
        if (Modules.get().isActive(BlockSelection.class)) ci.cancel();
    }

    // No Render

    @ModifyExpressionValue(method = "addSkyPass", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/level/CameraEntityRenderState;doesMobEffectBlockSky:Z", opcode = Opcodes.GETFIELD))
    private boolean modifyMobEffectBlocksSky(boolean original) {
        NoRender noRender = Modules.get().get(NoRender.class);
        if (noRender.noBlindness() || noRender.noDarkness()) return false;
        return original;
    }

    // Entity Shaders

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderLevelHead(CallbackInfo ci) {
        PostProcessShaders.beginRender();
    }

    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void onSubmitEntities(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector output, CallbackInfo ci) {
        if (renderDispatcher == null) {
            renderDispatcher = new FeatureRenderDispatcher(
                renderBuffers,
                mc.getModelManager(),
                mc.getAtlasManager(),
                mc.font,
                mc.gameRenderer.gameRenderState()
            );
        }

        draw(levelRenderState, poseStack, PostProcessShaders.CHAMS, _ -> Color.WHITE);
        draw(levelRenderState, poseStack, PostProcessShaders.ENTITY_OUTLINE, entity -> Modules.get().get(ESP.class).getColor(entity));
    }

    @Unique
    private void draw(LevelRenderState worldState, PoseStack matrices, EntityShader shader, Function<Entity, Color> colorGetter) {
        var camera = worldState.cameraRenderState.pos;
        var empty = true;

        for (var state : worldState.entityRenderStates) {
            Entity entity = ((IEntityRenderState) state).client33$getEntity();
            if (entity == null) continue;

            if (!shader.shouldDraw(entity)) continue;

            var color = colorGetter.apply(entity);
            if (color == null) continue;
            outlineRenderCommandQueue.setColor(color);

            var renderer = entityRenderDispatcher.getRenderer(state);
            var offset = renderer.getRenderOffset(state);

            matrices.pushPose();
            matrices.translate(state.x - camera.x + offset.x, state.y - camera.y + offset.y, state.z - camera.z + offset.z);
            renderer.submit(state, matrices, outlineRenderCommandQueue, worldState.cameraRenderState);
            matrices.popPose();

            empty = false;
        }

        if (empty)
            return;

        client33$pushEntityOutlineFramebuffer(shader.framebuffer);
        try {
            try (var frame = renderDispatcher.prepareFrame(outlineRenderCommandQueue)) {
                // JOML's no-argument Vector4f has w=1, which makes an opaque full-screen mask.
                try (var pass = com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder()
                    .createRenderPass(() -> "Client33 entity mask", shader.framebuffer.getColorTextureView(),
                        java.util.Optional.of(new org.joml.Vector4f(0.0f)), null, java.util.OptionalDouble.empty())) {
                    com.mojang.blaze3d.systems.RenderSystem.bindDefaultUniforms(pass);
                    frame.executeOutline(pass);
                }
            }
        } finally {
            outlineRenderCommandQueue.submitsPerOrder.clear();
            client33$popEntityOutlineFramebuffer();
        }
    }

    @Inject(method = "executeOutline", at = @At("TAIL"))
    private void addMainPass$submitEntityVertices(CallbackInfo ci) {
        PostProcessShaders.submitEntityVertices();
    }

    @Inject(method = "resize", at = @At("HEAD"))
    private void onResize(int width, int height, CallbackInfo ci) {
        PostProcessShaders.onResized(width, height);
    }

    // ILevelRenderer

    @Shadow
    @Final
    @Mutable
    private RenderTarget entityOutlineTarget;

    @Shadow
    @Final
    private LevelTargetBundle targets;

    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;
    @Shadow
    @Final
    private RenderBuffers renderBuffers;
    @Unique
    private Stack<RenderTarget> framebufferStack;

    @Unique
    private Stack<ResourceHandle<RenderTarget>> framebufferHandleStack;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init$IWorldRenderer(CallbackInfo ci) {
        framebufferStack = new ObjectArrayList<>();
        framebufferHandleStack = new ObjectArrayList<>();
    }

    @Override
    public void client33$pushEntityOutlineFramebuffer(RenderTarget framebuffer) {
        framebufferStack.push(this.entityOutlineTarget);
        this.entityOutlineTarget = framebuffer;

        framebufferHandleStack.push(this.targets.entityOutline);
        this.targets.entityOutline = () -> framebuffer;
    }

    @Override
    public void client33$popEntityOutlineFramebuffer() {
        this.entityOutlineTarget = framebufferStack.pop();
        this.targets.entityOutline = framebufferHandleStack.pop();
    }
}
