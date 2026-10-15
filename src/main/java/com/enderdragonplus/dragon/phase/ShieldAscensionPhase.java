package com.enderdragonplus.dragon.phase;

import com.enderdragonplus.EdpConfig;
import com.enderdragonplus.dragon.DragonBrain;
import com.enderdragonplus.dragon.DragonPhases;
import com.enderdragonplus.network.VfxPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ShieldAscensionPhase extends SkillPhase {
    public static final float SHIELD_RADIUS = 9.0F;
    private static final int TRAVEL_MAX = 80;
    private static final double BURST_RADIUS = 16.0;
    private int shieldStart;

    public ShieldAscensionPhase(EnderDragon dragon) {
        super(dragon);
    }

    @Override
    protected boolean start() {
        this.shieldStart = -1;
        this.moveTarget = this.brain().arenaCenter().add(0.0, 24.0, 0.0);
        this.moveSpeed = 1.4;
        this.brain().playSound(this.dragon.position(), SoundEvents.ENDER_DRAGON_GROWL, 10.0F, 0.5F);
        return true;
    }

    @Override
    protected void serverTick() {
        DragonBrain brain = this.brain();
        if (this.shieldStart < 0) {
            if (this.arrived(3.0) || this.tick > TRAVEL_MAX) {
                this.shieldStart = this.tick;
                CompoundTag data = new CompoundTag();
                data.putInt("duration", EdpConfig.SHIELD_DURATION.get());
                data.putFloat("radius", SHIELD_RADIUS);
                data.putInt("color", brain.stageColor());
                brain.sendVfx(VfxPayload.SHIELD, data);
                brain.playSound(this.dragon.position(), SoundEvents.BEACON_ACTIVATE, 8.0F, 0.6F);
            }
            return;
        }
        int local = this.tick - this.shieldStart;
        float spin = local * 0.04F;
        this.faceDirection = new Vec3(Mth.sin(spin), 0.0, -Mth.cos(spin));
        this.turnRate = 3.0F;
        if (local >= EdpConfig.SHIELD_DURATION.get()) {
            Vec3 center = brain.bodyCenter();
            double multiplier = EdpConfig.SHIELD_BURST_MULTIPLIER.get();
            for (LivingEntity victim : brain.victims(new AABB(center, center).inflate(BURST_RADIUS))) {
                if (victim.position().distanceTo(center) <= BURST_RADIUS) {
                    brain.damage(victim, multiplier);
                    brain.knockFrom(victim, center, 2.2, 0.6);
                }
            }
            CompoundTag data = new CompoundTag();
            VfxPayload.putVec(data, "pos", center);
            data.putFloat("radius", (float) BURST_RADIUS + 4.0F);
            data.putInt("duration", 22);
            data.putInt("color", brain.stageColor());
            data.putFloat("flash", 0.35F);
            data.putFloat("shake", 1.6F);
            data.putBoolean("sphere", true);
            brain.sendVfx(VfxPayload.SHOCKWAVE, data);
            brain.playSound(center, SoundEvents.GENERIC_EXPLODE.value(), 8.0F, 0.6F);
            this.finish();
        }
    }

    @Override
    public float onHurt(DamageSource source, float amount) {
        return 0.0F;
    }

    @Override
    public EnderDragonPhase<ShieldAscensionPhase> getPhase() {
        return DragonPhases.SHIELD_ASCENSION;
    }
}
