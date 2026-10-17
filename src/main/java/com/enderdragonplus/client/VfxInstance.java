package com.enderdragonplus.client;

import com.enderdragonplus.dragon.EdpMath;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.culling.Frustum;

public abstract class VfxInstance {
    protected int age;
    protected int lifetime;

    protected VfxInstance(int lifetime) {
        this.lifetime = lifetime;
    }

    public boolean tick() {
        this.age++;
        this.onTick();
        return this.age < this.lifetime;
    }

    protected void onTick() {
    }

    public abstract void render(float partialTick);

    protected static int heavyDetail(int value) {
        return com.enderdragonplus.EdpConfig.OPTIMIZE_HEAVY_EFFECTS.get() ? Math.max(8, value * 3 / 4) : value;
    }

    public boolean visible(Frustum frustum) {
        return true;
    }

    public boolean distorts() {
        return false;
    }

    public void renderDistortion(float partialTick) {
    }

    protected float time(float partialTick) {
        return this.age + partialTick;
    }

    protected float fade(float partialTick, float in, float out) {
        float t = this.time(partialTick);
        float a = 1.0F;
        if (in > 0.0F) {
            a = Math.min(a, t / in);
        }
        if (out > 0.0F) {
            a = Math.min(a, (this.lifetime - t) / out);
        }
        return Mth.clamp(a, 0.0F, 1.0F);
    }

    @Nullable
    protected static EnderDragon dragon(int id) {
        if (Minecraft.getInstance().level == null) {
            return null;
        }
        Entity entity = Minecraft.getInstance().level.getEntity(id);
        if (entity instanceof EnderDragon dragon) {
            return dragon;
        }
        return null;
    }

    protected static Vec3 lerpPos(Entity entity, float partialTick) {
        return new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()), Mth.lerp(partialTick, entity.yo, entity.getY()), Mth.lerp(partialTick, entity.zo, entity.getZ()));
    }

    protected static Vec3 mouth(EnderDragon dragon, float partialTick) {
        Vec3 head = lerpPos(dragon.head, partialTick).add(0.0, dragon.head.getBbHeight() * 0.5, 0.0);
        return EdpMath.mouth(head, Mth.rotLerp(partialTick, dragon.yRotO, dragon.getYRot()));
    }

    protected static Vec3 bodyCenter(EnderDragon dragon, float partialTick) {
        return lerpPos(dragon, partialTick).add(0.0, 2.0, 0.0);
    }

    protected static double distanceToCamera(Vec3 pos) {
        return Draw.cameraPos().distanceTo(pos);
    }

    protected static double distanceToPlayer(Vec3 pos) {
        if (Minecraft.getInstance().player == null) {
            return Double.MAX_VALUE;
        }
        return Minecraft.getInstance().player.position().distanceTo(pos);
    }
}
