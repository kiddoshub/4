
package com.gaspoweredcake.client33.renderer;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.gaspoweredcake.client33.mixininterface.IRenderPipeline;
import org.jspecify.annotations.NonNull;

public class ExtendedRenderPipelineBuilder extends RenderPipeline.Builder {
    private boolean lineSmooth;

    public ExtendedRenderPipelineBuilder(RenderPipeline.Snippet... snippets) {
        for (RenderPipeline.Snippet snippet : snippets) {
            withSnippet(snippet);
        }
    }

    public ExtendedRenderPipelineBuilder withLineSmooth() {
        lineSmooth = true;
        return this;
    }

    @Override
    public @NonNull RenderPipeline build() {
        RenderPipeline pipeline = super.build();
        ((IRenderPipeline) pipeline).client33$setLineSmooth(lineSmooth);
        LineSmoothing.register(pipeline.getLocation().toString(), lineSmooth);

        return pipeline;
    }
}
