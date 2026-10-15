package com.enderdragonplus.client.vfx;

import com.enderdragonplus.client.Draw;
import com.enderdragonplus.client.VfxInstance;
import com.enderdragonplus.client.VfxManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;

public final class DiveVfx extends VfxInstance {
    private static final double PILLAR = 40.0;
    private final int dragonId;
    private final Vec3 mark;
    private final int warn;
    private final int dive;
    private final float radius;
    private final int color;

    public DiveVfx(int dragonId, Vec3 mark, int warn, int dive, float radius, int color) {
        super(warn + dive + 4);
        this.dragonId = dragonId;
        this.mark = mark;
        this.warn = warn;
        this.dive = dive;
        this.radius = radius;
        this.color = color;
    }

    @Override
    protected void onTick() {
        if (this.age != this.warn + this.dive) {
            return;
        }
        VfxManager.add(new DecalVfx(this.mark, this.radius * 1.3F, 120, this.color, true));
        VfxManager.add(new DecalVfx(this.mark, this.radius * 0.8F, 120, this.color, false));
    }

    @Override
    public void render(float partialTick) {
        float t = this.time(partialTick);
        float total = this.warn + this.dive;
        if (t >= total) {
            return;
        }
        float k = t / total;
        float a = Math.min(1.0F, t / 6.0F);
        if (Draw.begin(Draw.Material.GROUND)) {
            Draw.terrain(this.mark, this.radius, 24, 0.08F, this.color, a);
            Draw.end(Draw.GROUND_DANGER, k, 0.7F, 0.0F);
        }
        if (Draw.begin(Draw.Material.GROUND)) {
            Draw.terrain(this.mark, this.radius * 0.35F, 8, 0.1F, this.color, a);
            Draw.end(Draw.GROUND_WARNING, k, 0.0F, 0.0F);
        }
        float pulse = 0.7F + 0.3F * Mth.sin(t * 1.2F);
        if (Draw.begin(Draw.Material.SPHERE)) {
            Draw.cylinder(this.mark, this.mark.add(0.0, PILLAR, 0.0), 0.8F + 1.2F * k, 24, this.color, a * 0.6F * pulse, 6.0F);
            Draw.end(Draw.SPHERE_SHELL, 2.0F, 2.0F, 1.5F);
        }
        int hot = Draw.mix(this.color, 0xFFFFFF, 0.5F);
        if (Draw.begin(Draw.Material.ENERGY)) {
            Draw.beam(this.mark, this.mark.add(0.0, PILLAR, 0.0), 0.3F + 0.8F * k, hot, a * pulse, 8.0F);
            if (t >= this.warn) {
                EnderDragon dragon = dragon(this.dragonId);
                if (dragon != null) {
                    Vec3 body = bodyCenter(dragon, partialTick);
                    float d = (t - this.warn) / this.dive;
                    Draw.beam(body, body.add(0.0, 16.0, 0.0), 7.0F, this.color, 0.4F + 0.4F * d, 3.0F);
                }
            }
            Draw.end(3.0F, 1.0F, 1.8F, Draw.MODE_STRIP);
        }
    }
}
