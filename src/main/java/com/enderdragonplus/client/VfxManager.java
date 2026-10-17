package com.enderdragonplus.client;

import com.enderdragonplus.EdpConfig;
import org.joml.Matrix4f;
import com.enderdragonplus.client.vfx.BarrageVfx;
import com.enderdragonplus.client.vfx.BreathBeamVfx;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import com.mojang.blaze3d.vertex.PoseStack;

public final class VfxManager {
    private static final List<VfxInstance> INSTANCES = new ArrayList<>();
    private static final List<VfxInstance> PENDING = new ArrayList<>();
    private static final List<VfxInstance> VISIBLE = new ArrayList<>();
    private static ClientLevel lastLevel;
    private static final Matrix4f worldView = new Matrix4f();
    private static boolean worldViewReady;

    private VfxManager() {
    }

    public static void add(VfxInstance instance) {
        PENDING.add(instance);
    }

    public static void clear() {
        worldViewReady = false;
        VISIBLE.clear();
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
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            worldView.set(event.getPoseStack().last().pose());
            worldViewReady = true;
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || INSTANCES.isEmpty() || !worldViewReady) {
            return;
        }
        float partialTick = event.getPartialTick();

        Draw.setup(event.getCamera());
        VISIBLE.clear();
        for (VfxInstance instance : INSTANCES) {
            if (!EdpConfig.OPTIMIZE_HEAVY_EFFECTS.get() || instance.visible(event.getFrustum())) {
                VISIBLE.add(instance);
            }
        }
        PoseStack stack = RenderSystem.getModelViewStack();
        stack.pushPose();
        stack.mulPoseMatrix(worldView);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        SceneCopy.invalidate();
        if (EdpConfig.ENABLE_DISTORTION.get()) {
            boolean any = false;
            for (VfxInstance instance : VISIBLE) {
                if (instance.distorts()) {
                    any = true;
                    break;
                }
            }
            if (any) {
                SceneCopy.capture();
                for (VfxInstance instance : VISIBLE) {
                    if (instance.distorts()) {
                        instance.renderDistortion(partialTick);
                    }
                }
            }
        }
        for (VfxInstance instance : VISIBLE) {
            instance.render(partialTick);
        }
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        stack.popPose();
        RenderSystem.applyModelViewMatrix();
    }
}
