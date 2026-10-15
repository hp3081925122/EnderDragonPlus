package com.enderdragonplus.client.vfx;

import com.enderdragonplus.client.CameraShake;
import com.enderdragonplus.client.Draw;
import com.enderdragonplus.client.VfxInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class RingWavesVfx extends VfxInstance {
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
    private static final int SEGMENTS = 96;
    private final Vec3 center;
    private final int count;
    private final int interval;
    private final float speed;
    private final float maxRadius;
    private final int color;

    public RingWavesVfx(Vec3 center, int count, int interval, float speed, float maxRadius, int color) {
        super(interval * Math.max(0, count - 1) + Mth.ceil(maxRadius / Math.max(0.05F, speed)) + 4);
        this.center = center;
        this.count = count;
        this.interval = interval;
        this.speed = speed;
        this.maxRadius = maxRadius;
        this.color = color;
    }

    @Override
    protected void onTick() {
        for (int i = 1; i < this.count; i++) {
            if (this.age == i * this.interval) {
                CameraShake.addAt(this.center, 1.0F, 50.0F, 10);
            }
        }
    }

    private float radius(int index, float t) {
        return (t - index * this.interval) * this.speed;
    }

    private float alpha(float r) {
        return Mth.clamp((this.maxRadius - r) / 8.0F, 0.0F, 1.0F) * Mth.clamp(r / 2.0F, 0.0F, 1.0F);
    }

    @Override
    public boolean distorts() {
        return true;
    }

    @Override
    public void renderDistortion(float partialTick) {
        float t = this.time(partialTick);
        if (Draw.begin(Draw.Material.DISTORT)) {
            for (int i = 0; i < this.count; i++) {
                float r = this.radius(i, t);
                if (r <= 0.0F || r > this.maxRadius) {
                    continue;
                }
                float a = this.alpha(r);
                Draw.cylinder(this.center, this.center.add(0.0, 1.6, 0.0), r, SEGMENTS, this.color, a, 1.0F);
            }
            Draw.end(0.03F, Draw.DISTORT_BAND, 2.0F, 0.5F);
        }
    }

    @Override
    public void render(float partialTick) {
        float t = this.time(partialTick);
        int hot = Draw.mix(this.color, 0xFFFFFF, 0.45F);
        Vec3 base = this.center.add(0.0, 0.2, 0.0);
        if (Draw.begin(Draw.Material.ENERGY)) {
            for (int i = 0; i < this.count; i++) {
                float r = this.radius(i, t);
                if (r <= 0.0F || r > this.maxRadius) {
                    continue;
                }
                float a = this.alpha(r);
                Draw.ring(base, UP, Math.max(0.0F, r - 2.2F), r + 0.4F, SEGMENTS, this.color, 0.0F, a, r * 0.5F);
                Draw.ring(base.add(0.0, 0.05, 0.0), UP, Math.max(0.0F, r - 0.4F), r + 0.3F, SEGMENTS, hot, a, a, r * 0.5F);
            }
            Draw.end(1.6F, 3.0F, 1.5F, Draw.MODE_RING);
        }
        if (Draw.begin(Draw.Material.SPHERE)) {
            for (int i = 0; i < this.count; i++) {
                float r = this.radius(i, t);
                if (r <= 0.0F || r > this.maxRadius) {
                    continue;
                }
                Draw.cylinder(this.center, this.center.add(0.0, 1.1, 0.0), r, SEGMENTS, this.color, this.alpha(r) * 0.9F, r * 0.25F);
            }
            Draw.end(Draw.SPHERE_SHELL, 2.0F, 2.5F, 1.0F);
        }
    }
}
