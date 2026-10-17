package com.enderdragonplus;

import com.enderdragonplus.client.ClientVfxHandler;
import com.enderdragonplus.dragon.DragonBrain;
import com.enderdragonplus.dragon.DragonBrainHolder;
import com.enderdragonplus.dragon.DragonPhases;
import com.enderdragonplus.item.EdpItems;
import com.enderdragonplus.network.VfxPayload;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import com.enderdragonplus.network.EdpNetwork;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;

@Mod(EnderDragonPlus.MODID)
public final class EnderDragonPlus {
    public static final String MODID = "enderdragonplus";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EnderDragonPlus() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        DragonPhases.init();
        EdpItems.ITEMS.register(modBus);
        modBus.addListener(EdpItems::addToTabs);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EdpConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, EdpConfig.CLIENT_SPEC);
        EdpNetwork.register();
        modBus.addListener(this::modifyAttributes);
        MinecraftForge.EVENT_BUS.addListener(this::onEntityJoin);
        MinecraftForge.EVENT_BUS.addListener(this::onStartTracking);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
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
            EdpNetwork.sendToPlayer(player, brain.stagePayload());
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
