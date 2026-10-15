package com.enderdragonplus.client.vfx;

import com.enderdragonplus.client.Draw;
import com.enderdragonplus.client.VfxInstance;
import com.enderdragonplus.dragon.EdpMath;
import net.minecraft.world.phys.Vec3;

public final class DecalVfx extends VfxInstance {
    private final Vec3 pos;
    private final float radius;
    private final int color;
    private final boolean crack;
    private final float seed;

    public DecalVfx(Vec3 pos, float radius, int life, int color, boolean crack) {
        super(life);
        this.pos = pos;
        this.radius = radius;
        this.color = color;
        this.crack = crack;
        this.seed = (float) ((pos.x * 0.137 + pos.z * 0.291) % 1.0);
    }

    private void shape(float half, int rgb, float alpha) {
        if (half > 5.0F) {
            Draw.terrain(this.pos, half, 16, 0.05F, rgb, alpha);
        } else {
            Draw.ground(this.pos, half, rgb, alpha);
        }
    }

    @Override
    public void render(float partialTick) {
        float t = this.time(partialTick);
        float spread = EdpMath.easeOut(t / 15.0F);
        float half = this.radius * (0.35F + 0.65F * spread);
        float alpha = this.fade(partialTick, 0.0F, this.lifetime * 0.35F);
        float progress = t / this.lifetime;
        if (this.crack) {
            if (Draw.begin(Draw.Material.GROUND)) {
                this.shape(half, Draw.mix(this.color, 0xFFFFFF, 0.25F), alpha);
                Draw.end(Draw.GROUND_CRACK, progress, this.seed, 0.0F);
            }
            return;
        }
        if (Draw.begin(Draw.Material.GROUND_ALPHA)) {
            this.shape(half, this.color, alpha);
            Draw.end(Draw.GROUND_SCORCH, progress, this.seed, 0.0F);
        }
        if (Draw.begin(Draw.Material.GROUND)) {
            this.shape(half, this.color, alpha);
            Draw.end(Draw.GROUND_SCORCH_GLOW, progress, this.seed, 0.0F);
        }
    }
}
