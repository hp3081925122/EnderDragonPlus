package com.enderdragonplus.client.vfx;

import com.enderdragonplus.client.CameraShake;
import com.enderdragonplus.client.Draw;
import com.enderdragonplus.client.VfxInstance;
import com.enderdragonplus.client.VfxManager;
import com.enderdragonplus.dragon.EdpMath;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.world.phys.Vec3;

public final class RuptureVfx extends VfxInstance {
    private static final int WARN = 14;
    private static final int ERUPT = 10;
    private final Vec3[] points;
    private final int[] delays;
    private final float radius;
    private final int color;
    private final int[] warningIndices;
    private final float[] warningAlphas;
    private final int[] eruptIndices;
    private final float[] eruptProgress;
    private final Int2ObjectOpenHashMap<IntArrayList> triggers = new Int2ObjectOpenHashMap<>();

    public RuptureVfx(Vec3[] points, int[] delays, float radius, int color) {
        super(maxDelay(delays) + ERUPT + 2);
        this.points = points;
        this.delays = delays;
        this.radius = radius;
        this.color = color;
        this.warningIndices = new int[points.length];
        this.warningAlphas = new float[points.length];
        this.eruptIndices = new int[points.length];
        this.eruptProgress = new float[points.length];
        for (int i = 0; i < delays.length; i++) {
            this.triggers.computeIfAbsent(delays[i], key -> new IntArrayList()).add(i);
        }
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
        IntArrayList due = this.triggers.get(this.age);
        if (due == null) {
            return;
        }
        for (int n = 0; n < due.size(); n++) {
            int i = due.getInt(n);
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
        int warningCount = 0;
        int eruptCount = 0;
        for (int i = 0; i < this.points.length; i++) {
            float warning = t - (this.delays[i] - WARN);
            if (warning >= 0.0F && warning < WARN) {
                this.warningIndices[warningCount] = i;
                this.warningAlphas[warningCount++] = Math.min(1.0F, warning / 4.0F);
            }
            float erupt = t - this.delays[i];
            if (erupt >= 0.0F && erupt < ERUPT) {
                this.eruptIndices[eruptCount] = i;
                this.eruptProgress[eruptCount++] = erupt / ERUPT;
            }
        }
        if (Draw.begin(Draw.Material.GROUND)) {
            for (int n = 0; n < warningCount; n++) {
                Draw.ground(this.points[this.warningIndices[n]], this.radius, this.color, this.warningAlphas[n]);
            }
            Draw.end(Draw.GROUND_WARNING, 0.5F, 0.0F, 0.0F);
        }
        int hot = Draw.mix(this.color, 0xFFFFFF, 0.5F);
        if (Draw.begin(Draw.Material.SPHERE)) {
            for (int n = 0; n < eruptCount; n++) {
                int i = this.eruptIndices[n];
                float k = this.eruptProgress[n];
                float h = 5.5F * EdpMath.easeOut(k * 2.0F);
                Vec3 p = this.points[i];
                Draw.cylinder(p, p.add(0.0, h, 0.0), this.radius * (0.9F - 0.4F * k), 16, this.color, 1.0F - k, 2.0F);
            }
            Draw.end(Draw.SPHERE_SHELL, 2.5F, 2.0F, 1.2F);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            for (int n = 0; n < eruptCount; n++) {
                int i = this.eruptIndices[n];
                float k = this.eruptProgress[n];
                float a = 1.0F - k;
                float h = 7.0F * EdpMath.easeOut(k * 2.0F);
                Vec3 p = this.points[i];
                Draw.beam(p, p.add(0.0, h, 0.0), 1.4F * a + 0.2F, hot, a, 2.0F);
            }
            Draw.end(3.0F, 1.0F, 2.0F, Draw.MODE_STRIP);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            for (int n = 0; n < eruptCount; n++) {
                int i = this.eruptIndices[n];
                float a = 1.0F - this.eruptProgress[n];
                Draw.billboard(this.points[i].add(0.0, 1.0, 0.0), this.radius * 2.5F * a + 1.0F, this.color, a);
            }
            Draw.end(0.5F, 2.0F, 2.0F, Draw.MODE_RADIAL);
        }
    }
}
