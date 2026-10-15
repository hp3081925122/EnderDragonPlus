package com.enderdragonplus.client;

import com.enderdragonplus.EnderDragonPlus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@EventBusSubscriber(modid = EnderDragonPlus.MODID, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterRenderPipelinesEvent event) {
        EdpShaders.register(event);
        event.registerPipeline(EdpDragonRenderTypes.EYES);
    }

    @SubscribeEvent
    public static void onRegisterRenderStateModifiers(net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(net.minecraft.client.renderer.entity.EnderDragonRenderer.class,
            (net.minecraft.world.entity.boss.enderdragon.EnderDragon entity, net.minecraft.client.renderer.entity.state.EnderDragonRenderState state) -> state.setRenderData(DragonVisualState.KEY, DragonVisualState.of(entity.getId())));
    }
}
