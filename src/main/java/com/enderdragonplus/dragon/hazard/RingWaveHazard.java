package com.enderdragonplus.dragon.hazard;

import com.enderdragonplus.dragon.DragonBrain;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class RingWaveHazard extends Hazard {
    private static final double THICKNESS = 1.0;
    private static final double HEIGHT = 4.0;
    private final Vec3 center;
    private final int delay;
    private final double speed;
    private final double maxRadius;
    private final double multiplier;
    private final IntOpenHashSet hit = new IntOpenHashSet();

    public RingWaveHazard(Vec3 center, int delay, double speed, double maxRadius, double multiplier) {
        this.center = center;
        this.delay = delay;
        this.speed = speed;
        this.maxRadius = maxRadius;
        this.multiplier = multiplier;
    }

    @Override
    public boolean tick(ServerLevel level, DragonBrain brain) {
        this.age++;
        int local = this.age - this.delay;
        if (local < 0) {
            return true;
        }
        double r = local * this.speed;
        if (r > this.maxRadius) {
            return false;
        }
        AABB box = new AABB(this.center, this.center).inflate(r + THICKNESS, HEIGHT, r + THICKNESS);
        for (LivingEntity victim : brain.victims(box)) {
            if (this.hit.contains(victim.getId()) || !victim.onGround()) {
                continue;
            }
            if (Math.abs(victim.getY() - this.center.y) >= HEIGHT) {
                continue;
            }
            double dx = victim.getX() - this.center.x;
            double dz = victim.getZ() - this.center.z;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (Math.abs(d - r) <= THICKNESS) {
                this.hit.add(victim.getId());
                brain.damage(victim, this.multiplier);
                brain.knockFrom(victim, this.center, 0.8, 0.45);
            }
        }
        return true;
    }
}
