package com.enderdragonplus.client.vfx;

import com.enderdragonplus.client.Draw;
import com.enderdragonplus.client.VfxInstance;
import com.enderdragonplus.client.VfxManager;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class BarrageVfx extends VfxInstance {
    private static final Map<Integer, BarrageVfx> ACTIVE = new HashMap<>();
    private final int id;
    private final Vec3[] directions;
    private final Vec3[] positions;
    private final Vec3[] previous;
    private final boolean[] alive;
    private final double speed;
    private final int life;
    private final int color;
    private int simAge;

    public BarrageVfx(int id, Vec3 origin, Vec3[] directions, double speed, int life, int color) {
        super(life + 2);
        this.id = id;
        this.directions = directions;
        this.positions = new Vec3[directions.length];
        this.previous = new Vec3[directions.length];
        this.alive = new boolean[directions.length];
        for (int i = 0; i < directions.length; i++) {
            this.positions[i] = origin;
            this.previous[i] = origin;
            this.alive[i] = true;
        }
        this.speed = speed;
        this.life = life;
        this.color = color;
        ACTIVE.put(id, this);
    }

    public static void clearAll() {
        ACTIVE.clear();
    }

    public static void hit(int id, int index) {
        BarrageVfx barrage = ACTIVE.get(id);
        if (barrage == null || index < 0 || index >= barrage.alive.length || !barrage.alive[index]) {
            return;
        }
        barrage.alive[index] = false;
        VfxManager.add(new ShockwaveVfx(barrage.positions[index], 2.5F, 10, barrage.color, true));
    }

    @Override
    public boolean tick() {
        boolean alive = super.tick();
        if (!alive) {
            ACTIVE.remove(this.id);
        }
        return alive;
    }

    @Override
    protected void onTick() {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        this.simAge++;
        boolean any = false;
        for (int i = 0; i < this.positions.length; i++) {
            this.previous[i] = this.positions[i];
            if (!this.alive[i]) {
                continue;
            }
            Vec3 from = this.positions[i];
            Vec3 to = from.add(this.directions[i].scale(this.speed));
            if (this.simAge > 3) {
                BlockHitResult hit = Minecraft.getInstance().level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
                if (hit.getType() != HitResult.Type.MISS) {
                    this.alive[i] = false;
                    VfxManager.add(new ShockwaveVfx(hit.getLocation(), 2.2F, 10, this.color, false));
                    continue;
                }
            }
            this.positions[i] = to;
            any = true;
        }
        if (!any || this.simAge >= this.life) {
            this.lifetime = this.age;
        }
    }

    @Override
    public void render(float partialTick) {
        int hot = Draw.mix(this.color, 0xFFFFFF, 0.55F);
        if (Draw.begin(Draw.Material.ENERGY)) {
            for (int i = 0; i < this.positions.length; i++) {
                if (!this.alive[i]) {
                    continue;
                }
                Vec3 head = this.previous[i].lerp(this.positions[i], partialTick);
                Vec3 tail = head.subtract(this.directions[i].scale(this.speed * 5.0));
                Vec3 mid = head.subtract(this.directions[i].scale(this.speed * 1.5));
                Draw.ribbon(new Vec3[]{head, mid, tail}, new float[]{0.7F, 0.45F, 0.0F}, new float[]{1.0F, 0.7F, 0.0F}, this.color, 0.0F, 1.5F);
                Draw.beam(head.add(this.directions[i].scale(0.5)), mid, 0.32F, hot, 1.0F, 1.0F);
            }
            Draw.end(2.0F, 1.0F, 1.8F, Draw.MODE_STRIP);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            for (int i = 0; i < this.positions.length; i++) {
                if (!this.alive[i]) {
                    continue;
                }
                Vec3 head = this.previous[i].lerp(this.positions[i], partialTick);
                Draw.billboard(head, 1.8F, this.color, 1.0F);
            }
            Draw.end(1.0F, 2.0F, 2.0F, Draw.MODE_RADIAL);
        }
    }
}
