package com.enderdragonplus.dragon.phase;

import com.enderdragonplus.EdpConfig;
import com.enderdragonplus.dragon.DragonBrain;
import com.enderdragonplus.dragon.DragonPhases;
import com.enderdragonplus.network.VfxPayload;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BlackHolePhase extends SkillPhase {
    public static final int FORM_TICKS = 20;
    public static final int COLLAPSE_TICKS = 12;
    private Vec3 center;
    private int activeTicks;
    private double radius;

    public BlackHolePhase(EnderDragon dragon) {
        super(dragon);
    }

    @Override
    protected boolean start() {
        DragonBrain brain = this.brain();
        List<LivingEntity> players = brain.combatTargets();
        if (players.isEmpty()) {
            return false;
        }
        double sx = 0.0;
        double sz = 0.0;
        double sy = 0.0;
        for (LivingEntity player : players) {
            sx += player.getX();
            sy += player.getY();
            sz += player.getZ();
        }
        int n = players.size();
        Vec3 centroid = new Vec3(sx / n, sy / n, sz / n);
        Vec3 arena = brain.arenaCenter();
        Vec3 flat = new Vec3(centroid.x + (arena.x - centroid.x) * 0.35, 0.0, centroid.z + (arena.z - centroid.z) * 0.35);
        this.center = new Vec3(flat.x, brain.groundY(flat.x, flat.z, centroid.y) + 7.0, flat.z);
        Vec3 away = new Vec3(this.dragon.getX() - this.center.x, 0.0, this.dragon.getZ() - this.center.z);
        if (away.lengthSqr() < 1.0) {
            away = new Vec3(1.0, 0.0, 0.0);
        }
        away = away.normalize();
        this.moveTarget = this.center.add(away.scale(24.0)).add(0.0, 12.0, 0.0);
        this.moveSpeed = 1.2;
        this.radius = EdpConfig.BLACK_HOLE_RADIUS.get();
        this.activeTicks = 120;
        if (brain.stage() == 3) {
            this.activeTicks = 150;
        }
        CompoundTag data = new CompoundTag();
        VfxPayload.putVec(data, "pos", this.center);
        data.putInt("form", FORM_TICKS);
        data.putInt("active", this.activeTicks);
        data.putInt("collapse", COLLAPSE_TICKS);
        data.putFloat("radius", (float) this.radius);
        data.putInt("color", brain.stageColor());
        brain.sendVfx(VfxPayload.BLACK_HOLE, data);
        brain.playSound(this.center, SoundEvents.WARDEN_SONIC_CHARGE, 8.0F, 0.4F);
        brain.playSound(this.center, SoundEvents.BEACON_DEACTIVATE, 8.0F, 0.5F);
        return true;
    }

    @Override
    protected void serverTick() {
        DragonBrain brain = this.brain();
        this.faceDirection = this.center.subtract(this.dragon.position());
        int collapseAt = FORM_TICKS + this.activeTicks;
        if (this.tick >= FORM_TICKS && this.tick < collapseAt) {
            double pull = EdpConfig.BLACK_HOLE_PULL.get();
            double multiplier = EdpConfig.BLACK_HOLE.damage.get();
            for (LivingEntity victim : brain.victims(new AABB(this.center, this.center).inflate(this.radius))) {
                Vec3 to = this.center.subtract(victim.position().add(0.0, victim.getBbHeight() * 0.5, 0.0));
                double dist = to.length();
                if (dist > this.radius || dist < 1.0E-3) {
                    continue;
                }
                double strength = pull * (0.35 + 0.65 * (1.0 - dist / this.radius));
                DragonBrain.pull(victim, to.scale(strength / dist));
                if (dist < 3.0 && this.tick % 10 == 0) {
                    brain.damage(victim, multiplier);
                }
            }
            if (this.tick % 40 == 0) {
                brain.playSound(this.center, SoundEvents.PORTAL_TRIGGER, 2.0F, 0.5F);
            }
            return;
        }
        if (this.tick == collapseAt + COLLAPSE_TICKS) {
            double multiplier = EdpConfig.BLACK_HOLE.damage.get() * 2.5;
            for (LivingEntity victim : brain.victims(new AABB(this.center, this.center).inflate(9.0))) {
                if (victim.position().distanceTo(this.center) <= 9.0) {
                    brain.damage(victim, multiplier);
                    brain.knockFrom(victim, this.center, 1.6, 0.7);
                }
            }
            brain.playSound(this.center, SoundEvents.GENERIC_EXPLODE.value(), 6.0F, 0.5F);
        }
        if (this.tick >= collapseAt + COLLAPSE_TICKS + 10) {
            this.finish();
        }
    }

    @Override
    public EnderDragonPhase<BlackHolePhase> getPhase() {
        return DragonPhases.BLACK_HOLE;
    }
}
