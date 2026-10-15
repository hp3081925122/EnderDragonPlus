package com.enderdragonplus.client;

import com.enderdragonplus.EnderDragonPlus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = EnderDragonPlus.MODID, value = Dist.CLIENT)
public final class ClientGameEvents {
    private ClientGameEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        VfxManager.tick();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        VfxManager.render(event);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        CameraShake.apply(event);
    }
}
