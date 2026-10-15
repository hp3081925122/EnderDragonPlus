package com.enderdragonplus.dragon.phase;

import com.enderdragonplus.EdpConfig;
import com.enderdragonplus.dragon.DragonBrain;
import com.enderdragonplus.dragon.DragonPhases;
import com.enderdragonplus.dragon.hazard.MeteorHazard;
import com.enderdragonplus.network.VfxPayload;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.phys.Vec3;

public class MeteorPhase extends SkillPhase {
    private static final int CAST_TICK = 20;
    private static final int FALL_TICKS = 24;

    public MeteorPhase(EnderDragon dragon) {
        super(dragon);
    }

    @Override
    protected boolean start() {
        if (this.brain().combatPlayers().isEmpty()) {
            return false;
        }
        this.moveTarget = this.dragon.position().add(0.0, 10.0, 0.0);
        this.moveSpeed = 0.6;
        this.brain().playSound(this.dragon.position(), SoundEvents.ENDER_DRAGON_GROWL, 8.0F, 0.6F);
        return true;
    }

    @Override
    protected void serverTick() {
        if (this.tick == CAST_TICK) {
            this.summon();
        }
        if (this.tick >= CAST_TICK + 30) {
            this.finish();
        }
    }

    private void summon() {
        DragonBrain brain = this.brain();
        List<ServerPlayer> players = brain.combatPlayers();
        if (players.isEmpty()) {
            return;
        }
        RandomSource random = this.dragon.getRandom();
        int count = EdpConfig.METEOR_COUNT.get();
        if (brain.stage() == 3) {
            count += 2;
        }
        double radius = EdpConfig.METEOR_RADIUS.get();
        double multiplier = EdpConfig.METEOR.damage.get();
        boolean breakBlocks = EdpConfig.METEOR_BREAK_BLOCKS.get();
        ListTag list = new ListTag();
        for (int i = 0; i < count; i++) {
            ServerPlayer player = players.get(i % players.size());
            double x = player.getX();
            double z = player.getZ();
            if (i < players.size()) {
                x += player.getDeltaMovement().x * 15.0;
                z += player.getDeltaMovement().z * 15.0;
            } else {
                x += (random.nextDouble() - 0.5) * 14.0;
                z += (random.nextDouble() - 0.5) * 14.0;
            }
            Vec3 impact = new Vec3(x, brain.groundY(x, z, player.getY()), z);
            Vec3 start = impact.add((random.nextDouble() - 0.5) * 28.0, 42.0, (random.nextDouble() - 0.5) * 28.0);
            int delay = i * 7 + random.nextInt(5);
            brain.addHazard(new MeteorHazard(impact, delay + FALL_TICKS, radius, multiplier, breakBlocks));
            CompoundTag meteor = new CompoundTag();
            VfxPayload.putVec(meteor, "s", start);
            VfxPayload.putVec(meteor, "e", impact);
            meteor.putInt("delay", delay);
            meteor.putInt("fall", FALL_TICKS);
            list.add(meteor);
        }
        CompoundTag data = new CompoundTag();
        data.put("meteors", list);
        data.putDouble("radius", radius);
        data.putInt("color", brain.stageColor());
        brain.sendVfx(VfxPayload.METEOR_SHOWER, data);
        brain.playSound(this.dragon.position(), SoundEvents.WITHER_SPAWN, 4.0F, 1.4F);
    }

    @Override
    public EnderDragonPhase<MeteorPhase> getPhase() {
        return DragonPhases.METEOR;
    }
}
