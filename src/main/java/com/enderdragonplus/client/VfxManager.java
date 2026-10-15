package com.enderdragonplus.client;

import com.enderdragonplus.EdpConfig;
import com.enderdragonplus.client.vfx.BarrageVfx;
import com.enderdragonplus.client.vfx.BreathBeamVfx;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4fStack;

public final class VfxManager {
    private static final List<VfxInstance> INSTANCES = new ArrayList<>();
    private static final List<VfxInstance> PENDING = new ArrayList<>();
    private static ClientLevel lastLevel;

    private VfxManager() {
    }

    public static void add(VfxInstance instance) {
        PENDING.add(instance);
    }

    public static void clear() {
        INSTANCES.clear();
        PENDING.clear();
        BarrageVfx.clearAll();
        BreathBeamVfx.clearAll();
        DragonStageClient.clear();
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != lastLevel) {
            lastLevel = mc.level;
            clear();
            CameraShake.clear();
            ScreenFx.clear();
        }
        if (mc.level == null || mc.isPaused()) {
            return;
        }
        INSTANCES.addAll(PENDING);
        PENDING.clear();
        INSTANCES.removeIf(instance -> !instance.tick());
        INSTANCES.addAll(PENDING);
        PENDING.clear();
        CameraShake.tick();
        ScreenFx.tick();
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || INSTANCES.isEmpty()) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Draw.setup(event.getCamera());
        Matrix4fStack stack = RenderSystem.getModelViewStack();
        stack.pushMatrix();
        stack.mul(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        SceneCopy.invalidate();
        if (EdpConfig.ENABLE_DISTORTION.get()) {
            boolean any = false;
            for (VfxInstance instance : INSTANCES) {
                if (instance.distorts()) {
                    any = true;
                    break;
                }
            }
            if (any) {
                SceneCopy.capture();
                for (VfxInstance instance : INSTANCES) {
                    if (instance.distorts()) {
                        instance.renderDistortion(partialTick);
                    }
                }
            }
        }
        for (VfxInstance instance : INSTANCES) {
            instance.render(partialTick);
        }
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        stack.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }
}
