package com.enderdragonplus.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DynamicUniformStorage;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class EdpGpu {
    private static final DynamicUniformStorage<Params> UNIFORMS = new DynamicUniformStorage<>("EDP uniforms", 48, 64);
    private static final java.util.List<GpuBuffer> VERTICES = new java.util.ArrayList<>();
    private static int nextBuffer;
    public static final Matrix4f MODEL_VIEW = new Matrix4f();
    private EdpGpu() {}
    public static void beginFrame() { UNIFORMS.endFrame(); nextBuffer = 0; }
    private record Params(Vector4f params, Vector4f tint, float width, float height, float time) implements DynamicUniformStorage.DynamicUniform {
        public void write(ByteBuffer buffer) { Std140Builder.intoBuffer(buffer).putVec4(params).putVec4(tint).putVec2(width, height).putFloat(time); }
    }
    public static void draw(MeshData mesh, int pipeline, Vector4f params, Vector4f tint) {
        if (mesh == null) return;
        var mc = Minecraft.getInstance();
        var target = mc.getMainRenderTarget();
        var color = RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride : target.getColorTextureView();
        var depth = RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : target.getDepthTextureView();
        float time = mc.level == null ? 0 : ((mc.level.getGameTime() % 24000L) + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false)) / 24000.0F;
        var custom = UNIFORMS.writeUniform(new Params(params, tint, color.texture().getWidth(0), color.texture().getHeight(0), time));
        var transform = RenderSystem.getDynamicUniforms().writeTransform(MODEL_VIEW, new Vector4f(1), new Vector3f(), new Matrix4f());
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        int size = mesh.vertexBuffer().remaining();
        if (nextBuffer == VERTICES.size()) VERTICES.add(null);
        GpuBuffer vertices = VERTICES.get(nextBuffer);
        if (vertices == null || vertices.size() < size) {
            if (vertices != null) vertices.close();
            vertices = RenderSystem.getDevice().createBuffer(() -> "EDP vertices", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, java.lang.Math.max(4096, Integer.highestOneBit(size - 1) << 1));
            VERTICES.set(nextBuffer, vertices);
        }
        nextBuffer++;
        encoder.writeToBuffer(vertices.slice(0, size), mesh.vertexBuffer());
        try (mesh) {
            var state = mesh.drawState();
            var indices = RenderSystem.getSequentialBuffer(state.mode());
            var indexBuffer = indices.getBuffer(state.indexCount());
            var noise = NoiseTexture.texture();
            try (var pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Ender Dragon Plus", color, OptionalInt.empty(), pipeline == 6 ? null : depth, OptionalDouble.empty())) {
                pass.setPipeline(EdpShaders.pipeline(pipeline));
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", transform);
                pass.setUniform("EdpUniforms", custom);
                if (pipeline >= 5) {
                    pass.bindTexture("Sampler0", SceneCopy.texture(), RenderSystem.getSamplerCache().getClampToEdge(com.mojang.blaze3d.textures.FilterMode.LINEAR));
                    if (pipeline == 5) pass.bindTexture("Sampler1", noise.getTextureView(), RenderSystem.getSamplerCache().getRepeat(com.mojang.blaze3d.textures.FilterMode.LINEAR));
                } else pass.bindTexture("Sampler0", noise.getTextureView(), RenderSystem.getSamplerCache().getRepeat(com.mojang.blaze3d.textures.FilterMode.LINEAR));
                pass.setVertexBuffer(0, vertices);
                pass.setIndexBuffer(indexBuffer, indices.type());
                pass.drawIndexed(0, 0, state.indexCount(), 1);
            }
        }
    }
}
