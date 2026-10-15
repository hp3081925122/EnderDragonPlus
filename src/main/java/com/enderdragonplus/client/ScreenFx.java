package com.enderdragonplus.client;

import com.enderdragonplus.EdpConfig;
import com.mojang.blaze3d.vertex.BufferBuilder;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;



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

    public static void render() {
        if (!EdpConfig.ENABLE_SCREEN_EFFECTS.get()) {
            return;
        }
        if (flash < 0.005F && chromatic < 0.005F && vignette < 0.005F) {
            return;
        }
        SceneCopy.capture();
        float r = ((vignetteColor >> 16) & 255) / 255.0F;
        float g = ((vignetteColor >> 8) & 255) / 255.0F;
        float b = (vignetteColor & 255) / 255.0F;
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(-1, 1, 0).setUv(0, 1);
        buffer.addVertex(-1, -1, 0).setUv(0, 0);
        buffer.addVertex(1, -1, 0).setUv(1, 0);
        buffer.addVertex(1, 1, 0).setUv(1, 1);
        EdpGpu.draw(buffer.buildOrThrow(), 6, new org.joml.Vector4f(chromatic, flash, vignette, 0), new org.joml.Vector4f(r, g, b, 0.85F));
        SceneCopy.invalidate();
    }
}
