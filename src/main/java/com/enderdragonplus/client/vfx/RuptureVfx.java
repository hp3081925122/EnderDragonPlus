package com.enderdragonplus.client.vfx;

import com.enderdragonplus.client.CameraShake;
import com.enderdragonplus.client.Draw;
import com.enderdragonplus.client.VfxInstance;
import com.enderdragonplus.client.VfxManager;
import com.enderdragonplus.dragon.EdpMath;
import net.minecraft.world.phys.Vec3;

public final class RuptureVfx extends VfxInstance {
    private static final int WARN = 14;
    private static final int ERUPT = 10;
    private final Vec3[] points;
    private final int[] delays;
    private final float radius;
    private final int color;

    public RuptureVfx(Vec3[] points, int[] delays, float radius, int color) {
        super(maxDelay(delays) + ERUPT + 2);
        this.points = points;
        this.delays = delays;
        this.radius = radius;
        this.color = color;
    }

    private static int maxDelay(int[] delays) {
        int max = 0;
        for (int d : delays) {
            max = Math.max(max, d);
        }
        return max;
    }

    @Override
    protected void onTick() {
        for (int i = 0; i < this.points.length; i++) {
            if (this.age != this.delays[i]) {
                continue;
            }
            Vec3 p = this.points[i];
            VfxManager.add(new ShockwaveVfx(p, this.radius * 1.8F, 10, this.color, false));
            VfxManager.add(new DecalVfx(p, this.radius * 1.4F, 60, this.color, true));
            if (i % 2 == 0) {
                CameraShake.addAt(p, 0.35F, 18.0F, 6);
            }
        }
    }

    @Override
    public void render(float partialTick) {
        float t = this.time(partialTick);
        if (Draw.begin(Draw.Material.GROUND)) {
            for (int i = 0; i < this.points.length; i++) {
                float local = t - (this.delays[i] - WARN);
                if (local < 0.0F || local >= WARN) {
                    continue;
                }
                Draw.ground(this.points[i], this.radius, this.color, Math.min(1.0F, local / 4.0F));
            }
            Draw.end(Draw.GROUND_WARNING, 0.5F, 0.0F, 0.0F);
        }
        int hot = Draw.mix(this.color, 0xFFFFFF, 0.5F);
        if (Draw.begin(Draw.Material.SPHERE)) {
            for (int i = 0; i < this.points.length; i++) {
                float local = t - this.delays[i];
                if (local < 0.0F || local >= ERUPT) {
                    continue;
                }
                float k = local / ERUPT;
                float h = 5.5F * EdpMath.easeOut(k * 2.0F);
                Vec3 p = this.points[i];
                Draw.cylinder(p, p.add(0.0, h, 0.0), this.radius * (0.9F - 0.4F * k), 16, this.color, 1.0F - k, 2.0F);
            }
            Draw.end(Draw.SPHERE_SHELL, 2.5F, 2.0F, 1.2F);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            for (int i = 0; i < this.points.length; i++) {
                float local = t - this.delays[i];
                if (local < 0.0F || local >= ERUPT) {
                    continue;
                }
                float k = local / ERUPT;
                float a = 1.0F - k;
                float h = 7.0F * EdpMath.easeOut(k * 2.0F);
                Vec3 p = this.points[i];
                Draw.beam(p, p.add(0.0, h, 0.0), 1.4F * a + 0.2F, hot, a, 2.0F);
            }
            Draw.end(3.0F, 1.0F, 2.0F, Draw.MODE_STRIP);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            for (int i = 0; i < this.points.length; i++) {
                float local = t - this.delays[i];
                if (local < 0.0F || local >= ERUPT) {
                    continue;
                }
                float a = 1.0F - local / ERUPT;
                Draw.billboard(this.points[i].add(0.0, 1.0, 0.0), this.radius * 2.5F * a + 1.0F, this.color, a);
            }
            Draw.end(0.5F, 2.0F, 2.0F, Draw.MODE_RADIAL);
        }
    }
}
