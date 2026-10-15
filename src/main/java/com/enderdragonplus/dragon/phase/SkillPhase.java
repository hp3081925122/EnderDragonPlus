package com.enderdragonplus.dragon.phase;

import com.enderdragonplus.dragon.DragonBrain;
import com.enderdragonplus.dragon.DragonBrainHolder;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.phys.Vec3;

public abstract class SkillPhase extends AbstractDragonPhaseInstance {
    protected int tick;
    @Nullable
    protected Vec3 moveTarget;
    protected double moveSpeed = 1.0;
    @Nullable
    protected Vec3 faceDirection;
    protected float turnRate = 6.0F;
    private boolean aborted;

    public SkillPhase(EnderDragon dragon) {
        super(dragon);
    }

    protected DragonBrain brain() {
        return ((DragonBrainHolder) this.dragon).edp$getBrain();
    }

    @Override
    public final void begin() {
        this.tick = 0;
        this.moveTarget = null;
        this.faceDirection = null;
        this.moveSpeed = 1.0;
        this.turnRate = 6.0F;
        this.aborted = false;
        if (!this.dragon.level().isClientSide()) {
            this.aborted = !this.start();
        }
    }

    @Override
    public final void doServerTick(net.minecraft.server.level.ServerLevel level) {
        if (this.aborted) {
            this.finish();
            return;
        }
        this.tick++;
        this.serverTick();
        if (this.dragon.getPhaseManager().getCurrentPhase() != this) {
            return;
        }
        this.move();
    }

    protected abstract boolean start();

    protected abstract void serverTick();

    protected void finish() {
        this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
    }

    private void move() {
        this.dragon.setDeltaMovement(Vec3.ZERO);
        if (this.moveTarget != null) {
            Vec3 pos = this.dragon.position();
            Vec3 delta = this.moveTarget.subtract(pos);
            double len = delta.length();
            if (len > 0.01) {
                double step = Math.min(len, Math.min(this.moveSpeed, Math.max(0.12, len * 0.08)));
                Vec3 next = pos.add(delta.scale(step / len));
                this.dragon.setPos(next.x, next.y, next.z);
                if (this.faceDirection == null && len > 2.0) {
                    this.faceYaw(delta);
                }
            }
        }
        if (this.faceDirection != null) {
            this.faceYaw(this.faceDirection);
        }
        this.dragon.yRotA = 0.0F;
    }

    protected void faceYaw(Vec3 dir) {
        if (dir.x * dir.x + dir.z * dir.z < 1.0E-6) {
            return;
        }
        float targetYaw = (float) (Mth.atan2(dir.x, -dir.z) * Mth.RAD_TO_DEG);
        float current = this.dragon.getYRot();
        float diff = Mth.wrapDegrees(targetYaw - current);
        this.dragon.setYRot(current + Mth.clamp(diff, -this.turnRate, this.turnRate));
    }

    protected boolean arrived(double distance) {
        return this.moveTarget == null || this.dragon.position().distanceTo(this.moveTarget) < distance;
    }

    @Override
    public boolean isSitting() {
        return false;
    }

    @Nullable
    @Override
    public Vec3 getFlyTargetLocation() {
        return null;
    }

    @Override
    public float getTurnSpeed() {
        return 0.0F;
    }
}
