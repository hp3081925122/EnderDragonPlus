package com.enderdragonplus.dragon.hazard;

import com.enderdragonplus.dragon.DragonBrain;
import com.enderdragonplus.dragon.EdpMath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class LanceHazard extends Hazard {
    private final Vec3 origin;
    private final Vec3 end;
    private final int charge;
    private final double radius;
    private final double multiplier;

    public LanceHazard(Vec3 origin, Vec3 end, int charge, double radius, double multiplier) {
        this.origin = origin;
        this.end = end;
        this.charge = charge;
        this.radius = radius;
        this.multiplier = multiplier;
    }

    @Override
    public boolean tick(ServerLevel level, DragonBrain brain) {
        this.age++;
        if (this.age < this.charge) {
            return true;
        }
        Vec3 dir = this.end.subtract(this.origin);
        Vec3 side = new Vec3(dir.x, 0.0, dir.z);
        if (side.lengthSqr() > 1.0E-6) {
            side = side.normalize();
        }
        for (LivingEntity victim : brain.victims(new AABB(this.origin, this.end).inflate(this.radius + 1.0))) {
            Vec3 center = victim.position().add(0.0, victim.getBbHeight() * 0.5, 0.0);
            if (EdpMath.distanceToSegment(center, this.origin, this.end) <= this.radius + victim.getBbWidth() * 0.5) {
                brain.damage(victim, this.multiplier);
                DragonBrain.push(victim, side.x * 0.5, 0.35, side.z * 0.5);
            }
        }
        level.playSound(null, this.end.x, this.end.y, this.end.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 1.3F + level.getRandom().nextFloat() * 0.2F);
        return false;
    }
}
