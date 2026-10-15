package com.enderdragonplus.client.vfx;

import com.enderdragonplus.client.CameraShake;
import com.enderdragonplus.client.Draw;
import com.enderdragonplus.client.ScreenFx;
import com.enderdragonplus.client.VfxInstance;
import com.enderdragonplus.client.VfxManager;
import com.enderdragonplus.dragon.EdpMath;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;

public final class NovaVfx extends VfxInstance {
    private static final int STREAKS = 28;
    private static final int BLAST_TICKS = 30;
    private final int dragonId;
    private final Vec3 center;
    private final Vec3[] zones;
    private final float safeRadius;
    private final int charge;
    private final float blast;
    private final int color;
    private final Vec3[] directions = new Vec3[STREAKS];
    private Vec3 lastCore;

    public NovaVfx(int dragonId, Vec3 center, Vec3[] zones, float safeRadius, int charge, float blast, int color) {
        super(charge + BLAST_TICKS + 4);
        this.dragonId = dragonId;
        this.center = center;
        this.zones = zones;
        this.safeRadius = safeRadius;
        this.charge = charge;
        this.blast = blast;
        this.color = color;
        this.lastCore = center.add(0.0, 28.0, 0.0);
        for (int i = 0; i < STREAKS; i++) {
            float y = 1.0F - (i + 0.5F) / STREAKS * 2.0F;
            float r = Mth.sqrt(1.0F - y * y);
            float a = i * 2.39996F;
            this.directions[i] = new Vec3(Mth.cos(a) * r, y, Mth.sin(a) * r);
        }
    }

    private Vec3 core(float partialTick) {
        EnderDragon dragon = dragon(this.dragonId);
        if (dragon != null) {
            this.lastCore = bodyCenter(dragon, partialTick).add(0.0, 4.0, 0.0);
        }
        return this.lastCore;
    }

    @Override
    protected void onTick() {
        if (this.age < this.charge) {
            float k = this.age / (float) this.charge;
            ScreenFx.sustain(0.15F * k, 0.55F * k, 0x3A0010);
            if (this.age % 20 == 0) {
                CameraShake.add(0.25F + 0.5F * k, 20);
            }
            return;
        }
        if (this.age == this.charge) {
            ScreenFx.flash(1.0F);
            ScreenFx.chromatic(1.0F);
            CameraShake.add(3.0F, 40);
            VfxManager.add(new ShockwaveVfx(this.center, this.blast * 0.6F, 40, this.color, false));
            VfxManager.add(new DecalVfx(this.center, 18.0F, 200, this.color, true));
        }
    }

    @Override
    public boolean distorts() {
        return true;
    }

    @Override
    public void renderDistortion(float partialTick) {
        float t = this.time(partialTick);
        if (t < this.charge) {
            float k = t / this.charge;
            if (Draw.begin(Draw.Material.DISTORT)) {
                Draw.sphere(this.core(partialTick), 3.0F + 9.0F * k, 16, 24, this.color, k);
                Draw.end(0.06F, Draw.DISTORT_LENS, 1.0F, 0.0F);
            }
            return;
        }
        float b = Math.min(1.0F, (t - this.charge) / BLAST_TICKS);
        if (Draw.begin(Draw.Material.DISTORT)) {
            Draw.sphere(this.center, this.blast * EdpMath.easeOut(b), 20, 32, this.color, 1.0F - b);
            Draw.end(0.05F, Draw.DISTORT_LENS, 1.0F, 0.0F);
        }
    }

