package com.enderdragonplus.mixin.client;

import com.enderdragonplus.client.ScreenFx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void edp$beginFrame(net.minecraft.client.DeltaTracker tracker, boolean advanceGameTime, CallbackInfo ci) {
        com.enderdragonplus.client.EdpGpu.beginFrame();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"))
    private void edp$screenEffects(net.minecraft.client.DeltaTracker tracker, boolean advanceGameTime, CallbackInfo ci) {
        if (Minecraft.getInstance().level != null) ScreenFx.render();
    }
}
