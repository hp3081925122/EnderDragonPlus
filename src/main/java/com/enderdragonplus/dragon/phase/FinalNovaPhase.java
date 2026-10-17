package com.enderdragonplus.dragon.phase;

import com.enderdragonplus.EdpConfig;
import com.enderdragonplus.dragon.DragonBrain;
import com.enderdragonplus.dragon.DragonPhases;
import com.enderdragonplus.network.VfxPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class FinalNovaPhase extends SkillPhase {
    public static final double BLAST_RADIUS = 80.0;
    private static final int TRAVEL_MAX = 70;
    private final List<Vec3> safeZones = new ArrayList<>();
    private Vec3 center;
    private int chargeStart;
    private int chargeTicks;
    private double safeRadius;

    public FinalNovaPhase(EnderDragon dragon) {
        super(dragon);
    }

    @Override
    protected boolean start() {
        this.chargeStart = -1;
        this.center = this.brain().arenaCenter();
        this.moveTarget = this.center.add(0.0, 28.0, 0.0);
        this.moveSpeed = 1.4;
        this.brain().playSound(this.dragon.position(), SoundEvents.ENDER_DRAGON_GROWL, 12.0F, 0.4F);
        return true;
    }

    @Override
    protected void serverTick() {
        DragonBrain brain = this.brain();
        if (this.chargeStart < 0) {
            if (this.arrived(3.0) || this.tick > TRAVEL_MAX) {
                this.beginCharge();
            }
            return;
        }
        int local = this.tick - this.chargeStart;
        float spin = local * 0.03F;
        this.faceDirection = new Vec3(Mth.sin(spin), 0.0, -Mth.cos(spin));
        this.turnRate = 2.0F;
        if (local == this.chargeTicks) {
            this.detonate();
        }
        if (local >= this.chargeTicks + 25) {
            this.finish();
        }
    }

    private void beginCharge() {
        DragonBrain brain = this.brain();
        this.chargeStart = this.tick;
        this.chargeTicks = EdpConfig.NOVA_CHARGE_TICKS.get();
        this.safeRadius = EdpConfig.NOVA_SAFE_RADIUS.get();
        this.safeZones.clear();
        RandomSource random = this.dragon.getRandom();
        int count = EdpConfig.NOVA_SAFE_ZONES.get();
        for (int i = 0; i < count; i++) {
            Vec3 chosen = null;
            for (int attempt = 0; attempt < 30; attempt++) {
                float angle = random.nextFloat() * Mth.TWO_PI;
                double r = 10.0 + random.nextDouble() * 22.0;
                double x = this.center.x + Mth.cos(angle) * r;
                double z = this.center.z + Mth.sin(angle) * r;
                Vec3 candidate = new Vec3(x, brain.groundY(x, z, this.center.y), z);
                chosen = candidate;
                if (Math.abs(candidate.y - this.center.y) <= 5.0 && this.farFromZones(candidate)) {
                    break;
                }
            }
            this.safeZones.add(chosen);
        }
        ListTag zones = new ListTag();
        for (Vec3 zone : this.safeZones) {
            CompoundTag z = new CompoundTag();
            VfxPayload.putVec(z, "p", zone);
            zones.add(z);
        }
        CompoundTag data = new CompoundTag();
        VfxPayload.putVec(data, "center", this.center);
        data.put("zones", zones);
        data.putFloat("safeRadius", (float) this.safeRadius);
        data.putInt("charge", this.chargeTicks);
        data.putFloat("blast", (float) BLAST_RADIUS);
        data.putInt("color", brain.stageColor());
        brain.sendVfx(VfxPayload.NOVA, data);
        brain.playSound(this.dragon.position(), SoundEvents.WITHER_SPAWN, 10.0F, 0.6F);
        brain.playSound(this.dragon.position(), SoundEvents.BEACON_POWER_SELECT, 10.0F, 0.5F);
    }

    private boolean farFromZones(Vec3 candidate) {
        double min = this.safeRadius * 2.5;
        for (Vec3 zone : this.safeZones) {
            if (zone.distanceTo(candidate) < min) {
                return false;
            }
        }
        return true;
    }

    private void detonate() {
        DragonBrain brain = this.brain();
        double multiplier = EdpConfig.FINAL_NOVA.damage.get();
        int hits = 0;
        for (LivingEntity victim : brain.victims(new AABB(this.center, this.center).inflate(BLAST_RADIUS, 60.0, BLAST_RADIUS))) {
            if (this.isSafe(victim)) {
                continue;
            }
            brain.damage(victim, multiplier);
            brain.knockFrom(victim, this.center, 1.4, 0.8);
            hits++;
        }

        brain.playSound(this.dragon.position(), SoundEvents.GENERIC_EXPLODE, 16.0F, 0.4F);
        brain.playSound(this.dragon.position(), SoundEvents.WARDEN_SONIC_BOOM, 16.0F, 0.5F);
    }

    private boolean isSafe(LivingEntity victim) {
        for (Vec3 zone : this.safeZones) {
            double dx = victim.getX() - zone.x;
            double dz = victim.getZ() - zone.z;
            double dy = victim.getY() - zone.y;
            if (dx * dx + dz * dz <= (this.safeRadius + 0.3) * (this.safeRadius + 0.3) && dy > -3.0 && dy < 8.0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public float onHurt(DamageSource source, float amount) {
        if (this.chargeStart >= 0) {
            return amount * 0.5F;
        }
        return amount;
    }

    @Override
    public EnderDragonPhase<FinalNovaPhase> getPhase() {
        return DragonPhases.FINAL_NOVA;
    }
}
