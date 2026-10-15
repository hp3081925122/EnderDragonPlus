package com.enderdragonplus.dragon.phase;

import com.enderdragonplus.EdpConfig;
import com.enderdragonplus.EnderDragonPlus;
import com.enderdragonplus.dragon.DragonBrain;
import com.enderdragonplus.dragon.DragonPhases;
import com.enderdragonplus.dragon.hazard.RingWaveHazard;
import com.enderdragonplus.network.VfxPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.phys.Vec3;

public class RingWavesPhase extends SkillPhase {
    private static final int MAX_APPROACH = 60;
    private static final int INTERVAL = 22;
    private static final double SPEED = 0.5;
    private static final double MAX_RADIUS = 48.0;
    private Vec3 center;
    private int count;
    private int slamTick = -1;

    public RingWavesPhase(EnderDragon dragon) {
        super(dragon);
    }

    @Override
    protected boolean start() {
        DragonBrain brain = this.brain();
        if (brain.combatTargets().isEmpty()) {
            return false;
        }
        this.center = brain.arenaCenter();
        this.moveTarget = this.center.add(0.0, 10.0, 0.0);
        this.moveSpeed = 1.6;
        this.count = brain.stage() + 1;
        this.slamTick = -1;
        brain.playSound(this.dragon.position(), SoundEvents.ENDER_DRAGON_GROWL, 8.0F, 0.6F);
        return true;
    }

    @Override
    protected void serverTick() {
        if (this.slamTick < 0) {
            if (this.arrived(3.0) || this.tick >= MAX_APPROACH) {
                this.slam();
            }
            return;
        }
        if (this.tick >= this.slamTick + INTERVAL * (this.count - 1) + 20) {
            this.finish();
        }
    }

    private void slam() {
        DragonBrain brain = this.brain();
        this.slamTick = this.tick;
        this.moveTarget = null;
        double multiplier = EdpConfig.RING_WAVES.damage.get();
        for (int i = 0; i < this.count; i++) {
            brain.addHazard(new RingWaveHazard(this.center, i * INTERVAL, SPEED, MAX_RADIUS, multiplier));
        }
        CompoundTag data = new CompoundTag();
        VfxPayload.putVec(data, "center", this.center);
        data.putInt("count", this.count);
        data.putInt("interval", INTERVAL);
        data.putFloat("speed", (float) SPEED);
        data.putFloat("maxRadius", (float) MAX_RADIUS);
        data.putInt("color", brain.stageColor());
        brain.sendVfx(VfxPayload.RING_WAVES, data);
        CompoundTag wave = new CompoundTag();
        VfxPayload.putVec(wave, "pos", this.center);
        wave.putFloat("radius", 10.0F);
        wave.putInt("duration", 16);
        wave.putInt("color", brain.stageColor());
        wave.putBoolean("sphere", false);
        wave.putFloat("shake", 1.6F);
        wave.putFloat("flash", 0.0F);
        brain.sendVfx(VfxPayload.SHOCKWAVE, wave);
        brain.playSound(this.center, SoundEvents.GENERIC_EXPLODE.value(), 6.0F, 0.5F);
        brain.playSound(this.center, SoundEvents.WARDEN_SONIC_BOOM, 6.0F, 0.6F);
        if (EdpConfig.DEBUG_LOGGING.get()) {
            EnderDragonPlus.LOGGER.info("Ring waves slam at {} with {} rings", this.center, this.count);
        }
    }

    @Override
    public EnderDragonPhase<RingWavesPhase> getPhase() {
        return DragonPhases.RING_WAVES;
    }
}
