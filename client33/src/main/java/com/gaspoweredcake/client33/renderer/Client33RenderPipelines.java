
package com.gaspoweredcake.client33.renderer;

import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.gaspoweredcake.client33.Client33;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public abstract class Client33RenderPipelines {
    private static final List<RenderPipeline> PIPELINES = new ArrayList<>();

    // Snippets

    private static final BindGroupLayout MESH_BIND_GROUP = BindGroupLayout.builder()
        .withUniform("MeshData", UniformType.UNIFORM_BUFFER)
        .build();

    private static final RenderPipeline.Snippet MESH_UNIFORMS = RenderPipeline.builder()
        .withBindGroupLayout(MESH_BIND_GROUP)
        .buildSnippet();

    // World

    public static final RenderPipeline WORLD_COLORED = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLocation(Client33.identifier("pipeline/world_colored"))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(Client33.identifier("shaders/pos_color.vert"))
        .withFragmentShader(Client33.identifier("shaders/pos_color.frag"))
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build()
    );

    public static final RenderPipeline WORLD_COLORED_LINES = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLineSmooth()
        .withLocation(Client33.identifier("pipeline/world_colored_lines"))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.DEBUG_LINES)
        .withVertexShader(Client33.identifier("shaders/pos_color.vert"))
        .withFragmentShader(Client33.identifier("shaders/pos_color.frag"))
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build()
    );

    public static final RenderPipeline WORLD_COLORED_DEPTH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLocation(Client33.identifier("pipeline/world_colored_depth"))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(Client33.identifier("shaders/pos_color.vert"))
        .withFragmentShader(Client33.identifier("shaders/pos_color.frag"))
        .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build()
    );

    public static final RenderPipeline WORLD_COLORED_LINES_DEPTH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLineSmooth()
        .withLocation(Client33.identifier("pipeline/world_colored_lines_depth"))
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.DEBUG_LINES)
        .withVertexShader(Client33.identifier("shaders/pos_color.vert"))
        .withFragmentShader(Client33.identifier("shaders/pos_color.frag"))
        .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build()
    );

    // UI

    public static final RenderPipeline UI_COLORED = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withShaderDefine("POSITION_2D")
        .withLocation(Client33.identifier("pipeline/ui_colored"))
        .withVertexBinding(0, Client33VertexFormats.POS2_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(Client33.identifier("shaders/pos_color.vert"))
        .withFragmentShader(Client33.identifier("shaders/pos_color.frag"))
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(true)
        .build()
    );

    public static final RenderPipeline UI_COLORED_LINES = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withShaderDefine("POSITION_2D")
        .withLocation(Client33.identifier("pipeline/ui_colored_lines"))
        .withVertexBinding(0, Client33VertexFormats.POS2_COLOR).withPrimitiveTopology(PrimitiveTopology.DEBUG_LINES)
        .withVertexShader(Client33.identifier("shaders/pos_color.vert"))
        .withFragmentShader(Client33.identifier("shaders/pos_color.frag"))
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(true)
        .build()
    );

    public static final RenderPipeline UI_TEXTURED = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withShaderDefine("POSITION_2D")
        .withLocation(Client33.identifier("pipeline/ui_textured"))
        .withVertexBinding(0, Client33VertexFormats.POS2_TEXTURE_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(Client33.identifier("shaders/pos_tex_color.vert"))
        .withFragmentShader(Client33.identifier("shaders/pos_tex_color.frag"))
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("u_Texture", UniformType.COMBINED_IMAGE_SAMPLER).build())
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(true)
        .build()
    );

    public static final RenderPipeline UI_TEXT = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withShaderDefine("POSITION_2D")
        .withLocation(Client33.identifier("pipeline/ui_text"))
        .withVertexBinding(0, Client33VertexFormats.POS2_TEXTURE_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(Client33.identifier("shaders/text.vert"))
        .withFragmentShader(Client33.identifier("shaders/text.frag"))
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("u_Texture", UniformType.COMBINED_IMAGE_SAMPLER).build())
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(true)
        .build()
    );

    // Post Process

    public static final RenderPipeline POST_OUTLINE = add(new ExtendedRenderPipelineBuilder()
        .withLocation(Client33.identifier("pipeline/post/outline"))
        .withVertexBinding(0, Client33VertexFormats.POS2).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(Client33.identifier("shaders/post-process/base.vert"))
        .withFragmentShader(Client33.identifier("shaders/post-process/outline.frag"))
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("u_Texture", UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("PostData", UniformType.UNIFORM_BUFFER)
            .withUniform("OutlineData", UniformType.UNIFORM_BUFFER)
            .build())
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build()
    );

    public static final RenderPipeline POST_IMAGE = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLocation(Client33.identifier("pipeline/post/image"))
        .withVertexBinding(0, Client33VertexFormats.POS2).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(Client33.identifier("shaders/post-process/base.vert"))
        .withFragmentShader(Client33.identifier("shaders/post-process/image.frag"))
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("u_Texture", UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("u_TextureI", UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("PostData", UniformType.UNIFORM_BUFFER)
            .withUniform("ImageData", UniformType.UNIFORM_BUFFER)
            .build())
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build()
    );

    public static final RenderPipeline POST_FRACTURE = add(new ExtendedRenderPipelineBuilder()
        .withLocation(Client33.identifier("pipeline/post/fracture"))
        .withVertexBinding(0, Client33VertexFormats.POS2).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(Client33.identifier("shaders/post-process/base.vert"))
        .withFragmentShader(Client33.identifier("shaders/post-process/fracture.frag"))
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("u_Texture", UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("u_Material", UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("PostData", UniformType.UNIFORM_BUFFER)
            .withUniform("FractureData", UniformType.UNIFORM_BUFFER)
            .build())
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build()
    );

    // Blur

    public static final RenderPipeline BLUR_DOWN = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLocation(Client33.identifier("pipeline/blur/down"))
        .withVertexBinding(0, Client33VertexFormats.POS2).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(Client33.identifier("shaders/blur.vert"))
        .withFragmentShader(Client33.identifier("shaders/blur_down.frag"))
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("u_Texture", UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
            .build())
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build()
    );

    public static final RenderPipeline BLUR_UP = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLocation(Client33.identifier("pipeline/blur/up"))
        .withVertexBinding(0, Client33VertexFormats.POS2).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(Client33.identifier("shaders/blur.vert"))
        .withFragmentShader(Client33.identifier("shaders/blur_up.frag"))
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("u_Texture", UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
            .build())
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build()
    );

    public static final RenderPipeline BLUR_PASSTHROUGH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLocation(Client33.identifier("pipeline/blur/passthrough"))
        .withVertexBinding(0, Client33VertexFormats.POS2).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(Client33.identifier("shaders/passthrough.vert"))
        .withFragmentShader(Client33.identifier("shaders/passthrough.frag"))
        .withBindGroupLayout(BindGroupLayout.builder().withUniform("u_Texture", UniformType.COMBINED_IMAGE_SAMPLER).build())
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .build()
    );

    public static final RenderPipeline MASKED_GLASS = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
        .withLocation(Client33.identifier("pipeline/blur/masked_glass"))
        .withVertexBinding(0, Client33VertexFormats.POS2).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withVertexShader(Client33.identifier("shaders/passthrough.vert"))
        .withFragmentShader(Client33.identifier("shaders/masked_glass.frag"))
        .withBindGroupLayout(BindGroupLayout.builder()
            .withUniform("u_Texture", UniformType.COMBINED_IMAGE_SAMPLER)
            .withUniform("GlassData", UniformType.UNIFORM_BUFFER).build())
        .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false).build()
    );

    private static RenderPipeline add(RenderPipeline pipeline) {
        PIPELINES.add(pipeline);
        return pipeline;
    }

    public static void precompile(com.mojang.blaze3d.pipeline.PipelineCache cache) {
        GpuDevice device = RenderSystem.getDevice();
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        var source = new com.mojang.renderpearl.api.pipeline.ShaderSource() {
            @Override
            public String getShader(net.minecraft.resources.Identifier identifier,
                                    com.mojang.renderpearl.api.pipeline.ShaderType type) {
                try (var in = resources.getResourceOrThrow(identifier).open()) {
                    return IOUtils.toString(in, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new IllegalStateException("Cannot load Client33 shader " + identifier, e);
                }
            }

            @Override
            public CachedIncludeSource getInclude(net.minecraft.resources.Identifier identifier) { return null; }

            @Override
            public void close() {}
        };
        for (RenderPipeline pipeline : PIPELINES) {
            var compiled = device.compilePipeline(pipeline, source, net.minecraft.util.Util.backgroundExecutor())
                .join().finishCompile();
            if (compiled == null) throw new IllegalStateException("Client33 pipeline failed to compile: " + pipeline.getLocation());
            cache.insert(pipeline, compiled);
        }
    }

    private Client33RenderPipelines() {
    }
}
