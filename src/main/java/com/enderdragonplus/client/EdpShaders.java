package com.enderdragonplus.client;

import com.enderdragonplus.EnderDragonPlus;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

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

    private EdpShaders() {
    }

    public static void register(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), EnderDragonPlus.id("edp_energy"), DefaultVertexFormat.POSITION_TEX_COLOR), s -> energy = s);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), EnderDragonPlus.id("edp_ground"), DefaultVertexFormat.POSITION_TEX_COLOR), s -> ground = s);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), EnderDragonPlus.id("edp_sphere"), DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL), s -> sphere = s);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), EnderDragonPlus.id("edp_distort"), DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL), s -> distort = s);
            event.registerShader(new ShaderInstance(event.getResourceProvider(), EnderDragonPlus.id("edp_screen"), DefaultVertexFormat.POSITION_TEX), s -> screen = s);
        } catch (IOException e) {
            EnderDragonPlus.LOGGER.error("Failed to load Ender Dragon Plus shaders", e);
        }
    }
}
