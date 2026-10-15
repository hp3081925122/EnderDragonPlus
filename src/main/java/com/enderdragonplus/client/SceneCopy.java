package com.enderdragonplus.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;

public final class SceneCopy {
    private static TextureTarget target;
    private static boolean ready;
    private SceneCopy() {}
    public static void capture() {
        var main = Minecraft.getInstance().getMainRenderTarget();
        var source = RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride.texture() : main.getColorTexture();
        int width = source.getWidth(0), height = source.getHeight(0);
        if (target == null) target = new TextureTarget("EDP scene copy", width, height, false);
        else if (target.width != width || target.height != height) target.resize(width, height);
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(source, target.getColorTexture(), 0, 0, 0, 0, 0, width, height);
        ready = true;
    }
    public static boolean ready() { return ready && target != null; }
    public static void invalidate() { ready = false; }
    public static GpuTextureView texture() { return target.getColorTextureView(); }
}
