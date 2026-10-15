package com.enderdragonplus.client.vfx;

import com.enderdragonplus.client.Draw;
import com.enderdragonplus.client.VfxInstance;
import com.enderdragonplus.dragon.EdpMath;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ShieldVfx extends VfxInstance {
    private final int dragonId;
    private final float radius;
    private final int color;
    private final List<Vec3> crystals = new ArrayList<>();
    private Vec3 lastCenter;
    private long arcSeed;

    public ShieldVfx(int dragonId, int duration, float radius, int color) {
        super(duration + 10);
        this.dragonId = dragonId;
        this.radius = radius;
        this.color = color;
    }

    private Vec3 center(float partialTick) {
        EnderDragon dragon = dragon(this.dragonId);
        if (dragon != null) {
            this.lastCenter = bodyCenter(dragon, partialTick);
        }
        return this.lastCenter;
    }

    private float scale(float partialTick) {
        float t = this.time(partialTick);
        float grow = EdpMath.easeOut(t / 12.0F);
        float shrink = Mth.clamp((this.lifetime - t) / 10.0F, 0.0F, 1.0F);
        return Math.min(grow, shrink);
    }

    @Override
    protected void onTick() {
        if (this.age % 2 != 0 || Minecraft.getInstance().level == null) {
            return;
        }
        Vec3 c = this.center(1.0F);
        if (c == null) {
            return;
        }
        this.arcSeed = this.age * 7919L;
        this.crystals.clear();
        for (EndCrystal crystal : Minecraft.getInstance().level.getEntitiesOfClass(EndCrystal.class, new AABB(c, c).inflate(160.0))) {
            this.crystals.add(crystal.position().add(0.0, 1.2, 0.0));
        }
    }

    @Override
    public boolean distorts() {
        return true;
    }

    @Override
    public void renderDistortion(float partialTick) {
        Vec3 c = this.center(partialTick);
        if (c == null) {
            return;
        }
        float s = this.scale(partialTick);
        if (Draw.begin(Draw.Material.DISTORT)) {
            Draw.sphere(c, this.radius * 1.08F * s, 16, 24, this.color, s);
            Draw.end(0.02F, Draw.DISTORT_LENS, 1.0F, 0.0F);
        }
    }

    @Override
    public void render(float partialTick) {
        Vec3 c = this.center(partialTick);
        if (c == null) {
            return;
        }
        float s = this.scale(partialTick);
        if (s <= 0.01F) {
            return;
        }
        int hot = Draw.mix(this.color, 0xFFFFFF, 0.5F);
        if (Draw.begin(Draw.Material.SPHERE)) {
            Draw.sphere(c, this.radius * s, 20, 32, this.color, s);
            Draw.end(Draw.SPHERE_SHIELD, 2.5F, 4.0F, 0.15F);
        }
        if (Draw.begin(Draw.Material.SPHERE)) {
            Draw.sphere(c, this.radius * 0.35F * s, 10, 16, this.color, s * 0.7F);
            Draw.end(Draw.SPHERE_ORB, 2.0F, 2.0F, 0.6F);
        }
        if (this.crystals.isEmpty()) {
            return;
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            long seed = this.arcSeed;
            for (Vec3 crystal : this.crystals) {
                Vec3 target = c.add(crystal.subtract(c).normalize().scale(this.radius * s));
                int segments = Mth.clamp((int) (crystal.distanceTo(target) / 3.0), 6, 24);
                for (int strand = 0; strand < 2; strand++) {
                    seed = seed * 6364136223846793005L + 1442695040888963407L;
                    Vec3[] points = Draw.lightning(crystal, target, segments, 1.6 - strand * 0.6, seed);
                    float[] widths = new float[points.length];
                    float[] alphas = new float[points.length];
                    for (int i = 0; i < points.length; i++) {
                        widths[i] = (1.2F - strand * 0.6F) * s;
                        alphas[i] = s;
                    }
                    int arcColor = this.color;
                    if (strand > 0) {
                        arcColor = hot;
                    }
                    Draw.ribbon(points, widths, alphas, arcColor, 0.0F, segments * 0.5F);
                }
            }
            Draw.end(3.0F, 1.0F, 1.6F, Draw.MODE_STRIP);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            for (Vec3 crystal : this.crystals) {
                Draw.billboard(crystal, 3.0F * s, hot, s);
                Draw.billboard(c.add(crystal.subtract(c).normalize().scale(this.radius * s)), 4.0F * s, this.color, s);
            }
            Draw.end(1.5F, 3.0F, 1.5F, Draw.MODE_RADIAL);
        }
    }
}
