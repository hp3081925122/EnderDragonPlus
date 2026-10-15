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

public class ScaleBarragePhase extends SkillPhase {
    public static final double SPEED = 0.85;
    public static final int LIFE = 80;
    private static final int FIRST_WAVE = 20;
    private static final int INTERVAL = 14;
    private LivingEntity target;
    private int waves;
    private int fired;

    public ScaleBarragePhase(EnderDragon dragon) {
        super(dragon);
    }

    @Override
    protected boolean start() {
        DragonBrain brain = this.brain();
        this.target = brain.validTarget();
        if (this.target == null) {
            return false;
        }
        double minY = brain.groundY(this.dragon.getX(), this.dragon.getZ(), this.target.getY()) + 18.0;
        this.moveTarget = new Vec3(this.dragon.getX(), Math.max(this.dragon.getY(), minY), this.dragon.getZ());
        this.moveSpeed = 0.8;
        this.waves = EdpConfig.BARRAGE_WAVES.get();
        if (brain.stage() == 3) {
            this.waves += 1;
        }
        this.fired = 0;
        brain.playSound(this.dragon.position(), SoundEvents.ENDER_DRAGON_GROWL, 6.0F, 1.1F);
        return true;
    }

    @Override
    protected void serverTick() {
        this.faceDirection = this.target.position().subtract(this.dragon.position());
        if (this.fired < this.waves && this.tick >= FIRST_WAVE && (this.tick - FIRST_WAVE) % INTERVAL == 0) {
            this.fireWave();
            this.fired++;
        }
        if (this.fired >= this.waves && this.tick >= FIRST_WAVE + INTERVAL * this.waves + 10) {
            this.finish();
        }
    }

    private void fireWave() {
        DragonBrain brain = this.brain();
        Vec3 origin = brain.bodyCenter();
        int count = EdpConfig.BARRAGE_PER_WAVE.get();
        double dy = this.target.getY(0.5) - origin.y;
        double horizontal = Math.max(1.0, Math.sqrt(this.target.distanceToSqr(origin.x, this.target.getY(), origin.z)));
        float pitch = Mth.clamp((float) Math.atan2(dy, horizontal), -0.9F, 0.2F);
        float offset = this.fired * Mth.PI / count;
        Vec3[] raw = new Vec3[count];
        float cp = Mth.cos(pitch);
        float sp = Mth.sin(pitch);
        for (int i = 0; i < count; i++) {
            float a = offset + Mth.TWO_PI * i / count;
            raw[i] = new Vec3(Mth.cos(a) * cp, sp, Mth.sin(a) * cp);
        }
        int[] packed = EdpMath.packDirections(raw);
        Vec3[] dirs = EdpMath.unpackDirections(packed);
        int id = this.dragon.getRandom().nextInt();
        double multiplier = EdpConfig.SCALE_BARRAGE.damage.get();
        brain.addHazard(new BarrageHazard(id, origin, dirs, SPEED, LIFE, multiplier));
        CompoundTag data = new CompoundTag();
        data.putInt("id", id);
        VfxPayload.putVec(data, "origin", origin);
        data.putIntArray("dirs", packed);
        data.putDouble("speed", SPEED);
        data.putInt("life", LIFE);
        data.putInt("color", brain.stageColor());
        brain.sendVfx(VfxPayload.BARRAGE, data);
        brain.playSound(origin, SoundEvents.SHULKER_SHOOT, 5.0F, 0.6F);
    }

    @Override
    public EnderDragonPhase<ScaleBarragePhase> getPhase() {
        return DragonPhases.SCALE_BARRAGE;
    }
}
