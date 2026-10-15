package com.enderdragonplus.network;

import com.enderdragonplus.EnderDragonPlus;
import com.enderdragonplus.client.ClientVfxHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class EdpNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(EnderDragonPlus.id("vfx"), () -> "1", "1"::equals, "1"::equals);

    private EdpNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(VfxPayload.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(VfxPayload::encode).decoder(VfxPayload::decode)
                .consumerMainThread((payload, context) -> ClientVfxHandler.handle(payload)).add();
    }

    public static void sendToPlayer(ServerPlayer player, VfxPayload payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    public static void sendToPlayersTrackingEntity(Entity entity, VfxPayload payload) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), payload);
    }

    public static void sendToPlayersNear(ServerLevel level, ServerPlayer excluded, double x, double y, double z, double radius, VfxPayload payload) {
        for (ServerPlayer player : level.players()) {
            if (player != excluded && player.distanceToSqr(x, y, z) < radius * radius) {
                sendToPlayer(player, payload);
            }
        }
    }
}
