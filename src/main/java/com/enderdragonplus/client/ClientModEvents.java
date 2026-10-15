package com.enderdragonplus.client;

import com.enderdragonplus.EnderDragonPlus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import com.enderdragonplus.item.EdpItems;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;

@EventBusSubscriber(modid = EnderDragonPlus.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        EdpShaders.register(event);
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> 0xFFFF5A9A, EdpItems.ENDING_CRYSTAL.get());
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiOverlaysEvent event) {
        event.registerBelowAll("screen_fx", (gui, graphics, partialTick, width, height) -> ScreenFx.render(graphics, partialTick));
    }
}
