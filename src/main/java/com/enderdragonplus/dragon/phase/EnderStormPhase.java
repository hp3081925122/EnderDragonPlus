package com.enderdragonplus.dragon.phase;

import com.enderdragonplus.EdpConfig;
import com.enderdragonplus.dragon.DragonBrain;
import com.enderdragonplus.dragon.DragonPhases;
import com.enderdragonplus.dragon.hazard.BlastHazard;
import com.enderdragonplus.network.VfxPayload;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.phys.Vec3;

public class EnderStormPhase extends SkillPhase {
    private static final int FIRST_STRIKE = 20;
    private static final int INTERVAL = 4;
    private static final int STRIKE_DELAY = 20;
    private static final double RADIUS = 2.8;
    private int strikes;
    private int fired;

    public EnderStormPhase(EnderDragon dragon) {
        super(dragon);
    }

    @Override
    protected boolean start() {
        DragonBrain brain = this.brain();
        if (brain.combatTargets().isEmpty()) {
            return false;
        }
        this.moveTarget = this.dragon.position().add(0.0, 12.0, 0.0);
        this.moveSpeed = 0.6;
        this.strikes = EdpConfig.STORM_STRIKES.get();
        if (brain.stage() >= 3) {
            this.strikes += 4;
        }
        this.fired = 0;
        brain.playSound(this.dragon.position(), SoundEvents.ENDER_DRAGON_GROWL, 8.0F, 0.7F);
        brain.playSound(this.dragon.position(), SoundEvents.ILLUSIONER_CAST_SPELL, 6.0F, 0.5F);
        return true;
    }

    @Override
    protected void serverTick() {
        if (this.fired < this.strikes && this.tick >= FIRST_STRIKE && (this.tick - FIRST_STRIKE) % INTERVAL == 0) {
            List<LivingEntity> players = this.brain().combatTargets();
            if (!players.isEmpty()) {
                this.strike(players.get(this.fired % players.size()));
            }
            this.fired++;
        }
        if (this.fired >= this.strikes && this.tick >= FIRST_STRIKE + INTERVAL * this.strikes + STRIKE_DELAY + 10) {
            this.finish();
        }
    }

    private void strike(LivingEntity player) {
        DragonBrain brain = this.brain();
        RandomSource random = this.dragon.getRandom();
        this.faceDirection = player.position().subtract(this.dragon.position());
        double x = player.getX();
        double z = player.getZ();
        if (this.fired % 2 == 0) {
            x += player.getDeltaMovement().x * 8.0;
            z += player.getDeltaMovement().z * 8.0;
        } else {
            x += (random.nextDouble() - 0.5) * 6.0;
            z += (random.nextDouble() - 0.5) * 6.0;
        }
        Vec3 pos = new Vec3(x, brain.groundY(x, z, player.getY()), z);
        brain.addHazard(new BlastHazard(pos, STRIKE_DELAY, RADIUS, EdpConfig.ENDER_STORM.damage.get(), 0.4, 0.6, SoundEvents.LIGHTNING_BOLT_IMPACT, 3.0F, 0.8F));
        CompoundTag data = new CompoundTag();
        VfxPayload.putVec(data, "pos", pos);
        data.putInt("delay", STRIKE_DELAY);
        data.putFloat("radius", (float) RADIUS);
        data.putInt("color", brain.stageColor());
        brain.sendVfx(VfxPayload.STORM_STRIKE, data);
        if (this.fired % 3 == 0) {
            brain.playSound(pos, SoundEvents.LIGHTNING_BOLT_THUNDER, 4.0F, 1.2F);
        }
    }

    @Override
    public EnderDragonPhase<EnderStormPhase> getPhase() {
        return DragonPhases.ENDER_STORM;
    }
}
