package com.enderdragonplus.client.vfx;

import com.enderdragonplus.client.DragonStageClient;
import com.enderdragonplus.client.Draw;
import com.enderdragonplus.client.VfxInstance;
import java.util.ArrayDeque;
import java.util.Iterator;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.phys.Vec3;

public final class DragonAuraVfx extends VfxInstance {
    private static final int HISTORY = 10;
    private static final int TAIL = 5;
    private static final int WING_ONE = 6;
    private static final int WING_TWO = 7;
    private final int dragonId;
    private final ArrayDeque<Vec3> tailTrail = new ArrayDeque<>();
    private final ArrayDeque<Vec3> wingOneTrail = new ArrayDeque<>();
    private final ArrayDeque<Vec3> wingTwoTrail = new ArrayDeque<>();
    private int missing;
    private float strength;
    private float rage;

    public DragonAuraVfx(int dragonId) {
        super(Integer.MAX_VALUE);
        this.dragonId = dragonId;
    }

    public boolean finished() {
        return this.age >= this.lifetime;
    }

    public float strength() {
        return this.strength;
    }

    public float rage() {
        return this.rage;
    }

    private static Vec3 center(EnderDragonPart part, float partialTick) {
        return lerpPos(part, partialTick).add(0.0, part.getBbHeight() * 0.5, 0.0);
    }

    private static void push(ArrayDeque<Vec3> trail, Vec3 pos) {
        trail.addFirst(pos);
        while (trail.size() > HISTORY) {
            trail.removeLast();
        }
    }

    @Override
    protected void onTick() {
        EnderDragon dragon = dragon(this.dragonId);
        if (dragon == null || dragon.isRemoved()) {
            this.missing++;
            if (this.missing > 100) {
                this.lifetime = this.age;
                DragonStageClient.remove(this.dragonId);
            }
            return;
        }
        this.missing = 0;
        float target = 1.0F;
        if (DragonStageClient.stage(this.dragonId) < 2 || dragon.dragonDeathTime > 0) {
            target = 0.0F;
        }
        this.strength = Mth.approach(this.strength, target, 0.05F);
        float rageTarget = 0.0F;
        if (DragonStageClient.stage(this.dragonId) >= 3 && dragon.dragonDeathTime <= 0) {
            rageTarget = 1.0F;
        }
        this.rage = Mth.approach(this.rage, rageTarget, 0.04F);
        EnderDragonPart[] parts = dragon.getSubEntities();
        push(this.tailTrail, center(parts[TAIL], 1.0F));
        push(this.wingOneTrail, center(parts[WING_ONE], 1.0F));
        push(this.wingTwoTrail, center(parts[WING_TWO], 1.0F));
    }

    private static void trail(Vec3 head, ArrayDeque<Vec3> history, float width, float alpha, int rgb) {
        int n = history.size() + 1;
        if (n < 3) {
            return;
        }
        Vec3[] points = new Vec3[n];
        float[] widths = new float[n];
        float[] alphas = new float[n];
        points[0] = head;
        Iterator<Vec3> it = history.iterator();
        for (int i = 1; i < n; i++) {
            points[i] = it.next();
        }
        for (int i = 0; i < n; i++) {
            float k = 1.0F - i / (float) (n - 1);
            widths[i] = width * k;
            alphas[i] = alpha * k;
        }
        Draw.ribbon(points, widths, alphas, rgb, 0.0F, n * 0.3F);
    }

    @Override
    public void render(float partialTick) {
        if (this.strength <= 0.01F) {
            return;
        }
        EnderDragon dragon = dragon(this.dragonId);
        if (dragon == null) {
            return;
        }
        int stage = DragonStageClient.stage(this.dragonId);
        int rgb = DragonStageClient.color(stage);
        int hot = Draw.mix(rgb, 0xFFFFFF, 0.4F);
        float s = this.strength;
        float pulse = 1.0F;
        if (stage >= 3) {
            pulse = 0.8F + 0.2F * Mth.sin((this.age + partialTick) * 0.3F);
        }
        EnderDragonPart[] parts = dragon.getSubEntities();
        Vec3 head = center(parts[0], partialTick);
        Vec3 neck = center(parts[1], partialTick);
        Vec3 body = center(parts[2], partialTick);
        Vec3 tailA = center(parts[3], partialTick);
        Vec3 tailB = center(parts[4], partialTick);
        Vec3 tailC = center(parts[TAIL], partialTick);
        if (Draw.begin(Draw.Material.ENERGY)) {
            Draw.ribbon(new Vec3[]{neck, body, tailA, tailB, tailC}, new float[]{1.6F, 2.4F, 1.8F, 1.2F, 0.6F}, new float[]{0.35F * s, 0.45F * s * pulse, 0.55F * s, 0.6F * s * pulse, 0.7F * s}, rgb, 0.0F, 2.5F);
            trail(tailC, this.tailTrail, 1.4F, 0.7F * s * pulse, hot);
            trail(center(parts[WING_ONE], partialTick), this.wingOneTrail, 3.5F, 0.3F * s, rgb);
            trail(center(parts[WING_TWO], partialTick), this.wingTwoTrail, 3.5F, 0.3F * s, rgb);
            Draw.end(1.4F, 1.0F, 1.2F, Draw.MODE_STRIP);
        }
        if (Draw.begin(Draw.Material.ENERGY)) {
            Draw.billboard(head, 3.5F * pulse, rgb, 0.6F * s);
            if (stage >= 3) {
                Draw.billboard(body, 16.0F, rgb, 0.22F * s * pulse);
            }
            Draw.end(0.6F, 2.0F, 1.6F, Draw.MODE_RADIAL);
        }
    }
}
