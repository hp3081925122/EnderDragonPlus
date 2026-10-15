package com.enderdragonplus.client.vfx;

import com.enderdragonplus.client.CameraShake;
import com.enderdragonplus.client.Draw;
import com.enderdragonplus.client.ScreenFx;
import com.enderdragonplus.client.VfxInstance;
import com.enderdragonplus.client.VfxManager;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class StormStrikeVfx extends VfxInstance {
    private static final int BOLT = 6;
    private static final double HEIGHT = 40.0;
    private final Vec3 pos;
    private final int delay;
    private final float radius;
    private final int color;
    private final long seed;

    public StormStrikeVfx(Vec3 pos, int delay, float radius, int color) {
        super(delay + BOLT + 4);
        this.pos = pos;
        this.delay = delay;
        this.radius = radius;
        this.color = color;
        this.seed = Double.doubleToLongBits(pos.x * 31.0 + pos.z * 17.0);
    }

    @Override
    protected void onTick() {
        if (this.age != this.delay) {
            return;
        }
        CameraShake.addAt(this.pos, 0.8F, 30.0F, 8);
        VfxManager.add(new ShockwaveVfx(this.pos, this.radius * 1.8F, 12, this.color, false));
        VfxManager.add(new DecalVfx(this.pos, this.radius * 1.2F, 80, this.color, false));
        if (distanceToPlayer(this.pos) < this.radius * 3.0F) {
            ScreenFx.flash(0.25F);
        }
    }

    private void bolt(Vec3 top, Vec3 bottom, int segments, double jitter, long seed, float width, float alpha, int rgb) {
        Vec3[] points = Draw.lightning(top, bottom, segments, jitter, seed);
        float[] widths = new float[points.length];
        float[] alphas = new float[points.length];
        for (int i = 0; i < points.length; i++) {
            widths[i] = width * (0.6F + 0.4F * i / (points.length - 1));
            alphas[i] = alpha;
        }
        Draw.ribbon(points, widths, alphas, rgb, 0.0F, 4.0F);
    }

    @Override
    public void render(float partialTick) {
        float t = this.time(partialTick);
        if (t < this.delay) {
            float k = t / this.delay;
            if (Draw.begin(Draw.Material.GROUND)) {
                Draw.ground(this.pos, this.radius, this.color, Math.min(1.0F, t / 4.0F));
                Draw.end(Draw.GROUND_WARNING, k, 0.0F, 0.0F);
            }
            if (k > 0.6F && Draw.begin(Draw.Material.ENERGY)) {
                float a = (k - 0.6F) / 0.4F;
                Draw.beam(this.pos, this.pos.add(0.0, HEIGHT, 0.0), 0.25F, this.color, a * 0.5F, 6.0F);
                Draw.end(4.0F, 1.0F, 2.0F, Draw.MODE_STRIP);
            }
            return;
        }
        float local = t - this.delay;
        if (local >= BOLT) {
            return;
        }
        float a = 1.0F - local / BOLT;
        long frame = this.seed + (long) (local / 2.0F);
        Vec3 top = this.pos.add(0.0, HEIGHT, 0.0);
        int hot = Draw.mix(this.color, 0xFFFFFF, 0.7F);
        if (Draw.begin(Draw.Material.ENERGY)) {
            this.bolt(top, this.pos, 14, 2.2, frame, 1.6F * a + 0.3F, a, this.color);
            this.bolt(top, this.pos, 14, 2.2, frame, 0.5F * a + 0.15F, a, hot);
            Vec3 branchFrom = top.lerp(this.pos, 0.45);
            float side = Mth.sin(frame * 1.7F) * 6.0F;
            this.bolt(branchFrom, branchFrom.add(side, -10.0, Mth.cos(frame * 1.3F) * 6.0F), 6, 1.2, frame + 7L, 0.6F * a, a * 0.7F, this.color);
            Draw.end(6.0F, 1.0F, 2.5F, Draw.MODE_STRIP);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            Draw.billboard(this.pos.add(0.0, 0.8, 0.0), this.radius * 3.0F * a + 1.0F, hot, a);
            Draw.end(0.5F, 2.0F, 2.0F, Draw.MODE_RADIAL);
        }
    }
}
