package com.enderdragonplus.dragon.hazard;

import com.enderdragonplus.dragon.DragonBrain;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MeteorHazard extends Hazard {
    private final Vec3 impact;
    private final int impactTick;
    private final double radius;
    private final double multiplier;
    private final boolean breakBlocks;

    public MeteorHazard(Vec3 impact, int impactTick, double radius, double multiplier, boolean breakBlocks) {
        this.impact = impact;
        this.impactTick = impactTick;
        this.radius = radius;
        this.multiplier = multiplier;
        this.breakBlocks = breakBlocks;
    }

    @Override
    public boolean tick(ServerLevel level, DragonBrain brain) {
        this.age++;
        if (this.age < this.impactTick) {
            return true;
        }
        AABB box = new AABB(this.impact, this.impact).inflate(this.radius);
        for (LivingEntity victim : brain.victims(box)) {
            double dist = victim.position().distanceTo(this.impact);
            if (dist <= this.radius) {
                double falloff = 1.0 - 0.5 * dist / this.radius;
                brain.damage(victim, this.multiplier * falloff);
                brain.knockFrom(victim, this.impact, 1.2 * falloff, 0.5);
            }
        }
        level.playSound(null, this.impact.x, this.impact.y, this.impact.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 3.0F, 0.7F + level.getRandom().nextFloat() * 0.2F);
        if (this.breakBlocks) {
            level.explode(brain.dragon(), this.impact.x, this.impact.y, this.impact.z, 2.0F, Level.ExplosionInteraction.MOB);
        }
        return false;
    }
}
