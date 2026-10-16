package com.enderdragonplus.client;

import com.enderdragonplus.EnderDragonPlus;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.Function;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public final class EdpDragonRenderTypes {
    public static final RenderPipeline EYES = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
        .withLocation(EnderDragonPlus.id("pipeline/dragon_eyes"))
        .withVertexShader("core/entity").withFragmentShader(EnderDragonPlus.id("core/edp_dragon_glow"))
        .withShaderDefine("EMISSIVE").withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING")
        .withSampler("Sampler0").withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE))
        .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
        .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false)).build();
    private static final Function<Identifier, RenderType> TYPES = Util.memoize(texture -> RenderType.create("edp_dragon_eyes", RenderSetup.builder(EYES).withTexture("Sampler0", texture).sortOnUpload().createRenderSetup()));
    private EdpDragonRenderTypes() {}
    public static RenderType eyes(Identifier texture) { return TYPES.apply(texture); }
}
