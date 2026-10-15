package com.enderdragonplus.client.vfx;

import com.enderdragonplus.client.CameraShake;
import com.enderdragonplus.client.Draw;
import com.enderdragonplus.client.VfxInstance;
import com.enderdragonplus.client.VfxManager;
import com.enderdragonplus.dragon.EdpMath;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class LanceVfx extends VfxInstance {
    private static final int FIRE = 12;
    private final Vec3 origin;
    private final Vec3 end;
    private final int charge;
    private final int color;
    private final Vec3 right;
    private final Vec3 up;
    private final float length;

    public LanceVfx(Vec3 origin, Vec3 end, int charge, int color) {
        super(charge + FIRE + 4);
        this.origin = origin;
        this.end = end;
        this.charge = charge;
        this.color = color;
        Vec3 dir = end.subtract(origin);
        if (dir.lengthSqr() < 1.0E-4) {
            dir = new Vec3(0.0, -1.0, 0.0);
        }
        Vec3[] base = Draw.basis(dir);
        this.right = base[0];
        this.up = base[1];
        this.length = (float) dir.length();
    }

    @Override
    protected void onTick() {
        if (this.age != this.charge) {
            return;
        }
        CameraShake.addAt(this.end, 1.0F, 30.0F, 8);
        CameraShake.addAt(this.origin, 0.5F, 20.0F, 6);
        VfxManager.add(new ShockwaveVfx(this.end, 4.0F, 12, this.color, true));
        VfxManager.add(new DecalVfx(this.end, 2.8F, 60, this.color, true));
    }

    private float open(float t) {
        float in = EdpMath.easeOut(t / 8.0F);
        float out = Mth.clamp((this.lifetime - t) / 6.0F, 0.0F, 1.0F);
        return Math.min(in, out);
    }

    @Override
    public boolean distorts() {
        return true;
    }

    @Override
    public void renderDistortion(float partialTick) {
        float t = this.time(partialTick);
        float open = this.open(t);
        if (open > 0.01F && Draw.begin(Draw.Material.DISTORT)) {
            Draw.plane(this.origin, this.right.scale(1.6F * open), this.up.scale(3.6F * open), this.color, open);
            Draw.end(0.05F, Draw.DISTORT_RIFT, 2.0F, 0.6F);
        }
    }

    @Override
    public void render(float partialTick) {
        float t = this.time(partialTick);
        float open = this.open(t);
        int hot = Draw.mix(this.color, 0xFFFFFF, 0.6F);
        if (open > 0.01F && Draw.begin(Draw.Material.ENERGY)) {
            Draw.plane(this.origin, this.right.scale(1.1F * open + 0.01F), this.up.scale(3.0F * open), this.color, open);
            Draw.end(0.8F, 2.0F, 18.0F, Draw.MODE_RIFT);
        }
        if (t < this.charge) {
            float k = t / this.charge;
            float flicker = 0.55F + 0.45F * Mth.sin(t * 2.7F);
            if (Draw.begin(Draw.Material.ENERGY)) {
                Draw.beam(this.origin, this.end, 0.12F + 0.3F * k, this.color, k * flicker * 0.9F, this.length / 3.0F);
                Draw.end(3.0F, 1.0F, 2.0F, Draw.MODE_STRIP);
            }
            if (Draw.begin(Draw.Material.ENERGY)) {
                Draw.billboard(this.end, 1.0F + 2.0F * k, this.color, k * flicker);
                Draw.billboard(this.origin, 2.0F + 2.0F * k, hot, k);
                Draw.end(1.0F, 2.0F, 2.0F, Draw.MODE_RADIAL);
            }
            return;
        }
        float local = t - this.charge;
        if (local >= FIRE) {
            return;
        }
        float f = 1.0F - local / FIRE;
        if (Draw.begin(Draw.Material.ENERGY)) {
            Draw.beam(this.origin, this.end, 2.6F * f + 0.4F, this.color, f, this.length / 2.0F);
            Draw.beam(this.origin, this.end, 0.9F * f + 0.1F, hot, f, this.length / 2.0F);
            Draw.end(5.0F, 1.0F, 1.5F, Draw.MODE_STRIP);
        }
        if (Draw.begin(Draw.Material.SPHERE)) {
            Draw.cylinder(this.origin, this.end, 1.2F * f + 0.3F, 16, this.color, f * 0.7F, this.length / 4.0F);
            Draw.end(Draw.SPHERE_SHELL, 2.0F, 2.0F, 2.0F);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            Draw.billboard(this.end, 6.0F * f + 1.0F, hot, f);
            Draw.end(0.5F, 2.0F, 2.0F, Draw.MODE_RADIAL);
        }
    }
}
