package com.enderdragonplus.dragon.phase;

import com.enderdragonplus.EdpConfig;
import com.enderdragonplus.dragon.DragonBrain;
import com.enderdragonplus.dragon.DragonPhases;
import com.enderdragonplus.dragon.EdpMath;
import com.enderdragonplus.dragon.hazard.BarrageHazard;
import com.enderdragonplus.network.VfxPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.phys.Vec3;

public class WingBladesPhase extends SkillPhase {
    public static final double SPEED = 0.7;
    public static final int LIFE = 70;
    private static final int FIRST_VOLLEY = 20;
    private static final int INTERVAL = 16;
    private static final float SPREAD = 30.0F * Mth.DEG_TO_RAD;
    private static final double HIT_RADIUS = 1.8;
    private LivingEntity target;
    private int volleys;
    private int fired;

    public WingBladesPhase(EnderDragon dragon) {
        super(dragon);
    }

    @Override
    protected boolean start() {
        DragonBrain brain = this.brain();
        this.target = brain.validTarget();
        if (this.target == null) {
            return false;
        }
        this.volleys = brain.stage() + 1;
        this.fired = 0;
        this.updateHover();
        this.moveSpeed = 1.2;
        brain.playSound(this.dragon.position(), SoundEvents.ENDER_DRAGON_FLAP, 8.0F, 0.7F);
        return true;
    }

    private void updateHover() {
        Vec3 back = new Vec3(this.dragon.getX() - this.target.getX(), 0.0, this.dragon.getZ() - this.target.getZ());
        if (back.lengthSqr() < 1.0E-4) {
            back = new Vec3(1.0, 0.0, 0.0);
        }
        back = back.normalize().scale(16.0);
        double y = this.brain().groundY(this.target.getX(), this.target.getZ(), this.target.getY()) + 8.0;
        this.moveTarget = new Vec3(this.target.getX() + back.x, Math.max(y, this.target.getY() + 6.0), this.target.getZ() + back.z);
    }

    @Override
    protected void serverTick() {
        if (!this.target.isAlive()) {
            this.finish();
            return;
        }
        this.faceDirection = this.target.position().subtract(this.dragon.position());
        if (this.tick % 10 == 0) {
            this.updateHover();
        }
        if (this.fired < this.volleys && this.tick >= FIRST_VOLLEY && (this.tick - FIRST_VOLLEY) % INTERVAL == 0) {
            this.fireVolley();
            this.fired++;
        }
        if (this.fired >= this.volleys && this.tick >= FIRST_VOLLEY + INTERVAL * this.volleys + 10) {
            this.finish();
        }
    }

    private void fireVolley() {
        DragonBrain brain = this.brain();
        Vec3 origin = brain.bodyCenter();
        Vec3 aim = this.target.position().add(0.0, this.target.getBbHeight() * 0.5, 0.0).add(this.target.getDeltaMovement().multiply(6.0, 0.0, 6.0)).subtract(origin);
        if (aim.lengthSqr() < 1.0E-4) {
            aim = new Vec3(0.0, -1.0, 1.0);
        }
        float yaw = EdpMath.yawOf(aim);
        float pitch = EdpMath.pitchOf(aim);
        int count = EdpConfig.WING_BLADES_PER_VOLLEY.get();
        Vec3[] raw = new Vec3[count];
        for (int i = 0; i < count; i++) {
            float k = 0.0F;
            if (count > 1) {
                k = i / (float) (count - 1) * 2.0F - 1.0F;
            }
            raw[i] = EdpMath.direction(yaw + k * SPREAD, pitch);
        }
        int[] packed = EdpMath.packDirections(raw);
        Vec3[] dirs = EdpMath.unpackDirections(packed);
        int id = this.dragon.getRandom().nextInt();
        brain.addHazard(new BarrageHazard(id, origin, dirs, SPEED, LIFE, EdpConfig.WING_BLADES.damage.get(), HIT_RADIUS));
        CompoundTag data = new CompoundTag();
        data.putInt("id", id);
        VfxPayload.putVec(data, "origin", origin);
        data.putIntArray("dirs", packed);
        data.putDouble("speed", SPEED);
        data.putInt("life", LIFE);
        data.putInt("color", brain.stageColor());
        data.putInt("style", 1);
        brain.sendVfx(VfxPayload.BARRAGE, data);
        brain.playSound(origin, SoundEvents.PLAYER_ATTACK_SWEEP, 6.0F, 0.5F);
        brain.playSound(origin, SoundEvents.ENDER_DRAGON_FLAP, 6.0F, 1.2F);
    }

    @Override
    public EnderDragonPhase<WingBladesPhase> getPhase() {
        return DragonPhases.WING_BLADES;
    }
}
