package com.enderdragonplus.client.vfx;

import com.enderdragonplus.client.Draw;
import com.enderdragonplus.client.VfxInstance;
import com.enderdragonplus.dragon.EdpMath;
import net.minecraft.world.phys.Vec3;

public final class ShockwaveVfx extends VfxInstance {
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
    private final Vec3 pos;
    private final float radius;
    private final int color;
    private final boolean sphere;

    public ShockwaveVfx(Vec3 pos, float radius, int duration, int color, boolean sphere) {
        super(duration);
        this.pos = pos;
        this.radius = radius;
        this.color = color;
        this.sphere = sphere;
    }

    private float progress(float partialTick) {
        return Math.min(1.0F, this.time(partialTick) / this.lifetime);
    }

    @Override
    public boolean distorts() {
        return true;
    }

    @Override
    public void renderDistortion(float partialTick) {
        float k = this.progress(partialTick);
        float r = this.radius * EdpMath.easeOut(k);
        if (Draw.begin(Draw.Material.DISTORT)) {
            Draw.ring(this.pos.add(0.0, 0.3, 0.0), UP, r * 0.8F, r * 1.08F, 48, this.color, 1.0F - k, 1.0F - k, 1.0F);
            Draw.end(0.035F, Draw.DISTORT_BAND, 2.0F, 0.4F);
        }
    }

    @Override
    public void render(float partialTick) {
        float k = this.progress(partialTick);
        float r = this.radius * EdpMath.easeOut(k);
        float a = 1.0F - k;
        Vec3 base = this.pos.add(0.0, 0.15, 0.0);
        if (Draw.begin(Draw.Material.ENERGY)) {
            Draw.ring(base, UP, Math.max(0.0F, r - 1.6F - this.radius * 0.08F), r, 64, this.color, a, a, 6.0F);
            Draw.ring(base.add(0.0, 0.05, 0.0), UP, Math.max(0.0F, r * 0.55F - 0.8F), r * 0.55F, 48, Draw.mix(this.color, 0xFFFFFF, 0.4F), a * 0.6F, a * 0.6F, 4.0F);
            Draw.end(1.4F, 3.0F, 1.5F, Draw.MODE_RING);
        }
        float height = this.radius * 0.18F * (1.0F - k) + 0.5F;
        if (Draw.begin(Draw.Material.SPHERE)) {
            Draw.cylinder(this.pos, this.pos.add(0.0, height, 0.0), r, 48, this.color, a * 0.8F, 2.0F);
            if (this.sphere) {
                Draw.sphere(this.pos, r * 0.7F, 16, 32, this.color, a * 0.7F);
            }
            Draw.end(Draw.SPHERE_SHELL, 2.5F, 2.0F, 0.6F);
        }
        if (k < 0.3F && Draw.begin(Draw.Material.ENERGY)) {
            Draw.billboard(this.pos.add(0.0, 1.0, 0.0), this.radius * 0.8F * (1.0F - k / 0.3F) + 2.0F, this.color, 1.0F - k / 0.3F);
            Draw.end(0.5F, 2.0F, 2.0F, Draw.MODE_RADIAL);
        }
    }
}
