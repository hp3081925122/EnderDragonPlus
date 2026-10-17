package com.enderdragonplus.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class SceneCopy {
    private static TextureTarget target;
    private static boolean ready;

    private SceneCopy() {
    }

    public static void capture() {
        long edpCopy = DebugProfiler.begin();
        try {
            RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
            if (target == null) {
                target = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
                target.setFilterMode(GL11.GL_LINEAR);
            } else if (target.width != main.width || target.height != main.height) {
                target.resize(main.width, main.height, Minecraft.ON_OSX);
                target.setFilterMode(GL11.GL_LINEAR);
            }
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
            DebugProfiler.beforeBlit();
            GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, target.width, target.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            DebugProfiler.afterBlit();
            main.bindWrite(false);
            ready = true;
        } finally {
            DebugProfiler.copyEnd(edpCopy);
        }
    }

    public static boolean ready() {
        return ready && target != null;
    }

    public static void invalidate() {
        ready = false;
    }

    public static int textureId() {
        return target.getColorTextureId();
    }
}
