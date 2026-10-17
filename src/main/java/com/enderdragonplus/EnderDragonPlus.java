package com.enderdragonplus;

import com.enderdragonplus.client.ClientVfxHandler;
import com.enderdragonplus.dragon.DragonBrain;
import com.enderdragonplus.dragon.DragonBrainHolder;
import com.enderdragonplus.dragon.DragonPhases;
import com.enderdragonplus.item.EdpItems;
import com.enderdragonplus.network.VfxPayload;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod(EnderDragonPlus.MODID)
public final class EnderDragonPlus {
    public static final String MODID = "enderdragonplus";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EnderDragonPlus(IEventBus modBus, ModContainer container) {
        DragonPhases.init();
        EdpItems.ITEMS.register(modBus);
        modBus.addListener(EdpItems::addToTabs);
        container.registerConfig(ModConfig.Type.COMMON, EdpConfig.COMMON_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, EdpConfig.CLIENT_SPEC);
        modBus.addListener(this::registerPayloads);
        modBus.addListener(this::modifyAttributes);
        NeoForge.EVENT_BUS.addListener(this::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(this::onStartTracking);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(VfxPayload.TYPE, VfxPayload.STREAM_CODEC, (payload, context) -> ClientVfxHandler.handle(payload));
    }

    private void modifyAttributes(EntityAttributeModificationEvent event) {
        if (!event.has(EntityType.ENDER_DRAGON, Attributes.ATTACK_DAMAGE)) {
            event.add(EntityType.ENDER_DRAGON, Attributes.ATTACK_DAMAGE, 10.0);
        }
    }

    private void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof EnderDragon dragon) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DragonBrain brain = ((DragonBrainHolder) dragon).edp$getBrain();
        if (brain.active()) {
            PacketDistributor.sendToPlayer(player, brain.stagePayload());
        }
    }

    private void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof EnderDragon dragon)) {
            return;
        }
        if (!((DragonBrainHolder) dragon).edp$getBrain().active()) {
            return;
        }
        AttributeInstance attack = dragon.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(EdpConfig.ATTACK_DAMAGE.get());
        }
        AttributeInstance health = dragon.getAttribute(Attributes.MAX_HEALTH);
        double configured = EdpConfig.MAX_HEALTH.get();
        if (health != null && health.getBaseValue() != configured) {
            float ratio = dragon.getHealth() / dragon.getMaxHealth();
            health.setBaseValue(configured);
            if (event.loadedFromDisk()) {
                dragon.setHealth(ratio * dragon.getMaxHealth());
            } else {
                dragon.setHealth(dragon.getMaxHealth());
            }
        }
    }
}
