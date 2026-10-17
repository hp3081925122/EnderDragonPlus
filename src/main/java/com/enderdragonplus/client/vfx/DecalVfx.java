package com.enderdragonplus.client.vfx;

import com.enderdragonplus.EdpConfig;
import com.enderdragonplus.client.Draw;
import com.enderdragonplus.client.VfxInstance;
import com.enderdragonplus.dragon.EdpMath;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.client.renderer.culling.Frustum;

public final class DecalVfx extends VfxInstance {
    private final Vec3 pos;
    private final AABB bounds;
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
        var level = net.minecraft.client.Minecraft.getInstance().level;
        double extent = radius + 1.0;
        this.bounds = level == null ? null : new AABB(pos.x - extent, level.getMinBuildHeight() - 1.0,
                pos.z - extent, pos.x + extent, level.getMaxBuildHeight() + 1.0, pos.z + extent);
        this.seed = (float) ((pos.x * 0.137 + pos.z * 0.291) % 1.0);
    }

    @Override
    public boolean visible(Frustum frustum) {
        return frustum == null || this.bounds == null || frustum.isVisible(this.bounds);
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
        float glow = EdpConfig.OPTIMIZE_HEAVY_EFFECTS.get()
                ? Math.max(0.0F, Math.min(1.0F, (0.5F - progress) / 0.15F)) : 1.0F;
        if (glow > 0.0F && Draw.begin(Draw.Material.GROUND)) {
            this.shape(half, this.color, alpha * glow);
            Draw.end(Draw.GROUND_SCORCH_GLOW, progress, this.seed, 0.0F);
        }
    }
}
