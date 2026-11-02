
package com.gaspoweredcake.client33.utils.render.postprocess;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.events.game.ResourcePacksReloadedEvent;
import com.gaspoweredcake.client33.renderer.MeshRenderer;
import com.gaspoweredcake.client33.renderer.Client33RenderPipelines;
import com.gaspoweredcake.client33.renderer.Texture;
import com.gaspoweredcake.client33.utils.render.UploadedMaterial;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.render.Chams;
import com.gaspoweredcake.client33.utils.PostInit;
import com.gaspoweredcake.client33.utils.render.color.Color;
import com.gaspoweredcake.client33.utils.render.AnimationPhase;
import meteordevelopment.orbit.EventHandler;
import com.gaspoweredcake.client33.renderer.DynamicUniformStorage;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.NonNull;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Optional;

import static com.gaspoweredcake.client33.Client33.mc;

public class ChamsShader extends EntityShader {
    private static final String[] FILE_FORMATS = {"png", "jpg"};

    private static Texture IMAGE_TEX;
    private static Texture WHITE_IMAGE;
    private static Chams chams;
    private final AnimationPhase fractureAnimation = new AnimationPhase(System.nanoTime());

    public ChamsShader() {
        super(Client33RenderPipelines.POST_IMAGE);
        Client33.EVENT_BUS.subscribe(ChamsShader.class);
    }

