package com.enderdragonplus.dragon.hazard;

import com.enderdragonplus.dragon.DragonBrain;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class BlastHazard extends Hazard {
    private final Vec3 pos;
    private final int delay;
    private final double radius;
    private final double multiplier;
    private final double knockHorizontal;
    private final double knockVertical;
    @Nullable
    private final SoundEvent sound;
    private final float volume;
    private final float pitch;

    public BlastHazard(Vec3 pos, int delay, double radius, double multiplier, double knockHorizontal, double knockVertical, @Nullable SoundEvent sound, float volume, float pitch) {
        this.pos = pos;
        this.delay = delay;
        this.radius = radius;
        this.multiplier = multiplier;
        this.knockHorizontal = knockHorizontal;
        this.knockVertical = knockVertical;
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public boolean tick(ServerLevel level, DragonBrain brain) {
        this.age++;
        if (this.age < this.delay) {
            return true;
        }
        AABB box = new AABB(this.pos, this.pos).inflate(this.radius, this.radius + 1.0, this.radius);
        for (LivingEntity victim : brain.victims(box)) {
            Vec3 center = victim.position().add(0.0, victim.getBbHeight() * 0.5, 0.0);
            double dist = Math.min(center.distanceTo(this.pos), victim.position().distanceTo(this.pos));
            if (dist <= this.radius) {
                double falloff = 1.0 - 0.4 * dist / this.radius;
                brain.damage(victim, this.multiplier * falloff);
                brain.knockFrom(victim, this.pos, this.knockHorizontal * falloff, this.knockVertical);
            }
        }
        if (this.sound != null) {
            level.playSound(null, this.pos.x, this.pos.y, this.pos.z, this.sound, SoundSource.HOSTILE, this.volume, this.pitch + level.getRandom().nextFloat() * 0.15F);
        }
        return false;
    }
}
