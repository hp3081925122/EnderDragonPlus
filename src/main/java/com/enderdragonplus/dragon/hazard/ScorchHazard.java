package com.enderdragonplus.dragon.hazard;

import com.enderdragonplus.dragon.DragonBrain;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ScorchHazard extends Hazard {
    private final Vec3 pos;
    private final double radius;
    private final int life;
    private final double multiplier;

    public ScorchHazard(Vec3 pos, double radius, int life, double multiplier) {
        this.pos = pos;
        this.radius = radius;
        this.life = life;
        this.multiplier = multiplier;
    }

    @Override
    public boolean tick(ServerLevel level, DragonBrain brain) {
        this.age++;
        if (this.age % 10 == 0) {
            AABB box = new AABB(this.pos, this.pos).inflate(this.radius, 2.0, this.radius);
            for (LivingEntity victim : brain.victims(box)) {
                double dx = victim.getX() - this.pos.x;
                double dz = victim.getZ() - this.pos.z;
                if (dx * dx + dz * dz <= this.radius * this.radius) {
                    brain.damage(victim, this.multiplier);
                }
            }
        }
        return this.age < this.life;
    }
}
