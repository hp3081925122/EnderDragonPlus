package com.enderdragonplus.client.vfx;

import com.enderdragonplus.client.CameraShake;
import com.enderdragonplus.EdpConfig;
import com.enderdragonplus.client.Draw;
import com.enderdragonplus.client.ScreenFx;
import com.enderdragonplus.client.VfxInstance;
import com.enderdragonplus.client.VfxManager;
import com.enderdragonplus.dragon.EdpMath;
import com.enderdragonplus.network.VfxPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

public final class MeteorShowerVfx extends VfxInstance {
    private final Vec3[] trailPoints = new Vec3[TRAIL];
    private final float[] trailWidths = new float[TRAIL];
    private final float[] trailAlphas = new float[TRAIL];
    private static final int TRAIL = 10;
    private final Vec3[] starts;
    private final Vec3[] impacts;
    private final int[] delays;
    private final int[] falls;
    private final float radius;
    private final int color;

    public MeteorShowerVfx(CompoundTag data) {
        super(1);
        ListTag list = data.getList("meteors", Tag.TAG_COMPOUND);
        int n = list.size();
        this.starts = new Vec3[n];
        this.impacts = new Vec3[n];
        this.delays = new int[n];
        this.falls = new int[n];
        int last = 0;
        for (int i = 0; i < n; i++) {
            CompoundTag meteor = list.getCompound(i);
            this.starts[i] = VfxPayload.getVec(meteor, "s");
            this.impacts[i] = VfxPayload.getVec(meteor, "e");
            this.delays[i] = meteor.getInt("delay");
            this.falls[i] = Math.max(1, meteor.getInt("fall"));
            last = Math.max(last, this.delays[i] + this.falls[i]);
        }
        this.lifetime = last + 5;
        this.radius = (float) data.getDouble("radius");
        this.color = data.getInt("color");
    }

    private Vec3 position(int i, float t) {
        float f = (t - this.delays[i]) / this.falls[i];
        return this.starts[i].lerp(this.impacts[i], EdpMath.easeIn(f) * 0.6F + f * 0.4F);
    }

    @Override
    protected void onTick() {
        for (int i = 0; i < this.impacts.length; i++) {
            if (this.age != this.delays[i] + this.falls[i]) {
                continue;
            }
            Vec3 impact = this.impacts[i];
            VfxManager.add(new ShockwaveVfx(impact, this.radius * 2.2F, 22, this.color, false));
            VfxManager.add(new DecalVfx(impact, this.radius * 1.5F, 140, this.color, true));
            VfxManager.add(new DecalVfx(impact, this.radius * 1.1F, 120, this.color, false));
            CameraShake.addAt(impact, 1.4F, 36.0F, 14);
            if (distanceToPlayer(impact) < this.radius * 2.5) {
                ScreenFx.flash(0.25F);
                ScreenFx.chromatic(0.4F);
            }
        }
    }

    @Override
    public boolean distorts() {
        return true;
    }

    @Override
    public void renderDistortion(float partialTick) {
        float t = this.time(partialTick);
        if (!Draw.begin(Draw.Material.DISTORT)) {
            return;
        }
        for (int i = 0; i < this.impacts.length; i++) {
            float f = (t - this.delays[i]) / this.falls[i];
            if (f < 0.0F || f > 1.0F) {
                continue;
            }
            Draw.sphere(this.position(i, t), 2.4F, heavyDetail(8), heavyDetail(12), this.color, 1.0F);
        }
        Draw.end(0.03F, Draw.DISTORT_LENS, 1.0F, 0.0F);
    }

    @Override
    public void render(float partialTick) {
        float t = this.time(partialTick);
        int hot = Draw.mix(this.color, 0xFFFFFF, 0.5F);
        for (int i = 0; i < this.impacts.length; i++) {
            int impactTick = this.delays[i] + this.falls[i];
            if (t > impactTick) {
                continue;
            }
            if (Draw.begin(Draw.Material.GROUND)) {
                Draw.terrain(this.impacts[i], this.radius, 6, 0.1F, this.color, 1.0F);
                Draw.end(Draw.GROUND_WARNING, t / impactTick, 0.0F, 0.0F);
            }
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            for (int i = 0; i < this.impacts.length; i++) {
                float f = (t - this.delays[i]) / this.falls[i];
                if (f < 0.0F || f > 1.0F) {
                    continue;
                }
                Vec3[] points = this.trailPoints;
                float[] widths = this.trailWidths;
                float[] alphas = this.trailAlphas;
                for (int j = 0; j < TRAIL; j++) {
                    float back = j * 0.9F;
                    float k = 1.0F - j / (float) (TRAIL - 1);
                    points[j] = this.position(i, Math.max(this.delays[i], t - back));
                    widths[j] = 2.4F * k + 0.1F;
                    alphas[j] = k;
                }
                Draw.ribbon(points, widths, alphas, this.color, 0.0F, 2.0F);
            }
            Draw.end(2.0F, 1.0F, 1.2F, Draw.MODE_STRIP);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            for (int i = 0; i < this.impacts.length; i++) {
                float f = (t - this.delays[i]) / this.falls[i];
                if (f < 0.0F || f > 1.0F) {
                    continue;
                }
                Vec3 p = this.position(i, t);
                Draw.billboard(p, EdpConfig.OPTIMIZE_HEAVY_EFFECTS.get() ? 5.0F : 6.0F, this.color, 1.0F);
                Draw.billboard(p, 2.6F, hot, 1.0F);
            }
            Draw.end(1.2F, 3.0F, 1.5F, Draw.MODE_RADIAL);
        }
        if (Draw.begin(Draw.Material.SPHERE)) {
            for (int i = 0; i < this.impacts.length; i++) {
                float f = (t - this.delays[i]) / this.falls[i];
                if (f < 0.0F || f > 1.0F) {
                    continue;
                }
                Draw.sphere(this.position(i, t), 1.1F, heavyDetail(10), heavyDetail(16), this.color, 1.0F);
            }
            Draw.end(Draw.SPHERE_ORB, 2.0F, 3.0F, 1.2F);
        }
    }
}
