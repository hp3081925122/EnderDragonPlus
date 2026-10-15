package com.enderdragonplus.client;

import com.enderdragonplus.EdpConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ViewportEvent;

public final class CameraShake {
    private static final List<float[]> SHAKES = new ArrayList<>();
    private static int clock;

    private CameraShake() {
    }

    public static void add(float intensity, int duration) {
        if (intensity <= 0.001F || duration <= 0) {
            return;
        }
        SHAKES.add(new float[]{intensity, duration, 0.0F});
    }

    public static void addAt(Vec3 pos, float intensity, float range, int duration) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        double dist = Minecraft.getInstance().player.position().distanceTo(pos);
        float f = 1.0F - (float) (dist / range);
        if (f > 0.0F) {
            add(intensity * f, duration);
        }
    }

    public static void clear() {
        SHAKES.clear();
    }

    public static void tick() {
        clock++;
        SHAKES.removeIf(s -> ++s[2] >= s[1]);
    }

    public static void apply(ViewportEvent.ComputeCameraAngles event) {
        if (SHAKES.isEmpty()) {
            return;
        }
        float partialTick = (float) event.getPartialTick();
        float total = 0.0F;
        for (float[] s : SHAKES) {
            float k = 1.0F - Mth.clamp((s[2] + partialTick) / s[1], 0.0F, 1.0F);
            total += s[0] * k * k;
        }
        total = Math.min(total, 4.0F) * EdpConfig.CAMERA_SHAKE.get().floatValue();
        if (total <= 0.001F) {
            return;
        }
        float t = clock + partialTick;
        float yaw = (Mth.sin(t * 2.1F) + Mth.sin(t * 3.7F + 1.3F) * 0.6F) * total;
        float pitch = (Mth.sin(t * 2.7F + 0.7F) + Mth.sin(t * 4.3F + 2.1F) * 0.5F) * total;
        float roll = Mth.sin(t * 1.9F + 3.1F) * total * 0.6F;
        event.setYaw(event.getYaw() + yaw);
        event.setPitch(event.getPitch() + pitch);
        event.setRoll(event.getRoll() + roll);
    }
}
