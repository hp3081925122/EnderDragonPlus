package com.enderdragonplus.dragon.phase;

import com.enderdragonplus.EdpConfig;
import com.enderdragonplus.dragon.DragonBrain;
import com.enderdragonplus.dragon.DragonPhases;
import com.enderdragonplus.dragon.hazard.LanceHazard;
import com.enderdragonplus.network.VfxPayload;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class VoidLancesPhase extends SkillPhase {
    private static final int FIRST_LANCE = 15;
    private static final int INTERVAL = 10;
    private static final int CHARGE = 22;
    private static final double LENGTH = 48.0;
    private static final double RADIUS = 1.4;
    private int lances;
    private int fired;

    public VoidLancesPhase(EnderDragon dragon) {
        super(dragon);
    }

    @Override
    protected boolean start() {
        DragonBrain brain = this.brain();
        if (brain.combatPlayers().isEmpty()) {
            return false;
        }
        double minY = brain.groundY(this.dragon.getX(), this.dragon.getZ(), this.dragon.getY()) + 16.0;
        this.moveTarget = new Vec3(this.dragon.getX(), Math.max(this.dragon.getY(), minY), this.dragon.getZ());
        this.moveSpeed = 0.7;
        this.lances = EdpConfig.LANCE_COUNT.get();
        if (brain.stage() >= 3) {
            this.lances += 2;
        }
        this.fired = 0;
        brain.playSound(this.dragon.position(), SoundEvents.ENDER_DRAGON_GROWL, 8.0F, 0.9F);
        return true;
    }

    @Override
    protected void serverTick() {
        if (this.fired < this.lances && this.tick >= FIRST_LANCE && (this.tick - FIRST_LANCE) % INTERVAL == 0) {
            List<ServerPlayer> players = this.brain().combatPlayers();
            if (!players.isEmpty()) {
                this.summon(players.get(this.fired % players.size()));
            }
            this.fired++;
        }
        if (this.fired >= this.lances && this.tick >= FIRST_LANCE + INTERVAL * this.lances + CHARGE + 10) {
            this.finish();
        }
    }

    private void summon(ServerPlayer player) {
        DragonBrain brain = this.brain();
        RandomSource random = this.dragon.getRandom();
        this.faceDirection = player.position().subtract(this.dragon.position());
        float angle = random.nextFloat() * Mth.TWO_PI;
        double height = 7.0 + random.nextDouble() * 5.0;
        Vec3 origin = player.position().add(Mth.cos(angle) * 16.0, height, Mth.sin(angle) * 16.0);
        Vec3 aim = player.position().add(0.0, 1.0, 0.0).add(player.getDeltaMovement().multiply(6.0, 0.0, 6.0));
        Vec3 dir = aim.subtract(origin).normalize();
        Vec3 far = origin.add(dir.scale(LENGTH));
        BlockHitResult hit = brain.level().clip(new ClipContext(origin.add(dir.scale(2.0)), far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.dragon));
        Vec3 end = far;
        if (hit.getType() != HitResult.Type.MISS) {
            end = hit.getLocation();
        }
        brain.addHazard(new LanceHazard(origin, end, CHARGE, RADIUS, EdpConfig.VOID_LANCES.damage.get()));
        CompoundTag data = new CompoundTag();
        VfxPayload.putVec(data, "origin", origin);
        VfxPayload.putVec(data, "end", end);
        data.putInt("charge", CHARGE);
        data.putInt("color", brain.stageColor());
        brain.sendVfx(VfxPayload.LANCE, data);
        brain.playSound(origin, SoundEvents.PORTAL_TRIGGER, 2.0F, 1.8F);
    }

    @Override
    public EnderDragonPhase<VoidLancesPhase> getPhase() {
        return DragonPhases.VOID_LANCES;
    }
}
