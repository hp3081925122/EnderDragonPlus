package com.enderdragonplus.client;

import com.enderdragonplus.EnderDragonPlus;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import java.io.IOException;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.client.event.RegisterShadersEvent;

public final class EdpShaders {
    @Nullable
    public static ShaderInstance energy;
    @Nullable
    public static ShaderInstance ground;
    @Nullable
    public static ShaderInstance sphere;
    @Nullable
    public static ShaderInstance distort;
    @Nullable
    public static ShaderInstance screen;

    private static boolean additive;

    public static void setAdditive(boolean value) {
        additive = value;
    }

    private static final class WorldShader extends ShaderInstance {
        private WorldShader(ResourceProvider provider, ResourceLocation id, VertexFormat format) throws IOException {
            super(provider, id, format);
        }

        @Override
        public void apply() {
            super.apply();
            RenderSystem.enableBlend();
            if (additive) {
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            } else {
                RenderSystem.defaultBlendFunc();
            }
        }
    }

    private EdpShaders() {
    }

    public static void register(RegisterShadersEvent event) {
        try {
            event.registerShader(new WorldShader(event.getResourceProvider(), EnderDragonPlus.id("edp_energy"), DefaultVertexFormat.POSITION_TEX_COLOR), s -> energy = s);
            event.registerShader(new WorldShader(event.getResourceProvider(), EnderDragonPlus.id("edp_ground"), DefaultVertexFormat.POSITION_TEX_COLOR), s -> ground = s);
            event.registerShader(new WorldShader(event.getResourceProvider(), EnderDragonPlus.id("edp_sphere"), DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL), s -> sphere = s);
            event.registerShader(new WorldShader(event.getResourceProvider(), EnderDragonPlus.id("edp_distort"), DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL), s -> distort = s);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), EnderDragonPlus.id("edp_screen"), DefaultVertexFormat.POSITION_TEX), s -> screen = s);
        } catch (IOException e) {
            EnderDragonPlus.LOGGER.error("Failed to load Ender Dragon Plus shaders", e);
        }
    }
}
