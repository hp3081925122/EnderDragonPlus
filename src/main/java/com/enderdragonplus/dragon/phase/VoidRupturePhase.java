package com.enderdragonplus.dragon.phase;

import com.enderdragonplus.EdpConfig;
import com.enderdragonplus.EnderDragonPlus;
import com.enderdragonplus.dragon.DragonBrain;
import com.enderdragonplus.dragon.DragonPhases;
import com.enderdragonplus.dragon.hazard.BlastHazard;
import com.enderdragonplus.network.VfxPayload;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.phys.Vec3;

public class VoidRupturePhase extends SkillPhase {
    private static final int CAST_TICK = 16;
    private static final int FIRST_DELAY = 18;
    private static final double STEP = 3.0;
    private static final double MAX_LENGTH = 48.0;
    private static final double RADIUS = 2.0;
    private int duration;

    public VoidRupturePhase(EnderDragon dragon) {
        super(dragon);
    }

    @Override
    protected boolean start() {
        DragonBrain brain = this.brain();
        if (brain.combatTargets().isEmpty()) {
            return false;
        }
        double minY = brain.groundY(this.dragon.getX(), this.dragon.getZ(), this.dragon.getY()) + 14.0;
        this.moveTarget = new Vec3(this.dragon.getX(), Math.max(this.dragon.getY(), minY), this.dragon.getZ());
        this.moveSpeed = 0.7;
        this.duration = CAST_TICK + 40;
        brain.playSound(this.dragon.position(), SoundEvents.ENDER_DRAGON_GROWL, 6.0F, 0.8F);
        return true;
    }

    @Override
    protected void serverTick() {
        LivingEntity nearest = this.nearest();
        if (nearest != null) {
            this.faceDirection = nearest.position().subtract(this.dragon.position());
        }
        if (this.tick == CAST_TICK) {
            this.cast();
        }
        if (this.tick >= this.duration) {
            this.finish();
        }
    }

    @Nullable
    private LivingEntity nearest() {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity player : this.brain().combatTargets()) {
            double d = player.distanceToSqr(this.dragon);
            if (d < bestDist) {
                bestDist = d;
                best = player;
            }
        }
        return best;
    }

    private void cast() {
        DragonBrain brain = this.brain();
        List<LivingEntity> players = brain.combatTargets();
        if (players.isEmpty()) {
            return;
        }
        double multiplier = EdpConfig.VOID_RUPTURE.damage.get();
        Vec3 origin = new Vec3(this.dragon.getX(), brain.groundY(this.dragon.getX(), this.dragon.getZ(), this.dragon.getY() - 14.0), this.dragon.getZ());
        ListTag points = new ListTag();
        int maxDelay = 0;
        int count = Math.min(4, players.size());
        for (int p = 0; p < count; p++) {
            LivingEntity player = players.get(p);
            Vec3 flat = new Vec3(player.getX() - origin.x, 0.0, player.getZ() - origin.z);
            double dist = flat.length();
            if (dist < 1.0E-3) {
                float a = this.dragon.getRandom().nextFloat() * Mth.TWO_PI;
                flat = new Vec3(Mth.cos(a), 0.0, Mth.sin(a));
                dist = 0.0;
            }
            Vec3 dir = flat.normalize();
            double length = Math.min(MAX_LENGTH, dist + 12.0);
            int lines = 1;
            if (brain.stage() >= 2) {
                lines = 3;
            }
            for (int l = 0; l < lines; l++) {
                float angle = 0.0F;
                if (l == 1) {
                    angle = 25.0F * Mth.DEG_TO_RAD;
                } else if (l == 2) {
                    angle = -25.0F * Mth.DEG_TO_RAD;
                }
                Vec3 lineDir = dir.yRot(angle);
                int i = 0;
                for (double s = STEP; s <= length; s += STEP) {
                    double x = origin.x + lineDir.x * s;
                    double z = origin.z + lineDir.z * s;
                    Vec3 pos = new Vec3(x, brain.groundY(x, z, origin.y), z);
                    int delay = FIRST_DELAY + i * 2;
                    SoundEvent sound = null;
                    if (i % 3 == 0) {
                        sound = SoundEvents.DRAGON_FIREBALL_EXPLODE;
                    }
                    brain.addHazard(new BlastHazard(pos, delay, RADIUS, multiplier, 0.2, 0.9, sound, 2.0F, 0.9F));
                    CompoundTag point = new CompoundTag();
                    VfxPayload.putVec(point, "p", pos);
                    point.putInt("delay", delay);
                    points.add(point);
                    maxDelay = Math.max(maxDelay, delay);
                    i++;
                }
            }
        }
        this.duration = CAST_TICK + maxDelay + 10;
        CompoundTag data = new CompoundTag();
        data.put("points", points);
        data.putFloat("radius", (float) RADIUS);
        data.putInt("color", brain.stageColor());
        brain.sendVfx(VfxPayload.RUPTURE, data);
        brain.playSound(origin, SoundEvents.EVOKER_CAST_SPELL, 5.0F, 0.6F);
        if (EdpConfig.DEBUG_LOGGING.get()) {
            EnderDragonPlus.LOGGER.info("Void rupture cast with {} points, last delay {}", points.size(), maxDelay);
        }
    }

    @Override
    public EnderDragonPhase<VoidRupturePhase> getPhase() {
        return DragonPhases.VOID_RUPTURE;
    }
}