    @Override
    public void render(float partialTick) {
        float t = this.time(partialTick);
        int hot = Draw.mix(this.color, 0xFFFFFF, 0.6F);
        int safeColor = 0x7CF7FF;
        if (t < this.charge) {
            float k = t / this.charge;
            Vec3 core = this.core(partialTick);
            if (Draw.begin(Draw.Material.GROUND)) {
                Draw.terrain(this.center, this.blast * 0.6F, 48, 0.08F, this.color, Math.min(1.0F, t / 10.0F));
                Draw.end(Draw.GROUND_DANGER, k, 0.7F, 0.0F);
            }
            if (Draw.begin(Draw.Material.GROUND)) {
                for (Vec3 zone : this.zones) {
                    Draw.terrain(zone, this.safeRadius, 8, 0.12F, safeColor, Math.min(1.0F, t / 8.0F));
                }
                Draw.end(Draw.GROUND_SAFE, k, 0.0F, 0.0F);
            }
            if (Draw.begin(Draw.Material.ENERGY)) {
                for (Vec3 zone : this.zones) {
                    Draw.beam(zone, zone.add(0.0, 40.0, 0.0), this.safeRadius * 1.6F, safeColor, 0.35F + 0.15F * Mth.sin(t * 0.4F), 4.0F);
                }
                Draw.end(0.6F, 1.0F, 0.8F, Draw.MODE_STRIP);
            }
            if (Draw.begin(Draw.Material.ENERGY)) {
                for (int i = 0; i < STREAKS; i++) {
                    float phase = (t * (0.03F + 0.03F * k) + i * 0.379F) % 1.0F;
                    double d = 36.0 * (1.0F - phase) + 2.0;
                    Vec3 head = core.add(this.directions[i].scale(d));
                    Vec3 tail = core.add(this.directions[i].scale(d + 3.0 + 4.0 * k));
                    Draw.beam(head, tail, 0.3F + 0.4F * k, hot, phase * (0.4F + 0.6F * k), 1.0F);
                }
                Draw.end(1.5F, 1.0F, 2.0F, Draw.MODE_STRIP);
            }
            if (Draw.begin(Draw.Material.ENERGY)) {
                Draw.ring(core, Draw.cameraPos().subtract(core), 4.0F + 12.0F * (1.0F - k), 5.0F + 12.0F * (1.0F - k), 48, this.color, k, k, 4.0F);
                Draw.end(-1.5F, 3.0F, 1.5F, Draw.MODE_RING);
            }
            if (Draw.begin(Draw.Material.ENERGY)) {
                Draw.billboard(core, 8.0F + 22.0F * EdpMath.easeIn(k), this.color, 0.9F);
                Draw.billboard(core, 3.0F + 8.0F * k, hot, 1.0F);
                Draw.end(1.0F, 3.0F, 1.3F, Draw.MODE_RADIAL);
            }
            if (Draw.begin(Draw.Material.SPHERE)) {
                Draw.sphere(core, 1.2F + 4.0F * EdpMath.easeIn(k), 14, 24, this.color, 1.0F);
                Draw.end(Draw.SPHERE_ORB, 2.0F, 3.0F, 1.0F + 2.0F * k);
            }
            return;
        }
        float b = Math.min(1.0F, (t - this.charge) / BLAST_TICKS);
        float r = this.blast * EdpMath.easeOut(b);
        float a = 1.0F - b;
        if (Draw.begin(Draw.Material.SPHERE)) {
            Draw.sphere(this.center, r, 24, 40, this.color, a);
            Draw.sphere(this.center, r * 0.85F, 20, 32, hot, a * 0.6F);
            Draw.end(Draw.SPHERE_SHELL, 1.8F, 2.0F, 1.2F);
        }
        if (b < 0.4F && Draw.begin(Draw.Material.SPHERE)) {
            Draw.sphere(this.center, 6.0F + 20.0F * b, 16, 24, 0xFFFFFF, 1.0F - b / 0.4F);
            Draw.end(Draw.SPHERE_ORB, 2.0F, 2.0F, 2.0F);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            Draw.ring(this.center.add(0.0, 0.3, 0.0), new Vec3(0.0, 1.0, 0.0), Math.max(0.0F, r - 6.0F), r, 96, hot, a, a, 10.0F);
            Draw.end(2.0F, 3.0F, 1.2F, Draw.MODE_RING);
        }
    }
}
