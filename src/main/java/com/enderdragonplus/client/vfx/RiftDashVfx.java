package com.enderdragonplus.client.vfx;

import com.enderdragonplus.client.CameraShake;
import com.enderdragonplus.client.Draw;
import com.enderdragonplus.client.VfxInstance;
import com.enderdragonplus.dragon.EdpMath;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;

public final class RiftDashVfx extends VfxInstance {
    private static final int TRAIL_LIFE = 16;
    private static final float WING_SPAN = 8.0F;
    private final int dragonId;
    private final Vec3 from;
    private final Vec3 to;
    private final int telegraph;
    private final int dash;
    private final int color;
    private final Vec3 dir;
    private final Vec3 side;
    private final List<double[]> samples = new ArrayList<>();

    public RiftDashVfx(int dragonId, Vec3 from, Vec3 to, int telegraph, int dash, int color) {
        super(telegraph + dash + TRAIL_LIFE + 10);
        this.dragonId = dragonId;
        this.from = from;
        this.to = to;
        this.telegraph = telegraph;
        this.dash = dash;
        this.color = color;
        Vec3 d = to.subtract(from);
        if (d.lengthSqr() < 1.0E-4) {
            d = new Vec3(0.0, 0.0, 1.0);
        }
        this.dir = d.normalize();
        Vec3 s = this.dir.cross(new Vec3(0.0, 1.0, 0.0));
        if (s.lengthSqr() < 1.0E-4) {
            s = new Vec3(1.0, 0.0, 0.0);
        }
        this.side = s.normalize();
    }

    @Override
    protected void onTick() {
        if (this.age == this.telegraph) {
            CameraShake.addAt(this.from, 0.8F, 40.0F, 10);
        }
        if (this.age == this.telegraph + this.dash) {
            CameraShake.addAt(this.to, 1.0F, 40.0F, 12);
        }
        this.samples.removeIf(s -> this.age - s[4] > TRAIL_LIFE);
        if (this.age < this.telegraph - 1 || this.age > this.telegraph + this.dash + 2) {
            return;
        }
        EnderDragon dragon = dragon(this.dragonId);
        if (dragon == null) {
            return;
        }
        float yaw = dragon.getYRot() * Mth.DEG_TO_RAD;
        this.samples.add(new double[]{dragon.getX(), dragon.getY() + 2.0, dragon.getZ(), yaw, this.age});
    }

    private float riftOpen(float t, float openAt, float closeAt) {
        float open = EdpMath.easeOut((t - openAt) / 6.0F);
        float close = Mth.clamp((closeAt - t) / 8.0F, 0.0F, 1.0F);
        return Mth.clamp(Math.min(open, close), 0.0F, 1.0F);
    }

    private void riftPlane(Vec3 center, float open, float alpha) {
        Vec3 up = new Vec3(0.0, 6.5 * open, 0.0);
        Vec3 right = this.side.scale(3.5 * open + 0.01);
        Draw.plane(center, right, up, this.color, alpha);
    }

    @Override
    public boolean distorts() {
        return true;
    }

    @Override
    public void renderDistortion(float partialTick) {
        float t = this.time(partialTick);
        float entry = this.riftOpen(t, 0.0F, this.telegraph + this.dash + 4.0F);
        float exit = this.riftOpen(t, this.telegraph * 0.5F, this.lifetime);
        if (Draw.begin(Draw.Material.DISTORT)) {
            if (entry > 0.01F) {
                this.riftPlane(this.from, entry * 1.4F, entry);
            }
            if (exit > 0.01F) {
                this.riftPlane(this.to, exit * 1.4F, exit);
            }
            Draw.end(0.06F, Draw.DISTORT_RIFT, 2.0F, 0.5F);
        }
    }

    @Override
    public void render(float partialTick) {
        float t = this.time(partialTick);
        float entry = this.riftOpen(t, 0.0F, this.telegraph + this.dash + 4.0F);
        float exit = this.riftOpen(t, this.telegraph * 0.5F, this.lifetime);
        int hot = Draw.mix(this.color, 0xFFFFFF, 0.4F);
        if (Draw.begin(Draw.Material.ENERGY)) {
            if (entry > 0.01F) {
                this.riftPlane(this.from, entry, entry);
            }
            if (exit > 0.01F) {
                this.riftPlane(this.to, exit, exit);
            }
            Draw.end(0.8F, 2.0F, 18.0F, Draw.MODE_RIFT);
        }
        if (t < this.telegraph) {
            float k = t / this.telegraph;
            float flicker = 0.6F + 0.4F * Mth.sin(t * 2.3F);
            if (Draw.begin(Draw.Material.ENERGY)) {
                Draw.beam(this.from, this.to, 0.4F + 1.2F * k, this.color, k * flicker, (float) this.from.distanceTo(this.to) / 3.0F);
                Draw.end(3.0F, 1.0F, 2.0F, Draw.MODE_STRIP);
            }
        }
        if (this.samples.size() >= 2 && Draw.begin(Draw.Material.ENERGY)) {
            Draw.normal(0.0, 1.0, 0.0);
            for (int i = 0; i < this.samples.size() - 1; i++) {
                double[] a = this.samples.get(i);
                double[] b = this.samples.get(i + 1);
                float fa = 1.0F - Mth.clamp((t - (float) a[4]) / TRAIL_LIFE, 0.0F, 1.0F);
                float fb = 1.0F - Mth.clamp((t - (float) b[4]) / TRAIL_LIFE, 0.0F, 1.0F);
                Vec3 pa = new Vec3(a[0], a[1], a[2]);
                Vec3 pb = new Vec3(b[0], b[1], b[2]);
                Vec3 sa = new Vec3(Math.cos(a[3]), 0.0, Math.sin(a[3])).scale(WING_SPAN * (0.5 + 0.5 * fa));
                Vec3 sb = new Vec3(Math.cos(b[3]), 0.0, Math.sin(b[3])).scale(WING_SPAN * (0.5 + 0.5 * fb));
                float ua = i * 0.35F;
                float ub = (i + 1) * 0.35F;
                Draw.vertex(pa.subtract(sa), ua, 0.0F, this.color, fa * 0.8F);
                Draw.vertex(pa.add(sa), ua, 1.0F, this.color, fa * 0.8F);
                Draw.vertex(pb.add(sb), ub, 1.0F, this.color, fb * 0.8F);
                Draw.vertex(pb.subtract(sb), ub, 0.0F, this.color, fb * 0.8F);
            }
            Draw.end(1.0F, 1.5F, 0.7F, Draw.MODE_STRIP);
        }
        if (this.samples.size() >= 2 && Draw.begin(Draw.Material.ENERGY)) {
            int n = this.samples.size();
            Vec3[] points = new Vec3[n];
            float[] widths = new float[n];
            float[] alphas = new float[n];
            for (int i = 0; i < n; i++) {
                double[] s = this.samples.get(i);
                float f = 1.0F - Mth.clamp((t - (float) s[4]) / TRAIL_LIFE, 0.0F, 1.0F);
                points[i] = new Vec3(s[0], s[1], s[2]);
                widths[i] = 2.5F * f;
                alphas[i] = f;
            }
            Draw.ribbon(points, widths, alphas, hot, 0.0F, n * 0.5F);
            Draw.end(2.5F, 1.0F, 2.0F, Draw.MODE_STRIP);
        }
    }
}
