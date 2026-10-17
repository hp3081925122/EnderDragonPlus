package com.enderdragonplus.client.showcase;

import com.enderdragonplus.EnderDragonPlus;
import com.enderdragonplus.debug.ShowcaseDirector;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = EnderDragonPlus.MODID, value = Dist.CLIENT)
public final class ShowcaseClient {
    private static final int CAPTION_TIME = 80;
    private static boolean active;
    private static int dragonId;
    @Nullable
    private static Vec3 camPos;
    @Nullable
    private static Vec3 lookPos;
    private static double groundY = 64.0;
    private static float angle;
    private static float distance = 28.0F;
    private static float height = 11.2F;
    private static float speed = 0.004F;
    private static int shot;
    private static String title = "";
    private static String subtitle = "";
    private static int captionColor = 0xFFFFFF;
    private static int captionAge = -1;
    private static boolean showCaption;

    private ShowcaseClient() {
    }

    public static void handle(int id, CompoundTag d) {
        int action = d.getInt("action");
        if (action == ShowcaseDirector.ACTION_START) {
            active = true;
            dragonId = id;
            camPos = null;
            lookPos = null;
            shot = ShowcaseDirector.SHOT_WIDE;
            captionAge = -1;
            EnderDragonPlus.LOGGER.info("Showcase client started for dragon {}", id);
        } else if (action == ShowcaseDirector.ACTION_STOP) {
            stop();
        } else if (action == ShowcaseDirector.ACTION_CAPTION) {
            title = d.getString("title");
            subtitle = d.getString("subtitle");
            captionColor = d.getInt("color");
            showCaption = d.getBoolean("show");
            shot = d.getInt("shot");
            captionAge = 0;
        }
    }

    private static void stop() {
        if (active) {
            EnderDragonPlus.LOGGER.info("Showcase client stopped");
        }
        active = false;
        captionAge = -1;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        stop();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!active || mc.level == null || mc.player == null) {
            return;
        }
        if (captionAge >= 0) {
            captionAge++;
            if (captionAge > CAPTION_TIME) {
                captionAge = -1;
            }
        }
        LocalPlayer player = mc.player;
        Entity entity = mc.level.getEntity(dragonId);
        if (!player.isSpectator() || !(entity instanceof EnderDragon dragon)) {
            return;
        }
        float targetDistance = 28.0F;
        float targetHeight = 11.2F;
        float targetSpeed = 0.004F;
        if (shot == ShowcaseDirector.SHOT_WIDE) {
            targetDistance = 44.8F;
            targetHeight = 21.0F;
            targetSpeed = 0.0025F;
        } else if (shot == ShowcaseDirector.SHOT_CLOSE) {
            targetDistance = 18.2F;
            targetHeight = 6.3F;
            targetSpeed = 0.007F;
        }
        distance = Mth.lerp(0.03F, distance, targetDistance);
        height = Mth.lerp(0.03F, height, targetHeight);
        speed = Mth.lerp(0.05F, speed, targetSpeed);
        ClientLevel level = mc.level;
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(dragon.getX()), Mth.floor(dragon.getZ()));
        if (ground > level.getMinBuildHeight() + 1) {
            groundY = Mth.lerp(0.1, groundY, ground);
        }
        Vec3 focus = new Vec3(dragon.getX(), Mth.lerp(0.45, groundY, dragon.getY() + 2.0), dragon.getZ());
        if (camPos == null) {
            camPos = player.position();
            lookPos = focus;
            angle = (float) Mth.atan2(player.getZ() - dragon.getZ(), player.getX() - dragon.getX());
        }
        angle += speed;
        Vec3 desired = focus.add(Mth.cos(angle) * distance, height, Mth.sin(angle) * distance);
        int floor = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(desired.x), Mth.floor(desired.z)) + 3;
        if (floor > level.getMinBuildHeight() + 4 && desired.y < floor) {
            desired = new Vec3(desired.x, floor, desired.z);
        }
        camPos = camPos.lerp(desired, 0.05);
        lookPos = lookPos.lerp(focus, 0.15);
        player.setPos(camPos.x, camPos.y, camPos.z);
        player.setDeltaMovement(Vec3.ZERO);
        Vec3 dir = lookPos.subtract(camPos.add(0.0, player.getEyeHeight(), 0.0));
        double horizontal = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float yaw = (float) (Mth.atan2(dir.z, dir.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) (-(Mth.atan2(dir.y, horizontal) * Mth.RAD_TO_DEG));
        player.setYRot(player.getYRot() + Mth.wrapDegrees(yaw - player.getYRot()));
        player.setXRot(Mth.clamp(pitch, -90.0F, 90.0F));
        player.setYHeadRot(player.getYRot());
    }

    @SubscribeEvent
    public static void onRenderLayer(RenderGuiLayerEvent.Pre event) {
        if (!active) {
            return;
        }
        if (event.getName().getNamespace().equals("minecraft") && !event.getName().equals(VanillaGuiLayers.BOSS_OVERLAY)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!active || !showCaption || captionAge < 0) {
            return;
        }
        float t = captionAge + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        float alpha = Math.min(Mth.clamp(t / 8.0F, 0.0F, 1.0F), Mth.clamp((CAPTION_TIME - t) / 15.0F, 0.0F, 1.0F));
        if (alpha < 0.03F) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int a = Mth.clamp((int) (alpha * 255.0F), 4, 255) << 24;
        float cx = g.guiWidth() * 0.5F;
        float cy = g.guiHeight() * 0.7F;
        float slide = (1.0F - Mth.clamp(t / 10.0F, 0.0F, 1.0F)) * 12.0F;
        g.pose().pushPose();
        g.pose().translate(cx, cy + slide, 0.0F);
        g.pose().scale(2.6F, 2.6F, 1.0F);
        g.drawString(font, title, -font.width(title) * 0.5F, -font.lineHeight, a | 0xFFFFFF, true);
        g.pose().popPose();
        int lineHalf = (int) (Mth.clamp(t / 12.0F, 0.0F, 1.0F) * 90.0F);
        g.fill((int) cx - lineHalf, (int) (cy + slide) + 4, (int) cx + lineHalf, (int) (cy + slide) + 5, a | captionColor);
        g.pose().pushPose();
        g.pose().translate(cx, cy + slide + 9.0F, 0.0F);
        g.pose().scale(1.2F, 1.2F, 1.0F);
        g.drawString(font, subtitle, -font.width(subtitle) * 0.5F, 0.0F, a | captionColor, true);
        g.pose().popPose();
    }
}
