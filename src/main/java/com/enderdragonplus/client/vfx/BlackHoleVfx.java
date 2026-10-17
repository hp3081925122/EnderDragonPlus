package com.enderdragonplus.client.vfx;

import com.enderdragonplus.client.CameraShake;
import com.enderdragonplus.client.Draw;
import com.enderdragonplus.client.ScreenFx;
import com.enderdragonplus.client.VfxInstance;
import com.enderdragonplus.client.VfxManager;
import com.enderdragonplus.dragon.EdpMath;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class BlackHoleVfx extends VfxInstance {
    private final Vec3[] streakPoints = new Vec3[STREAK_POINTS];
    private final float[] streakWidths = new float[STREAK_POINTS];
    private final float[] streakAlphas = new float[STREAK_POINTS];
    private static final int STREAKS = 18;
    private static final int STREAK_POINTS = 8;
    private static final Vec3 DISK_AXIS = new Vec3(0.22, 1.0, 0.14).normalize();
    private static final Vec3 DISK_AXIS_B = new Vec3(-0.3, 1.0, 0.25).normalize();
    private final Vec3 pos;
    private final int form;
    private final int active;
    private final int collapse;
    private final float radius;
    private final int color;

    public BlackHoleVfx(Vec3 pos, int form, int active, int collapse, float radius, int color) {
        super(form + active + collapse + 2);
        this.pos = pos;
        this.form = form;
        this.active = active;
        this.collapse = collapse;
        this.radius = radius;
        this.color = color;
    }

    private float scale(float t) {
        if (t < this.form) {
            return EdpMath.easeOut(t / this.form);
        }
        float c = (t - this.form - this.active) / this.collapse;
        if (c <= 0.0F) {
            return 1.0F + 0.04F * Mth.sin(t * 0.5F);
        }
        return 1.0F - EdpMath.easeIn(c) * 0.9F;
    }

    @Override
    protected void onTick() {
        int end = this.form + this.active + this.collapse;
        if (this.age == end) {
            VfxManager.add(new ShockwaveVfx(this.pos, 14.0F, 22, this.color, true));
            CameraShake.addAt(this.pos, 2.2F, 50.0F, 20);
            if (distanceToPlayer(this.pos) < 20.0) {
                ScreenFx.flash(0.55F);
                ScreenFx.chromatic(0.8F);
            }
            return;
        }
        if (this.age < end) {
            double d = distanceToPlayer(this.pos);
            float near = 1.0F - (float) (d / this.radius);
            if (near > 0.0F) {
                float s = this.scale(this.age);
                ScreenFx.sustain(0.45F * near * s, 0.6F * near * s, 0x1A0026);
            }
        }
    }

    @Override
    public boolean distorts() {
        return true;
    }

    @Override
    public void renderDistortion(float partialTick) {
        float s = this.scale(this.time(partialTick));
        if (Draw.begin(Draw.Material.DISTORT)) {
            Draw.sphere(this.pos, 7.5F * s, heavyDetail(16), heavyDetail(24), this.color, 1.0F);
            Draw.end(0.08F, Draw.DISTORT_LENS, 1.0F, 0.0F);
        }
        if (Draw.begin(Draw.Material.DISTORT)) {
            Draw.ring(this.pos, DISK_AXIS, 2.5F * s, 11.0F * s, heavyDetail(48), this.color, 1.0F, 1.0F, 1.0F);
            Draw.end(0.03F, Draw.DISTORT_BAND, 2.0F, 0.6F);
        }
    }

    @Override
    public void render(float partialTick) {
        float t = this.time(partialTick);
        float s = this.scale(t);
        float alpha = this.fade(partialTick, 0.0F, 3.0F);
        int hot = Draw.mix(this.color, 0xFFFFFF, 0.55F);
        if (Draw.begin(Draw.Material.GROUND)) {
            Draw.terrain(this.pos.subtract(0.0, 7.0, 0.0), this.radius, 24, 0.08F, this.color, alpha * s);
            Draw.end(Draw.GROUND_DANGER, s, 0.3F, 0.0F);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            Draw.billboard(this.pos, 16.0F * s, this.color, alpha * 0.8F);
            Draw.end(0.4F, 2.0F, 1.2F, Draw.MODE_RADIAL);
        }
        if (Draw.begin(Draw.Material.SPHERE_ALPHA)) {
            Draw.sphere(this.pos, 2.4F * s, heavyDetail(16), heavyDetail(24), this.color, alpha);
            Draw.end(Draw.SPHERE_DARK, 3.0F, 3.0F, 0.5F);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            Draw.ring(this.pos, DISK_AXIS, 2.6F * s, 9.5F * s, heavyDetail(64), this.color, alpha, alpha * 0.2F, 3.0F);
            Draw.end(-0.9F, 5.0F, 1.0F, Draw.MODE_RING);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            Draw.ring(this.pos, DISK_AXIS_B, 2.3F * s, 5.0F * s, heavyDetail(48), hot, alpha, alpha * 0.4F, 2.0F);
            Draw.end(1.4F, 4.0F, 2.5F, Draw.MODE_RING);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            Vec3[] base = Draw.basis(DISK_AXIS);
            for (int i = 0; i < STREAKS; i++) {
                float phase = (t * 0.025F + i * 0.618F) % 1.0F;
                float baseAngle = i * 2.39996F;
                float tilt = ((i * 37) % 11 - 5) * 0.06F;
                Vec3[] points = this.streakPoints;
                float[] widths = this.streakWidths;
                float[] alphas = this.streakAlphas;
                for (int j = 0; j < STREAK_POINTS; j++) {
                    float p = Math.max(0.0F, phase - j * 0.025F);
                    float r = (this.radius * 0.9F * (1.0F - p) + 2.4F) * s;
                    float angle = baseAngle + p * 7.0F;
                    Vec3 offset = base[0].scale(Mth.cos(angle) * r).add(base[1].scale(Mth.sin(angle) * r)).add(DISK_AXIS.scale(r * tilt));
                    points[j] = this.pos.add(offset);
                    float k = 1.0F - j / (float) (STREAK_POINTS - 1);
                    widths[j] = (0.25F + 0.45F * phase) * k;
                    alphas[j] = alpha * k * Math.min(1.0F, phase * 4.0F);
                }
                Draw.ribbon(points, widths, alphas, hot, 0.0F, 1.0F);
            }
            Draw.end(1.0F, 1.0F, 1.5F, Draw.MODE_STRIP);
        }
    }
}
