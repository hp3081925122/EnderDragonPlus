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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class VoidBreathPhase extends SkillPhase {
    public static final int CHARGE_TICKS = 30;
    public static final double BEAM_LENGTH = 64.0;
    private static final double START_OFFSET = 12.0;
    private LivingEntity target;
    private Vec3 aim;
    private int beamTicks;

    public VoidBreathPhase(EnderDragon dragon) {
        super(dragon);
    }

    @Override
    protected boolean start() {
        DragonBrain brain = this.brain();
        this.target = brain.validTarget();
        if (this.target == null) {
            return false;
        }
        Vec3 tp = this.target.position();
        Vec3 dp = this.dragon.position();
        Vec3 flat = new Vec3(dp.x - tp.x, 0.0, dp.z - tp.z);
        double dist = flat.length();
        if (dist < 1.0) {
            flat = new Vec3(1.0, 0.0, 0.0);
        } else {
            flat = flat.scale(1.0 / dist);
        }
        double want = Mth.clamp(dist, 20.0, 30.0);
        this.moveTarget = new Vec3(tp.x + flat.x * want, tp.y + 16.0, tp.z + flat.z * want);
        this.moveSpeed = 1.2;
        this.beamTicks = 70;
        if (brain.stage() == 3) {
            this.beamTicks = 90;
        }
        CompoundTag data = new CompoundTag();
        data.putInt("duration", CHARGE_TICKS);
        data.putInt("color", brain.stageColor());
        brain.sendVfx(VfxPayload.BREATH_CHARGE, data);
        brain.playSound(this.dragon.position(), SoundEvents.ENDER_DRAGON_GROWL, 6.0F, 0.7F);
        return true;
    }

    private void stepAim() {
        if (!this.target.isAlive()) {
            return;
        }
        Vec3 goal = this.target.position();
        double dx = goal.x - this.aim.x;
        double dz = goal.z - this.aim.z;
        double flatDist = Math.sqrt(dx * dx + dz * dz);
        double speed = EdpConfig.BREATH_TRACK_SPEED.get();
        double x = goal.x;
        double z = goal.z;
        if (flatDist > speed) {
            x = this.aim.x + dx / flatDist * speed;
            z = this.aim.z + dz / flatDist * speed;
        }
        double y = this.aim.y + Mth.clamp(goal.y - this.aim.y, -0.6, 0.6);
        this.aim = new Vec3(x, y, z);
    }

    @Override
    protected void serverTick() {
        DragonBrain brain = this.brain();
        if (this.tick < CHARGE_TICKS) {
            if (!this.target.isAlive()) {
                this.finish();
                return;
            }
            this.faceDirection = this.target.position().subtract(this.dragon.position());
            return;
        }
        if (this.tick == CHARGE_TICKS) {
            this.moveTarget = null;
            Vec3 tp = this.target.position();
            Vec3 toDragon = new Vec3(this.dragon.getX() - tp.x, 0.0, this.dragon.getZ() - tp.z);
            if (toDragon.lengthSqr() < 1.0E-4) {
                toDragon = new Vec3(1.0, 0.0, 0.0);
            }
            Vec3 start = tp.add(toDragon.normalize().scale(START_OFFSET));
            this.aim = new Vec3(start.x, brain.groundY(start.x, start.z, tp.y), start.z);
            CompoundTag data = new CompoundTag();
            VfxPayload.putVec(data, "aim", this.aim);
            data.putInt("duration", this.beamTicks);
            data.putDouble("length", BEAM_LENGTH);
            data.putInt("color", brain.stageColor());
            brain.sendVfx(VfxPayload.BREATH_BEAM, data);
            brain.playSound(brain.mouthPos(), SoundEvents.WARDEN_SONIC_BOOM, 5.0F, 0.6F);
        }
        int local = this.tick - CHARGE_TICKS;
        if (local < this.beamTicks) {
            if (local > 0) {
                this.stepAim();
                CompoundTag aimData = new CompoundTag();
                VfxPayload.putVec(aimData, "aim", this.aim);
                brain.sendVfx(VfxPayload.BREATH_AIM, aimData);
            }
            Vec3 mouth = brain.mouthPos();
            Vec3 dir = this.aim.subtract(mouth);
            if (dir.lengthSqr() < 1.0E-4) {
                dir = new Vec3(0.0, -1.0, 0.0);
            }
            dir = dir.normalize();
            this.faceDirection = dir;
            Vec3 end = mouth.add(dir.scale(BEAM_LENGTH));
            BlockHitResult hit = this.dragon.level().clip(new ClipContext(mouth, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.dragon));
            boolean hitBlock = hit.getType() != HitResult.Type.MISS;
            if (hitBlock) {
                end = hit.getLocation();
            }
            if (EdpConfig.DEBUG_LOGGING.get() && local % 10 == 0) {
                EnderDragonPlus.LOGGER.info("Void breath tick {}: aim {} target distance {}", local, this.aim, this.aim.distanceTo(this.target.position()));
            }
            if (local % 4 == 0) {
                double multiplier = EdpConfig.VOID_BREATH.damage.get();
                for (LivingEntity victim : brain.victims(new AABB(mouth, end).inflate(2.0))) {
                    Vec3 center = victim.position().add(0.0, victim.getBbHeight() * 0.5, 0.0);
                    if (EdpMath.distanceToSegment(center, mouth, end) <= 1.5 + victim.getBbWidth() * 0.5) {
                        brain.damage(victim, multiplier);
                    }
                }
            }
            if (hitBlock && local % 6 == 0) {
                brain.addHazard(new ScorchHazard(end, 2.2, 100, EdpConfig.BREATH_SCORCH_MULTIPLIER.get()));
            }
            return;
        }
        if (local >= this.beamTicks + 10) {
            this.finish();
        }
    }

    @Override
    public EnderDragonPhase<VoidBreathPhase> getPhase() {
        return DragonPhases.VOID_BREATH;
    }
}
