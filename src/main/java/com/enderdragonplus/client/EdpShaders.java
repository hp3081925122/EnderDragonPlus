package com.enderdragonplus.client;

import com.enderdragonplus.EnderDragonPlus;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

public final class EdpShaders {
    private static final RenderPipeline[] PIPELINES = new RenderPipeline[7];
    static {
        String[] fragments = {"energy", "ground", "ground", "sphere", "sphere", "distort", "screen"};
        for (int i = 0; i < fragments.length; i++) {
            boolean screen = i == 6;
            boolean normal = i >= 3 && !screen;
            var builder = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                .withLocation(EnderDragonPlus.id("pipeline/edp_" + i))
                .withVertexShader(EnderDragonPlus.id("core/edp_" + (screen ? "screen" : normal ? "normal" : "tex")))
                .withFragmentShader(EnderDragonPlus.id("core/edp_" + fragments[i]))
                .withVertexFormat(screen ? DefaultVertexFormat.POSITION_TEX : normal ? DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL : DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                .withUniform("EdpUniforms", UniformType.UNIFORM_BUFFER)
                .withSampler("Sampler0")
                .withDepthStencilState(screen ? java.util.Optional.empty() : java.util.Optional.of(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false)))
                .withCull(false);
            if (i == 5) builder.withSampler("Sampler1");
            if (!screen) builder.withColorTargetState(new ColorTargetState(i == 0 || i == 1 || i == 3 ? BlendFunction.LIGHTNING : BlendFunction.TRANSLUCENT));
            PIPELINES[i] = builder.build();
        }
    }
    private EdpShaders() {}
    public static void register(RegisterRenderPipelinesEvent event) {
        for (var pipeline : PIPELINES) event.registerPipeline(pipeline);
    }
    public static RenderPipeline pipeline(int index) { return PIPELINES[index]; }
}