    @PostInit
    public static void load() {
        try {
            ByteBuffer data = null;
            for (String fileFormat : FILE_FORMATS) {
                Optional<Resource> optional = mc.getResourceManager().getResource(Client33.identifier("textures/chams." + fileFormat));
                if (optional.isEmpty() || optional.get().open() == null) {
                    continue;
                }

                data = TextureUtil.readResource(optional.get().open());
                break;
            }
            if (data == null) return;

            data.rewind();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer comp = stack.mallocInt(1);

                STBImage.stbi_set_flip_vertically_on_load(true);
                ByteBuffer image = STBImage.stbi_load_from_memory(data, width, height, comp, 4);

                IMAGE_TEX = new Texture(width.get(0), height.get(0), GpuFormat.RGBA8_UNORM, FilterMode.NEAREST, FilterMode.NEAREST);
                IMAGE_TEX.upload(image);

                STBImage.stbi_image_free(image);
                STBImage.stbi_set_flip_vertically_on_load(false);
            }
        } catch (IOException e) {
            Client33.LOG.error("Error loading the chams shader", e);
        }
    }

    @EventHandler
    private static void onResourcePacksReloaded(ResourcePacksReloadedEvent event) {
        load();
    }

    @Override
    protected RenderPipeline pipeline() {
        return chams.shader.get() == Chams.Shader.Fracture ? Client33RenderPipelines.POST_FRACTURE : super.pipeline();
    }

    @Override
    protected void setupPass(MeshRenderer renderer) {
        Texture uploaded = UploadedMaterial.texture(System.nanoTime(), chams.imageAnimate.get(), chams.imageSpeed.get());
        Texture material = uploaded != null ? uploaded : IMAGE_TEX;
        if (material == null) {
            if (WHITE_IMAGE == null) {
                WHITE_IMAGE = new Texture(1, 1, GpuFormat.RGBA8_UNORM, FilterMode.LINEAR, FilterMode.LINEAR);
                WHITE_IMAGE.upload(new byte[] {-1, -1, -1, -1});
            }
            material = WHITE_IMAGE;
        }
        float viewportAspect = (float) mc.getWindow().getWidth() / Math.max(1, mc.getWindow().getHeight());
        float materialAspect = uploaded == null ? viewportAspect : (float) material.getWidth() / material.getHeight();
        if (chams.shader.get() == Chams.Shader.Fracture) {
            Color base = chams.fractureColor.get();
            Color seam = chams.fractureSeam.get();
            Color accent = chams.fractureAccent.get();
            renderer.uniform("FractureData", FRACTURE_STORAGE.writeUniform(new FractureUniforms(
                base.r / 255f, base.g / 255f, base.b / 255f, base.a / 255f,
                seam.r / 255f, seam.g / 255f, seam.b / 255f, seam.a / 255f,
                chams.fractureScale.get().floatValue(), chams.fractureWidth.get().floatValue(),
                chams.fractureGlow.get().floatValue(),
                fractureAnimation.sample(System.nanoTime(), chams.fractureSpeed.get()),
                chams.fractureDepth.get().floatValue(), chams.fractureMotion.get().floatValue(),
                chams.fractureFlow.get().floatValue(), chams.fracturePattern.get().ordinal(),
                chams.fractureDensity.get().floatValue(), chams.fractureRoughness.get().floatValue(),
                (float) Math.toRadians(chams.fractureLightAngle.get()), chams.fractureTonalVariation.get().floatValue(),
                chams.fractureEngraving.get().floatValue(), chams.fractureEngravingSpacing.get().floatValue(),
                chams.fractureInkContrast.get().floatValue(), chams.fracturePaperWarmth.get().floatValue(),
                accent.r / 255f, accent.g / 255f, accent.b / 255f, chams.fracturePalette.get().ordinal(),
                uploaded == null ? 0 : chams.imageBlend.get().floatValue(), chams.imageScale.get().floatValue(), materialAspect, viewportAspect)));
            renderer.sampler("u_Material", material.getTextureView(), material.getSampler());
            return;
        }
        Color color = chams.shaderColor.get();

        renderer.uniform("ImageData", UNIFORM_STORAGE.writeUniform(new UniformData(
            color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f,
            chams.imageScale.get().floatValue(), materialAspect, viewportAspect
        )));

        renderer.sampler("u_TextureI", material.getTextureView(), material.getSampler());
    }

    @Override
    protected boolean shouldDraw() {
        if (chams == null) chams = Modules.get().get(Chams.class);
        return chams.isShader();
    }

    @Override
    public boolean shouldDraw(Entity entity) {
        if (!shouldDraw()) return false;
        return chams.entities.get().contains(entity.getType()) && (entity != mc.player || !chams.ignoreSelfDepth.get());
    }

    // Uniforms

    private static final int UNIFORM_SIZE = new Std140SizeCalculator()
        .putVec4().putVec4()
        .get();

    private static final DynamicUniformStorage<UniformData> UNIFORM_STORAGE = new DynamicUniformStorage<>("Client33 - Image UBO", UNIFORM_SIZE, 16);

    private static final int FRACTURE_SIZE = new Std140SizeCalculator().putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().get();
    private static final DynamicUniformStorage<FractureUniforms> FRACTURE_STORAGE = new DynamicUniformStorage<>("Client33 - Fracture UBO", FRACTURE_SIZE, 16);

    public static void flipFrame() {
        UNIFORM_STORAGE.endFrame();
        FRACTURE_STORAGE.endFrame();
    }

    private record FractureUniforms(float r, float g, float b, float a, float sr, float sg, float sb, float sa,
                                    float size, float width, float glow, float speed, float depth, float motion, float flow, float pattern,
                                    float density, float roughness, float lightAngle, float tonalVariation,
                                    float engraving, float engravingSpacing, float inkContrast, float paperWarmth,
                                    float ar, float ag, float ab, float palette,
                                    float imageBlend, float imageScale, float imageAspect, float viewportAspect) implements DynamicUniformStorage.DynamicUniform {
        @Override
        public void write(@NonNull ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer).putVec4(r, g, b, a).putVec4(sr, sg, sb, sa).putVec4(size, width, glow, speed).putVec4(depth, motion, flow, pattern).putVec4(density, roughness, lightAngle, tonalVariation).putVec4(engraving, engravingSpacing, inkContrast, paperWarmth).putVec4(ar, ag, ab, palette).putVec4(imageBlend, imageScale, imageAspect, viewportAspect);
        }
    }

    private record UniformData(float r, float g, float b, float a, float scale, float imageAspect, float viewportAspect) implements DynamicUniformStorage.DynamicUniform {
        @Override
        public void write(@NonNull ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec4(r, g, b, a).putVec4(scale, imageAspect, viewportAspect, 0);
        }
    }
}
