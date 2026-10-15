package com.enderdragonplus.mixin.client;

import com.enderdragonplus.EnderDragonPlus;
import com.enderdragonplus.client.DragonStageClient;
import com.enderdragonplus.client.DragonVisualState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.monster.dragon.EnderDragonModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnderDragonRenderer.class)
public abstract class EnderDragonRendererMixin {
    @Unique private static final Identifier EDP_GLOW_CRACKS = EnderDragonPlus.id("textures/entity/dragon_glow_cracks.png");
    @Unique private static final Identifier EDP_GLOW_VEINS = EnderDragonPlus.id("textures/entity/dragon_glow_veins.png");
    @Shadow @Final private EnderDragonModel model;

    @Redirect(method = "submit(Lnet/minecraft/client/renderer/entity/state/EnderDragonRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/resources/Identifier;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
    private void edp$tintBody(SubmitNodeCollector collector, Model<EnderDragonRenderState> model, Object rawState, PoseStack pose, Identifier texture, int light, int overlay, int outline, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        EnderDragonRenderState state = (EnderDragonRenderState) rawState;
        int stage = state.getRenderDataOrDefault(DragonVisualState.KEY, DragonVisualState.NONE).stage();
        int color = stage >= 3 ? 0xFFC49AA6 : stage == 2 ? 0xFFE2D2F2 : -1;
        collector.submitModel(model, state, pose, model.renderType(texture), light, overlay, color, null, outline, crumbling);
    }

    @Redirect(method = "submit(Lnet/minecraft/client/renderer/entity/state/EnderDragonRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
    private void edp$tintEyes(SubmitNodeCollector collector, Model<EnderDragonRenderState> model, Object rawState, PoseStack pose, RenderType type, int light, int overlay, int outline, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        EnderDragonRenderState state = (EnderDragonRenderState) rawState;
        type = com.enderdragonplus.client.EdpDragonRenderTypes.eyes(Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon_eyes.png"));
        DragonVisualState visual = state.getRenderDataOrDefault(DragonVisualState.KEY, DragonVisualState.NONE);
        int stage = visual.stage();
        int color = stage >= 3 ? 0xFFFF3A5A : stage == 2 ? 0xFFFFA0FF : -1;
        collector.submitModel(model, state, pose, type, light, overlay, color, null, outline, crumbling);
        if (stage >= 3) collector.submitModel(model, state, pose, type, light, overlay, color, null, outline, crumbling);
        float glow = visual.glow();
        if (glow <= 0.01F) return;
        float t = state.ageInTicks;
        int rgb = DragonStageClient.color(stage);
        float rage = visual.rage();
        float cracks = glow * (rage > 0 ? 0.75F + 0.25F * Mth.sin(t * 0.25F) : 0.85F + 0.15F * Mth.sin(t * 0.08F));
        collector.submitModel(this.model, state, pose, com.enderdragonplus.client.EdpDragonRenderTypes.eyes(EDP_GLOW_CRACKS), 15728880, OverlayTexture.NO_OVERLAY, edp$scale(rgb, cracks), null, outline, null);
        if (rage > 0.01F) {
            float veins = rage * (0.55F + 0.45F * Mth.sin(t * 0.25F + Mth.PI));
            collector.submitModel(this.model, state, pose, com.enderdragonplus.client.EdpDragonRenderTypes.eyes(EDP_GLOW_VEINS), 15728880, OverlayTexture.NO_OVERLAY, edp$scale(rgb, veins), null, outline, null);
        }
    }
    @Unique private static int edp$scale(int rgb, float intensity) {
        float k = Mth.clamp(intensity, 0, 1);
        return ARGB.color(255, (int) (((rgb >> 16) & 255) * k), (int) (((rgb >> 8) & 255) * k), (int) ((rgb & 255) * k));
    }
}
