package com.enderdragonplus.client;

import com.enderdragonplus.EdpConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

public final class ScreenFx {
    private static float flash;
    private static float chromatic;
    private static float vignette;
    private static int vignetteColor = 0x400060;
    private static float holdChromatic;
    private static float holdVignette;

    private ScreenFx() {
    }

    public static void flash(float amount) {
        flash = Math.max(flash, amount);
    }

    public static void chromatic(float amount) {
        chromatic = Math.max(chromatic, amount);
    }

    public static void sustain(float chromaticAmount, float vignetteAmount, int color) {
        holdChromatic = Math.max(holdChromatic, chromaticAmount);
        if (vignetteAmount > holdVignette) {
            holdVignette = vignetteAmount;
            vignetteColor = color;
        }
    }

    public static void clear() {
        flash = 0.0F;
        chromatic = 0.0F;
        vignette = 0.0F;
        holdChromatic = 0.0F;
        holdVignette = 0.0F;
    }

    public static void tick() {
        flash = Math.max(0.0F, flash * 0.82F - 0.01F);
        chromatic = Math.max(holdChromatic, chromatic * 0.88F);
        vignette = Math.max(holdVignette, vignette * 0.9F);
        holdChromatic = 0.0F;
        holdVignette = 0.0F;
    }

    public static void render(GuiGraphics graphics, float partialTick) {
        if (!EdpConfig.ENABLE_SCREEN_EFFECTS.get()) {
            return;
        }
        if (flash < 0.005F && chromatic < 0.005F && vignette < 0.005F) {
            return;
        }
        ShaderInstance shader = EdpShaders.screen;
        if (shader == null) {
            return;
        }
        graphics.flush();
        ShaderInstance previousShader = RenderSystem.getShader();
        int previousTexture = RenderSystem.getShaderTexture(0);
        float[] previousColor = RenderSystem.getShaderColor().clone();
        SceneCopy.capture();

        shader.safeGetUniform("EdpParams").set(chromatic, flash, vignette, 0.0F);
        float r = ((vignetteColor >> 16) & 255) / 255.0F;
        float g = ((vignetteColor >> 8) & 255) / 255.0F;
        float b = (vignetteColor & 255) / 255.0F;
        shader.safeGetUniform("EdpTint").set(r, g, b, 0.85F);
        try {
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, SceneCopy.textureId());
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            Matrix4f matrix = graphics.pose().last().pose();
            float w = graphics.guiWidth();
            float h = graphics.guiHeight();
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            buffer.vertex(matrix, 0.0F, 0.0F, 0.0F).uv(0.0F, 1.0F).endVertex();
            buffer.vertex(matrix, 0.0F, h, 0.0F).uv(0.0F, 0.0F).endVertex();
            buffer.vertex(matrix, w, h, 0.0F).uv(1.0F, 0.0F).endVertex();
            buffer.vertex(matrix, w, 0.0F, 0.0F).uv(1.0F, 1.0F).endVertex();
            BufferUploader.drawWithShader(buffer.end());
        } finally {
            RenderSystem.setShader(() -> previousShader);
            RenderSystem.setShaderTexture(0, previousTexture);
            RenderSystem.setShaderColor(previousColor[0], previousColor[1], previousColor[2], previousColor[3]);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            SceneCopy.invalidate();
        }
    }
}
