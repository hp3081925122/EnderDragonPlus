package com.enderdragonplus.mixin.client;

import com.enderdragonplus.EnderDragonPlus;
import com.enderdragonplus.client.DragonStageClient;
import com.enderdragonplus.client.TintedVertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonRenderer.class)
public abstract class EnderDragonRendererMixin {
    @Unique
    private static final String EDP_RENDER = "render(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V";
    @Unique
    private static final ResourceLocation EDP_GLOW_CRACKS = EnderDragonPlus.id("textures/entity/dragon_glow_cracks.png");
    @Unique
    private static final ResourceLocation EDP_GLOW_VEINS = EnderDragonPlus.id("textures/entity/dragon_glow_veins.png");
    @Unique
    private static final String EDP_MODEL_RENDER = "Lnet/minecraft/client/renderer/entity/EnderDragonRenderer$DragonModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFF)V";

    @Shadow
    @Final
    private EnderDragonRenderer.DragonModel model;

    @Unique
    private int edp$stage;

    @Inject(method = EDP_RENDER, at = @At("HEAD"))
    private void edp$captureStage(EnderDragon dragon, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        this.edp$stage = DragonStageClient.stage(dragon.getId());
    }

    @Redirect(method = EDP_RENDER, at = @At(value = "INVOKE", target = EDP_MODEL_RENDER, ordinal = 2))
    private void edp$tintBody(EnderDragonRenderer.DragonModel instance, PoseStack pose, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha) {
        int color = -1;
        if (this.edp$stage == 2) {
            color = 0xFFE2D2F2;
        } else if (this.edp$stage >= 3) {
            color = 0xFFC49AA6;
        }
        instance.renderToBuffer(pose, new TintedVertexConsumer(consumer, color), light, overlay, 1.0F, 1.0F, 1.0F, alpha);
    }

    @Redirect(method = EDP_RENDER, at = @At(value = "INVOKE", target = EDP_MODEL_RENDER, ordinal = 3))
    private void edp$tintEyes(EnderDragonRenderer.DragonModel instance, PoseStack pose, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha) {
        int color = -1;
        if (this.edp$stage == 2) {
            color = 0xFFFFA0FF;
        } else if (this.edp$stage >= 3) {
            color = 0xFFFF3A5A;
        }
        instance.renderToBuffer(pose, new TintedVertexConsumer(consumer, color), light, overlay, 1.0F, 1.0F, 1.0F, alpha);
        if (this.edp$stage >= 3) {
            instance.renderToBuffer(pose, new TintedVertexConsumer(consumer, color), light, overlay, 1.0F, 1.0F, 1.0F, alpha);
        }
    }

    @Inject(method = EDP_RENDER, at = @At(value = "INVOKE", target = EDP_MODEL_RENDER, ordinal = 3, shift = At.Shift.AFTER))
    private void edp$glowLayer(EnderDragon dragon, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        float glow = DragonStageClient.glow(dragon.getId());
        if (glow <= 0.01F) {
            return;
        }
        float t = dragon.tickCount + partialTick;
        int rgb = DragonStageClient.color(this.edp$stage);
        float rage = DragonStageClient.rage(dragon.getId());
        float cracks = glow * (0.85F + 0.15F * Mth.sin(t * 0.08F));
        if (rage > 0.0F) {
            cracks = glow * (0.75F + 0.25F * Mth.sin(t * 0.25F));
        }
        edp$renderColor(this.model, pose, buffers.getBuffer(RenderType.eyes(EDP_GLOW_CRACKS)), edp$scale(rgb, cracks));
        if (rage > 0.01F) {
            float veins = rage * (0.55F + 0.45F * Mth.sin(t * 0.25F + Mth.PI));
            edp$renderColor(this.model, pose, buffers.getBuffer(RenderType.eyes(EDP_GLOW_VEINS)), edp$scale(rgb, veins));
        }
    }

    @Unique
    private static void edp$renderColor(EnderDragonRenderer.DragonModel model, PoseStack pose, VertexConsumer consumer, int color) {
        model.renderToBuffer(pose, new TintedVertexConsumer(consumer, color), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Unique
    private static int edp$scale(int rgb, float intensity) {
        float k = Mth.clamp(intensity, 0.0F, 1.0F);
        return FastColor.ARGB32.color(255, (int) (((rgb >> 16) & 255) * k), (int) (((rgb >> 8) & 255) * k), (int) ((rgb & 255) * k));
    }
}
