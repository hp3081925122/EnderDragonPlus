package com.enderdragonplus.dragon.hazard;

import com.enderdragonplus.dragon.DragonBrain;
import com.enderdragonplus.network.VfxPayload;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class BarrageHazard extends Hazard {
    private final int id;
    private final Vec3[] positions;
    private final Vec3[] directions;
    private final boolean[] alive;
    private final double speed;
    private final int life;
    private final double multiplier;
    private final double hitRadius;

    public BarrageHazard(int id, Vec3 origin, Vec3[] directions, double speed, int life, double multiplier) {
        this(id, origin, directions, speed, life, multiplier, 0.6);
    }

    public BarrageHazard(int id, Vec3 origin, Vec3[] directions, double speed, int life, double multiplier, double hitRadius) {
        this.id = id;
        this.directions = directions;
        this.positions = new Vec3[directions.length];
        this.alive = new boolean[directions.length];
        for (int i = 0; i < directions.length; i++) {
            this.positions[i] = origin;
            this.alive[i] = true;
        }
        this.speed = speed;
        this.life = life;
        this.multiplier = multiplier;
        this.hitRadius = hitRadius;
    }

    @Override
    public boolean tick(ServerLevel level, DragonBrain brain) {
        this.age++;
        boolean any = false;
        for (int i = 0; i < this.positions.length; i++) {
            if (!this.alive[i]) {
                continue;
            }
            Vec3 from = this.positions[i];
            Vec3 to = from.add(this.directions[i].scale(this.speed));
            if (this.age > 3) {
                BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, brain.dragon()));
                if (hit.getType() != HitResult.Type.MISS) {
                    this.alive[i] = false;
                    continue;
                }
            }
            List<LivingEntity> victims = brain.victims(new AABB(from, to).inflate(this.hitRadius));
            if (!victims.isEmpty()) {
                brain.damage(victims.get(0), this.multiplier);
                this.alive[i] = false;
                CompoundTag data = new CompoundTag();
                data.putInt("id", this.id);
                data.putInt("index", i);
                brain.sendVfx(VfxPayload.BARRAGE_HIT, data);
                continue;
            }
            this.positions[i] = to;
            any = true;
        }
        return any && this.age < this.life;
    }
}
