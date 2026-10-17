package com.enderdragonplus.dragon.phase;

import com.enderdragonplus.EdpConfig;
import com.enderdragonplus.dragon.DragonBrain;
import com.enderdragonplus.dragon.DragonPhases;
import com.enderdragonplus.network.VfxPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ShieldAscensionPhase extends SkillPhase {
    public static final float SHIELD_RADIUS = 9.0F;
    private static final int TRAVEL_MAX = 80;
    private static final double BURST_RADIUS = 16.0;
    private final List<EndCrystal> crystals = new ArrayList<>();
    private int shieldStart;

    public ShieldAscensionPhase(EnderDragon dragon) {
        super(dragon);
    }

    @Override
    protected boolean start() {
        this.shieldStart = -1;
        this.moveTarget = this.brain().arenaCenter().add(0.0, 24.0, 0.0);
        this.moveSpeed = 1.4;
        this.crystals.clear();
        this.reviveCrystals();
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
            brain.playSound(center, SoundEvents.GENERIC_EXPLODE, 8.0F, 0.6F);
            this.explodeCrystals();
            this.finish();
        }
    }

    private void reviveCrystals() {
        DragonBrain brain = this.brain();
        ServerLevel level = brain.level();
        if (!EdpConfig.SHIELD_REVIVE_CRYSTALS.get() || level.dimension() != Level.END) {
            return;
        }
        BlockPos beam = BlockPos.containing(this.moveTarget);
        int revived = 0;
        for (SpikeFeature.EndSpike spike : SpikeFeature.getSpikesForLevel(level)) {
            List<EndCrystal> found = level.getEntitiesOfClass(EndCrystal.class, spike.getTopBoundingBox());
            EndCrystal crystal;
            if (found.isEmpty()) {
                crystal = EntityType.END_CRYSTAL.create(level);
                if (crystal == null) {
                    continue;
                }
                crystal.moveTo(spike.getCenterX() + 0.5, spike.getHeight() + 1, spike.getCenterZ() + 0.5, this.dragon.getRandom().nextFloat() * 360.0F, 0.0F);
                level.addFreshEntity(crystal);
                BlockPos pos = crystal.blockPosition();
                level.setBlockAndUpdate(pos.below(), Blocks.BEDROCK.defaultBlockState());
                level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
                CompoundTag data = new CompoundTag();
                VfxPayload.putVec(data, "pos", crystal.position());
                data.putInt("delay", 4);
                data.putFloat("radius", 2.5F);
                data.putInt("color", brain.stageColor());
                brain.sendVfx(VfxPayload.STORM_STRIKE, data);
                brain.playSound(crystal.position(), SoundEvents.RESPAWN_ANCHOR_CHARGE, 4.0F, 0.8F);
                revived++;
            } else {
                crystal = found.get(0);
            }
            crystal.setInvulnerable(true);
            crystal.setBeamTarget(beam);
            this.crystals.add(crystal);
        }
    }

    private void explodeCrystals() {
        List<EndCrystal> alive = new ArrayList<>();
        for (EndCrystal crystal : this.crystals) {
            if (!crystal.isRemoved()) {
                crystal.setInvulnerable(false);
                crystal.setBeamTarget(null);
                alive.add(crystal);
            }
        }
        this.crystals.clear();
        alive = Util.shuffledCopy(new it.unimi.dsi.fastutil.objects.ObjectArrayList<>(alive), this.dragon.getRandom());
        int count = Mth.floor(alive.size() * EdpConfig.SHIELD_CRYSTAL_EXPLODE_RATIO.get());
        for (int i = 0; i < count; i++) {
            EndCrystal crystal = alive.get(i);
            crystal.hurt(crystal.damageSources().generic(), 1.0F);
        }
    }

    @Override
    public void end() {
        for (EndCrystal crystal : this.crystals) {
            if (!crystal.isRemoved()) {
                crystal.setInvulnerable(false);
                crystal.setBeamTarget(null);
            }
        }
        this.crystals.clear();
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
