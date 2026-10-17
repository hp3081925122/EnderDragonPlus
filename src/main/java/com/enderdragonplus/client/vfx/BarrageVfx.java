package com.enderdragonplus.client.vfx;

import com.enderdragonplus.client.Draw;
import com.enderdragonplus.client.VfxInstance;
import com.enderdragonplus.client.VfxManager;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class BarrageVfx extends VfxInstance {
    public static final int STYLE_CRESCENT = 1;
    private static final float CRESCENT_RADIUS = 2.2F;
    private static final float CRESCENT_ARC = 60.0F * Mth.DEG_TO_RAD;
    private static final Map<Integer, BarrageVfx> ACTIVE = new HashMap<>();
    private final int id;
    private final Vec3[] directions;
    private final Vec3[] velocities;
    private final Vec3[] crescentPoints = new Vec3[9];
    private final float[] crescentWidths = new float[9];
    private final float[] crescentAlphas = new float[9];
    private final Vec3[] trailPoints = new Vec3[3];
    private static final float[] TRAIL_WIDTHS = {0.7F, 0.45F, 0.0F};
    private static final float[] TRAIL_ALPHAS = {1.0F, 0.7F, 0.0F};
    private final Vec3[] positions;
    private final Vec3[] previous;
    private final boolean[] alive;
    private final double speed;
    private final int life;
    private final int color;
    private final int style;
    private int simAge;

    public BarrageVfx(int id, Vec3 origin, Vec3[] directions, double speed, int life, int color, int style) {
        super(life + 2);
        this.id = id;
        this.directions = directions;
        this.velocities = new Vec3[directions.length];
        this.positions = new Vec3[directions.length];
        this.previous = new Vec3[directions.length];
        this.alive = new boolean[directions.length];
        for (int i = 0; i < directions.length; i++) {
            this.positions[i] = origin;
            this.velocities[i] = directions[i].scale(speed);
            this.previous[i] = origin;
            this.alive[i] = true;
        }
        this.speed = speed;
        this.life = life;
        this.color = color;
        this.style = style;
        ACTIVE.put(id, this);
    }

    public static void clearAll() {
        ACTIVE.clear();
    }

    public static void hit(int id, int index) {
        BarrageVfx barrage = ACTIVE.get(id);
        if (barrage == null || index < 0 || index >= barrage.alive.length || !barrage.alive[index]) {
            return;
        }
        barrage.alive[index] = false;
        VfxManager.add(new ShockwaveVfx(barrage.positions[index], 2.5F, 10, barrage.color, true));
    }

    @Override
    public boolean tick() {
        boolean alive = super.tick();
        if (!alive) {
            ACTIVE.remove(this.id);
        }
        return alive;
    }

    @Override
    protected void onTick() {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        this.simAge++;
        boolean any = false;
        for (int i = 0; i < this.positions.length; i++) {
            this.previous[i] = this.positions[i];
            if (!this.alive[i]) {
                continue;
            }
            Vec3 from = this.positions[i];
            Vec3 to = from.add(this.velocities[i]);
            if (this.simAge > 3) {
                BlockHitResult hit = Minecraft.getInstance().level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (net.minecraft.world.entity.Entity) null));
                if (hit.getType() != HitResult.Type.MISS) {
                    this.alive[i] = false;
                    VfxManager.add(new ShockwaveVfx(hit.getLocation(), 2.2F, 10, this.color, false));
                    continue;
                }
            }
            this.positions[i] = to;
            any = true;
        }
        if (!any || this.simAge >= this.life) {
            this.lifetime = this.age;
        }
    }

    private void crescent(Vec3 head, Vec3 dir, float scale, float alpha, int rgb) {
        Vec3 side = dir.cross(new Vec3(0.0, 1.0, 0.0));
        if (side.lengthSqr() < 1.0E-4) {
            side = new Vec3(1.0, 0.0, 0.0);
        }
        side = side.normalize();
        int n = 9;
        Vec3[] points = this.crescentPoints;
        float[] widths = this.crescentWidths;
        float[] alphas = this.crescentAlphas;
        float radius = CRESCENT_RADIUS * scale;
        for (int j = 0; j < n; j++) {
            float k = j / (float) (n - 1) * 2.0F - 1.0F;
            float a = k * CRESCENT_ARC;
            points[j] = head.add(side.scale(Mth.sin(a) * radius)).subtract(dir.scale((1.0F - Mth.cos(a)) * radius * 0.8F));
            float taper = 1.0F - k * k;
            widths[j] = 0.15F + 0.85F * taper * scale;
            alphas[j] = alpha * (0.3F + 0.7F * taper);
        }
        Draw.ribbon(points, widths, alphas, rgb, 0.0F, 2.0F);
    }

    private void renderCrescents(float partialTick) {
        int hot = Draw.mix(this.color, 0xFFFFFF, 0.55F);
        if (Draw.begin(Draw.Material.ENERGY)) {
            for (int i = 0; i < this.positions.length; i++) {
                if (!this.alive[i]) {
                    continue;
                }
                Vec3 head = this.previous[i].lerp(this.positions[i], partialTick);
                Vec3 dir = this.directions[i];
                this.crescent(head, dir, 1.0F, 1.0F, this.color);
                this.crescent(head.subtract(dir.scale(this.speed * 1.6)), dir, 0.9F, 0.5F, this.color);
                this.crescent(head.subtract(dir.scale(this.speed * 3.2)), dir, 0.8F, 0.25F, this.color);
            }
            Draw.end(2.5F, 1.0F, 1.6F, Draw.MODE_STRIP);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            for (int i = 0; i < this.positions.length; i++) {
                if (!this.alive[i]) {
                    continue;
                }
                Vec3 head = this.previous[i].lerp(this.positions[i], partialTick);
                this.crescent(head.add(this.directions[i].scale(0.15)), this.directions[i], 0.92F, 0.9F, hot);
                Draw.billboard(head, 2.4F, this.color, 0.7F);
            }
            Draw.end(1.0F, 2.0F, 2.0F, Draw.MODE_RADIAL);
        }
    }

    @Override
    public void render(float partialTick) {
        if (this.style == STYLE_CRESCENT) {
            this.renderCrescents(partialTick);
            return;
        }
        int hot = Draw.mix(this.color, 0xFFFFFF, 0.55F);
        if (Draw.begin(Draw.Material.ENERGY)) {
            for (int i = 0; i < this.positions.length; i++) {
                if (!this.alive[i]) {
                    continue;
                }
                Vec3 head = this.previous[i].lerp(this.positions[i], partialTick);
                Vec3 tail = head.subtract(this.directions[i].scale(this.speed * 5.0));
                Vec3 mid = head.subtract(this.directions[i].scale(this.speed * 1.5));
                this.trailPoints[0] = head;
                this.trailPoints[1] = mid;
                this.trailPoints[2] = tail;
                Draw.ribbon(this.trailPoints, TRAIL_WIDTHS, TRAIL_ALPHAS, this.color, 0.0F, 1.5F);
                Draw.beam(head.add(this.directions[i].scale(0.5)), mid, 0.32F, hot, 1.0F, 1.0F);
            }
            Draw.end(2.0F, 1.0F, 1.8F, Draw.MODE_STRIP);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            for (int i = 0; i < this.positions.length; i++) {
                if (!this.alive[i]) {
                    continue;
                }
                Vec3 head = this.previous[i].lerp(this.positions[i], partialTick);
                Draw.billboard(head, 1.8F, this.color, 1.0F);
            }
            Draw.end(1.0F, 2.0F, 2.0F, Draw.MODE_RADIAL);
        }
    }
}
