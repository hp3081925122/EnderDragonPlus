package com.enderdragonplus.client.vfx;

import com.enderdragonplus.client.Draw;
import com.enderdragonplus.client.VfxInstance;
import com.enderdragonplus.client.VfxManager;
import com.enderdragonplus.dragon.EdpMath;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class BreathBeamVfx extends VfxInstance {
    private final Vec3[] helixPoints = new Vec3[HELIX_POINTS];
    private final float[] helixWidths = new float[HELIX_POINTS];
    private final float[] helixAlphas = new float[HELIX_POINTS];
    private static final Map<Integer, BreathBeamVfx> ACTIVE = new HashMap<>();
    private static final int HELIX_POINTS = 24;
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
    private final int dragonId;
    private final int duration;
    private final double length;
    private final int color;
    private Vec3 aim;
    private Vec3 prevAim;
    private Vec3 lastOrigin;
    private boolean aimUpdated;
    private long clippedFrame = Long.MIN_VALUE;
    private Vec3 clippedFrom;
    private Vec3 clippedDirection;
    private BlockHitResult clippedHit;

    private BlockHitResult renderClip(Vec3 from, Vec3 direction) {
        long frame = Draw.frameNumber();
        if (this.clippedHit == null || this.clippedFrame != frame || !from.equals(this.clippedFrom) || !direction.equals(this.clippedDirection)) {
            this.clippedHit = this.clip(from, direction);
            this.clippedFrame = frame;
            this.clippedFrom = from;
            this.clippedDirection = direction;
        }
        return this.clippedHit;
    }

    public BreathBeamVfx(int dragonId, Vec3 aim, int duration, double length, int color) {
        super(duration + 10);
        this.dragonId = dragonId;
        this.aim = aim;
        this.prevAim = aim;
        this.duration = duration;
        this.length = length;
        this.color = color;
        ACTIVE.put(dragonId, this);
    }

    public static void updateAim(int dragonId, Vec3 aim) {
        BreathBeamVfx beam = ACTIVE.get(dragonId);
        if (beam != null) {
            beam.prevAim = beam.aim;
            beam.aim = aim;
            beam.aimUpdated = true;
        }
    }

    public static void clearAll() {
        ACTIVE.clear();
    }

    @Override
    public boolean tick() {
        if (!this.aimUpdated) {
            this.prevAim = this.aim;
        }
        this.aimUpdated = false;
        boolean alive = super.tick();
        if (!alive && ACTIVE.get(this.dragonId) == this) {
            ACTIVE.remove(this.dragonId);
        }
        return alive;
    }

    private Vec3 origin(float partialTick) {
        EnderDragon dragon = dragon(this.dragonId);
        if (dragon != null) {
            this.lastOrigin = mouth(dragon, partialTick);
        }
        return this.lastOrigin;
    }

    private Vec3 direction(Vec3 from, float partialTick) {
        Vec3 target = this.prevAim.lerp(this.aim, partialTick);
        Vec3 dir = target.subtract(from);
        if (dir.lengthSqr() < 1.0E-4) {
            return new Vec3(0.0, -1.0, 0.0);
        }
        return dir.normalize();
    }

    private BlockHitResult clip(Vec3 from, Vec3 dir) {
        Vec3 to = from.add(dir.scale(this.length));
        return Minecraft.getInstance().level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
    }

    private float width(float partialTick) {
        float t = this.time(partialTick);
        float open = EdpMath.easeOut(t / 4.0F);
        float close = Mth.clamp((this.lifetime - t) / 10.0F, 0.0F, 1.0F);
        return open * close;
    }

    @Override
    protected void onTick() {
        if (this.age > this.duration || this.age % 5 != 0 || Minecraft.getInstance().level == null) {
            return;
        }
        Vec3 from = this.origin(1.0F);
        if (from == null) {
            return;
        }
        BlockHitResult hit = this.clip(from, this.direction(from, 1.0F));
        if (hit.getType() != HitResult.Type.MISS) {
            VfxManager.add(new DecalVfx(hit.getLocation(), 2.6F, 100, this.color, false));
        }
    }

    @Override
    public boolean distorts() {
        return true;
    }

    @Override
    public void renderDistortion(float partialTick) {
        Vec3 from = this.origin(partialTick);
        if (from == null) {
            return;
        }
        float w = this.width(partialTick);
        Vec3 end = this.renderClip(from, this.direction(from, partialTick)).getLocation();
        if (Draw.begin(Draw.Material.DISTORT)) {
            Draw.cylinder(from, end, 2.0F * w, 16, this.color, w, 1.0F);
            Draw.end(0.025F, Draw.DISTORT_LENS, 1.0F, 0.0F);
        }
    }

    @Override
    public void render(float partialTick) {
        Vec3 from = this.origin(partialTick);
        if (from == null) {
            return;
        }
        float t = this.time(partialTick);
        float w = this.width(partialTick);
        if (w <= 0.01F) {
            return;
        }
        Vec3 dir = this.direction(from, partialTick);
        BlockHitResult hit = this.renderClip(from, dir);
        Vec3 end = hit.getLocation();
        boolean hitBlock = hit.getType() != HitResult.Type.MISS;
        float len = (float) from.distanceTo(end);
        int hot = Draw.mix(this.color, 0xFFFFFF, 0.45F);
        if (Draw.begin(Draw.Material.SPHERE)) {
            Draw.cylinder(from, end, 1.25F * w, 24, this.color, 0.9F * w, len / 8.0F);
            Draw.end(Draw.SPHERE_SHELL, 1.6F, 2.0F, 0.9F);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            Draw.beam(from, end, 3.4F * w, this.color, 0.55F * w, len / 10.0F);
            Draw.beam(from, end, 1.1F * w, hot, w, len / 5.0F);
            Draw.end(1.6F, 1.0F, 1.4F, Draw.MODE_STRIP);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            Vec3[] base = Draw.basis(dir);
            for (int strand = 0; strand < 2; strand++) {
                Vec3[] points = this.helixPoints;
                float[] widths = this.helixWidths;
                float[] alphas = this.helixAlphas;
                for (int i = 0; i < HELIX_POINTS; i++) {
                    float k = i / (float) (HELIX_POINTS - 1);
                    float angle = k * len * 0.45F - t * 0.6F + strand * Mth.PI;
                    double radius = (0.4 + 1.1 * Math.min(1.0F, k * 4.0F)) * w;
                    Vec3 offset = base[0].scale(Mth.cos(angle) * radius).add(base[1].scale(Mth.sin(angle) * radius));
                    points[i] = from.add(dir.scale(len * k)).add(offset);
                    widths[i] = 0.3F * w;
                    alphas[i] = w * (1.0F - k * 0.6F);
                }
                Draw.ribbon(points, widths, alphas, hot, 0.0F, len / 4.0F);
            }
            Draw.end(2.2F, 1.0F, 2.0F, Draw.MODE_STRIP);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            Draw.billboard(from, 3.5F * w, this.color, w);
            Draw.billboard(end, 7.0F * w, this.color, w);
            Draw.billboard(end, 3.0F * w, hot, w);
            Draw.end(1.0F, 3.0F, 1.4F, Draw.MODE_RADIAL);
        }
        if (hitBlock && Draw.begin(Draw.Material.ENERGY)) {
            float pulse = (t * 0.12F) % 1.0F;
            Draw.ring(end.add(0.0, 0.2, 0.0), UP, 1.0F + 4.0F * pulse, 1.8F + 4.5F * pulse, 32, this.color, w * (1.0F - pulse), w * (1.0F - pulse), 3.0F);
            Draw.end(1.0F, 2.0F, 1.5F, Draw.MODE_RING);
        }
    }
}
