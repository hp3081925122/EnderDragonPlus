package com.enderdragonplus.dragon.phase;

import com.enderdragonplus.EdpConfig;
import com.enderdragonplus.EnderDragonPlus;
import com.enderdragonplus.dragon.DragonBrain;
import com.enderdragonplus.dragon.DragonPhases;
import com.enderdragonplus.dragon.EdpMath;
import com.enderdragonplus.network.VfxPayload;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class RiftDashPhase extends SkillPhase {
    public static final int TELEGRAPH_TICKS = 18;
    public static final int DASH_TICKS = 8;
    private static final int RECOVER_TICKS = 12;
    private final IntSet hitIds = new IntOpenHashSet();
    private int dashes;
    private int dashIndex;
    private int cycleStart;
    private Vec3 dashFrom;
    private Vec3 dashTo;

    public RiftDashPhase(EnderDragon dragon) {
        super(dragon);
    }

    @Override
    protected boolean start() {
        this.dashes = this.brain().stage();
        this.dashIndex = 0;
        return this.planDash(this.brain().validTarget());
    }

    private boolean planDash(LivingEntity target) {
        if (target == null) {
            return false;
        }
        DragonBrain brain = this.brain();
        Vec3 aim = target.position().add(0.0, 0.3, 0.0);
        Vec3 back = new Vec3(this.dragon.getX() - aim.x, 0.0, this.dragon.getZ() - aim.z);
        if (back.lengthSqr() < 1.0E-4) {
            back = new Vec3(1.0, 0.0, 0.0);
        }
        back = back.normalize();
        this.dashFrom = aim.add(back.scale(28.0));
        this.dashTo = aim.subtract(back.scale(26.0));
        Vec3 dir = this.dashTo.subtract(this.dashFrom).normalize();
        if (EdpConfig.DEBUG_LOGGING.get()) {
            EnderDragonPlus.LOGGER.info("Rift dash planned from {} to {} through target {} at y {}", this.dashFrom, this.dashTo, target.getName().getString(), target.getY());
        }
        this.cycleStart = this.tick;
        this.hitIds.clear();
        this.moveTarget = this.dashFrom;
        this.moveSpeed = 2.5;
        this.faceDirection = dir;
        this.turnRate = 20.0F;
        CompoundTag data = new CompoundTag();
        VfxPayload.putVec(data, "from", this.dashFrom);
        VfxPayload.putVec(data, "to", this.dashTo);
        data.putInt("telegraph", TELEGRAPH_TICKS);
        data.putInt("dash", DASH_TICKS);
        data.putInt("color", brain.stageColor());
        brain.sendVfx(VfxPayload.RIFT_DASH, data);
        brain.playSound(this.dashFrom, SoundEvents.PORTAL_TRIGGER, 3.0F, 1.6F);
        return true;
    }

    @Override
    protected void serverTick() {
        int local = this.tick - this.cycleStart;
        if (local < TELEGRAPH_TICKS) {
            this.faceDirection = this.dashTo.subtract(this.dashFrom);
            return;
        }
        if (local < TELEGRAPH_TICKS + DASH_TICKS) {
            if (local == TELEGRAPH_TICKS) {
                this.brain().playSound(this.dashFrom, SoundEvents.ENDERMAN_TELEPORT, 6.0F, 0.5F);
            }
            this.moveTarget = null;
            double k = (local - TELEGRAPH_TICKS + 1) / (double) DASH_TICKS;
            Vec3 prev = this.dragon.position();
            Vec3 next = this.dashFrom.lerp(this.dashTo, EdpMath.easeOut((float) k));
            this.dragon.setPos(next.x, next.y, next.z);
            this.hitAlong(prev, next);
            return;
        }
        if (local >= TELEGRAPH_TICKS + DASH_TICKS + RECOVER_TICKS) {
            this.dashIndex++;
            if (this.dashIndex >= this.dashes || !this.planDash(this.brain().pickTarget())) {
                this.finish();
            }
        }
    }

    private void hitAlong(Vec3 from, Vec3 to) {
        DragonBrain brain = this.brain();
        double multiplier = EdpConfig.RIFT_DASH.damage.get();
        Vec3 dir = to.subtract(from);
        for (LivingEntity victim : brain.victims(new AABB(from, to).inflate(5.0))) {
            if (this.hitIds.contains(victim.getId())) {
                continue;
            }
            Vec3 center = victim.position().add(0.0, victim.getBbHeight() * 0.5, 0.0);
            if (EdpMath.distanceToSegment(center, from, to.add(0.0, 1.5, 0.0)) <= 4.5) {
                this.hitIds.add(victim.getId());
                brain.damage(victim, multiplier);
                Vec3 side = new Vec3(-dir.z, 0.0, dir.x);
                if (side.dot(center.subtract(from)) < 0.0) {
                    side = side.scale(-1.0);
                }
                if (side.lengthSqr() > 1.0E-6) {
                    side = side.normalize();
                }
                DragonBrain.push(victim, side.x * 1.6, 0.6, side.z * 1.6);
            }
        }
    }

    @Override
    public EnderDragonPhase<RiftDashPhase> getPhase() {
        return DragonPhases.RIFT_DASH;
    }
}
