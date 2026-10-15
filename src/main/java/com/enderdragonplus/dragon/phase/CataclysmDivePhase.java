package com.enderdragonplus.dragon.phase;

import com.enderdragonplus.EdpConfig;
import com.enderdragonplus.EnderDragonPlus;
import com.enderdragonplus.dragon.DragonBrain;
import com.enderdragonplus.dragon.DragonPhases;
import com.enderdragonplus.dragon.EdpMath;
import com.enderdragonplus.dragon.hazard.ScorchHazard;
import com.enderdragonplus.network.VfxPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CataclysmDivePhase extends SkillPhase {
    private static final int LOCK_TICK = 30;
    private static final int WARN = 20;
    private static final int DIVE = 8;
    private static final int END_TICK = 80;
    private static final double RISE = 36.0;
    private LivingEntity target;
    private Vec3 mark;
    private Vec3 diveFrom;
    private boolean impacted;

    public CataclysmDivePhase(EnderDragon dragon) {
        super(dragon);
    }

    @Override
    protected boolean start() {
        DragonBrain brain = this.brain();
        this.target = brain.validTarget();
        if (this.target == null) {
            return false;
        }
        this.mark = null;
        this.diveFrom = null;
        this.impacted = false;
        this.moveSpeed = 1.8;
        this.turnRate = 12.0F;
        this.trackTarget();
        brain.playSound(this.dragon.position(), SoundEvents.ENDER_DRAGON_GROWL, 10.0F, 0.5F);
        return true;
    }

    private Vec3 ground(double x, double z, double fallback) {
        return new Vec3(x, this.brain().groundY(x, z, fallback), z);
    }

    private void trackTarget() {
        this.moveTarget = this.ground(this.target.getX(), this.target.getZ(), this.target.getY()).add(0.0, RISE, 0.0);
    }

    @Override
    protected void serverTick() {
        int diveStart = LOCK_TICK + WARN;
        if (this.tick < LOCK_TICK) {
            if (this.target.isAlive()) {
                this.trackTarget();
            }
            return;
        }
        if (this.tick == LOCK_TICK) {
            this.lockMark();
            return;
        }
        if (this.tick < diveStart) {
            this.faceDirection = this.mark.subtract(this.dragon.position());
            return;
        }
        if (this.tick == diveStart) {
            this.moveTarget = null;
            this.diveFrom = this.dragon.position();
            this.brain().playSound(this.diveFrom, SoundEvents.ENDER_DRAGON_FLAP, 10.0F, 0.5F);
        }
        if (this.tick <= diveStart + DIVE) {
            float k = (this.tick - diveStart) / (float) DIVE;
            Vec3 next = this.diveFrom.lerp(this.mark.add(0.0, 2.0, 0.0), EdpMath.easeIn(k));
            this.dragon.setPos(next.x, next.y, next.z);
            if (this.tick == diveStart + DIVE) {
                this.impact();
            }
            return;
        }
        if (this.moveTarget == null) {
            this.faceDirection = null;
            this.moveTarget = this.mark.add(0.0, 20.0, 0.0);
            this.moveSpeed = 0.9;
        }
        if (this.tick >= END_TICK) {
            this.finish();
        }
    }

    private void lockMark() {
        DragonBrain brain = this.brain();
        double x = this.target.getX() + this.target.getDeltaMovement().x * 4.0;
        double z = this.target.getZ() + this.target.getDeltaMovement().z * 4.0;
        this.mark = this.ground(x, z, this.target.getY());
        this.moveTarget = this.mark.add(0.0, RISE, 0.0);
        this.moveSpeed = 0.8;
        float radius = EdpConfig.DIVE_RADIUS.get().floatValue();
        CompoundTag data = new CompoundTag();
        VfxPayload.putVec(data, "mark", this.mark);
        data.putInt("warn", WARN);
        data.putInt("dive", DIVE);
        data.putFloat("radius", radius);
        data.putInt("color", brain.stageColor());
        brain.sendVfx(VfxPayload.DIVE, data);
        brain.playSound(this.mark, SoundEvents.BEACON_POWER_SELECT, 6.0F, 0.5F);
        if (EdpConfig.DEBUG_LOGGING.get()) {
            EnderDragonPlus.LOGGER.info("Cataclysm dive locked at {}", this.mark);
        }
    }

    private void impact() {
        if (this.impacted) {
            return;
        }
        this.impacted = true;
        DragonBrain brain = this.brain();
        double radius = EdpConfig.DIVE_RADIUS.get();
        double multiplier = EdpConfig.CATACLYSM_DIVE.damage.get();
        for (LivingEntity victim : brain.victims(new AABB(this.mark, this.mark).inflate(radius, radius * 0.6 + 2.0, radius))) {
            double dist = victim.position().distanceTo(this.mark);
            if (dist <= radius) {
                double falloff = 1.0 - 0.5 * dist / radius;
                brain.damage(victim, multiplier * falloff);
                brain.knockFrom(victim, this.mark, 2.0 * falloff, 0.9);
            }
        }
        double scorch = EdpConfig.BREATH_SCORCH_MULTIPLIER.get();
        for (int i = 0; i < 6; i++) {
            float a = i * Mth.TWO_PI / 6.0F;
            double x = this.mark.x + Mth.cos(a) * 5.0;
            double z = this.mark.z + Mth.sin(a) * 5.0;
            brain.addHazard(new ScorchHazard(this.ground(x, z, this.mark.y), 2.5, 100, scorch));
        }
        CompoundTag wave = new CompoundTag();
        VfxPayload.putVec(wave, "pos", this.mark);
        wave.putFloat("radius", (float) radius * 2.0F);
        wave.putInt("duration", 26);
        wave.putInt("color", brain.stageColor());
        wave.putBoolean("sphere", true);
        wave.putFloat("shake", 2.5F);
        wave.putFloat("flash", 0.5F);
        brain.sendVfx(VfxPayload.SHOCKWAVE, wave);
        brain.playSound(this.mark, SoundEvents.GENERIC_EXPLODE.value(), 10.0F, 0.5F);
        brain.playSound(this.mark, SoundEvents.DRAGON_FIREBALL_EXPLODE, 8.0F, 0.6F);
    }

    @Override
    public EnderDragonPhase<CataclysmDivePhase> getPhase() {
        return DragonPhases.CATACLYSM_DIVE;
    }
}
