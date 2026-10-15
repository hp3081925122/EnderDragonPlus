package com.enderdragonplus.client.vfx;

import com.enderdragonplus.client.Draw;
import com.enderdragonplus.client.VfxInstance;
import com.enderdragonplus.dragon.EdpMath;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;

public final class BreathChargeVfx extends VfxInstance {
    private static final int STREAKS = 14;
    private final int dragonId;
    private final int duration;
    private final int color;
    private final Vec3[] directions = new Vec3[STREAKS];

    public BreathChargeVfx(int dragonId, int duration, int color) {
        super(duration + 4);
        this.dragonId = dragonId;
        this.duration = duration;
        this.color = color;
        for (int i = 0; i < STREAKS; i++) {
            float y = 1.0F - (i + 0.5F) / STREAKS * 2.0F;
            float r = Mth.sqrt(1.0F - y * y);
            float a = i * 2.39996F;
            this.directions[i] = new Vec3(Mth.cos(a) * r, y, Mth.sin(a) * r);
        }
    }

    @Override
    public boolean distorts() {
        return true;
    }

    @Override
    public void renderDistortion(float partialTick) {
        EnderDragon dragon = dragon(this.dragonId);
        if (dragon == null) {
            return;
        }
        float k = Mth.clamp(this.time(partialTick) / this.duration, 0.0F, 1.0F);
        if (Draw.begin(Draw.Material.DISTORT)) {
            Draw.sphere(mouth(dragon, partialTick), 1.0F + 2.0F * k, 10, 16, this.color, k);
            Draw.end(0.03F, Draw.DISTORT_LENS, 1.0F, 0.0F);
        }
    }

    @Override
    public void render(float partialTick) {
        EnderDragon dragon = dragon(this.dragonId);
        if (dragon == null) {
            return;
        }
        Vec3 m = mouth(dragon, partialTick);
        float t = this.time(partialTick);
        float k = Mth.clamp(t / this.duration, 0.0F, 1.0F);
        float alpha = this.fade(partialTick, 4.0F, 4.0F);
        int hot = Draw.mix(this.color, 0xFFFFFF, 0.3F);
        if (Draw.begin(Draw.Material.ENERGY)) {
            for (int i = 0; i < STREAKS; i++) {
                float phase = (t * 0.07F + i * 0.137F) % 1.0F;
                double d = 7.0 * (1.0F - phase) + 0.6;
                Vec3 head = m.add(this.directions[i].scale(d));
                Vec3 tail = m.add(this.directions[i].scale(d + 1.2 + 1.5 * k));
                Draw.beam(head, tail, 0.18F + 0.12F * k, hot, alpha * phase, 1.0F);
            }
            Draw.end(1.5F, 1.0F, 2.5F, Draw.MODE_STRIP);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            Draw.ring(m, Draw.cameraPos().subtract(m), 0.8F + 3.5F * (1.0F - k), 1.4F + 3.5F * (1.0F - k), 40, this.color, alpha * k, alpha * k, 3.0F);
            Draw.end(-1.2F, 2.0F, 1.5F, Draw.MODE_RING);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            Draw.billboard(m, 1.5F + 4.5F * EdpMath.easeIn(k), this.color, alpha);
            Draw.billboard(m, 0.8F + 1.5F * k, hot, alpha);
            Draw.end(0.8F, 3.0F, 1.6F, Draw.MODE_RADIAL);
        }
        if (Draw.begin(Draw.Material.SPHERE)) {
            Draw.sphere(m, 0.25F + 0.55F * k, 10, 16, this.color, alpha);
            Draw.end(Draw.SPHERE_ORB, 2.0F, 3.0F, 0.8F);
        }
    }
}
