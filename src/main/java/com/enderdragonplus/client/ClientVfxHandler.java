package com.enderdragonplus.client;

import com.enderdragonplus.EdpConfig;
import com.enderdragonplus.client.showcase.ShowcaseClient;
import com.enderdragonplus.client.vfx.BarrageVfx;
import com.enderdragonplus.client.vfx.BlackHoleVfx;
import com.enderdragonplus.client.vfx.BreathBeamVfx;
import com.enderdragonplus.client.vfx.BreathChargeVfx;
import com.enderdragonplus.client.vfx.DiveVfx;
import com.enderdragonplus.client.vfx.LanceVfx;
import com.enderdragonplus.client.vfx.MeteorShowerVfx;
import com.enderdragonplus.client.vfx.NovaVfx;
import com.enderdragonplus.client.vfx.RiftDashVfx;
import com.enderdragonplus.client.vfx.RingWavesVfx;
import com.enderdragonplus.client.vfx.RuptureVfx;
import com.enderdragonplus.client.vfx.ShieldVfx;
import com.enderdragonplus.client.vfx.ShockwaveVfx;
import com.enderdragonplus.client.vfx.StormStrikeVfx;
import com.enderdragonplus.dragon.EdpMath;
import com.enderdragonplus.network.VfxPayload;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

public final class ClientVfxHandler {
    private ClientVfxHandler() {
    }

    public static void handle(VfxPayload payload) {
        CompoundTag d = payload.data();
        int id = payload.entityId();

        switch (payload.kind()) {
            case VfxPayload.BREATH_CHARGE -> VfxManager.add(new BreathChargeVfx(id, d.getInt("duration"), d.getInt("color")));
            case VfxPayload.BREATH_BEAM -> VfxManager.add(new BreathBeamVfx(id, VfxPayload.getVec(d, "aim"), d.getInt("duration"), d.getDouble("length"), d.getInt("color")));
            case VfxPayload.STAGE -> DragonStageClient.set(id, d.getInt("stage"));
            case VfxPayload.BREATH_AIM -> BreathBeamVfx.updateAim(id, VfxPayload.getVec(d, "aim"));
            case VfxPayload.METEOR_SHOWER -> VfxManager.add(new MeteorShowerVfx(d));
            case VfxPayload.RIFT_DASH -> VfxManager.add(new RiftDashVfx(id, VfxPayload.getVec(d, "from"), VfxPayload.getVec(d, "to"), d.getInt("telegraph"), d.getInt("dash"), d.getInt("color")));
            case VfxPayload.SHIELD -> VfxManager.add(new ShieldVfx(id, d.getInt("duration"), d.getFloat("radius"), d.getInt("color")));
            case VfxPayload.SHOCKWAVE -> {
                Vec3 pos = VfxPayload.getVec(d, "pos");
                float radius = d.getFloat("radius");
                VfxManager.add(new ShockwaveVfx(pos, radius, d.getInt("duration"), d.getInt("color"), d.getBoolean("sphere")));
                CameraShake.addAt(pos, d.getFloat("shake"), radius * 3.0F, 20);
                if (VfxInstance.distanceToPlayer(pos) < radius * 2.0F) {
                    ScreenFx.flash(d.getFloat("flash"));
                    ScreenFx.chromatic(d.getFloat("flash"));
                }
            }
            case VfxPayload.BLACK_HOLE -> VfxManager.add(new BlackHoleVfx(VfxPayload.getVec(d, "pos"), d.getInt("form"), d.getInt("active"), d.getInt("collapse"), d.getFloat("radius"), d.getInt("color")));
            case VfxPayload.BARRAGE -> VfxManager.add(new BarrageVfx(d.getInt("id"), VfxPayload.getVec(d, "origin"), EdpMath.unpackDirections(d.getIntArray("dirs")), d.getDouble("speed"), d.getInt("life"), d.getInt("color"), d.getInt("style")));
            case VfxPayload.BARRAGE_HIT -> BarrageVfx.hit(d.getInt("id"), d.getInt("index"));
            case VfxPayload.NOVA -> {
                ListTag list = d.getList("zones", Tag.TAG_COMPOUND);
                Vec3[] zones = new Vec3[list.size()];
                for (int i = 0; i < zones.length; i++) {
                    zones[i] = VfxPayload.getVec(list.getCompound(i), "p");
                }
                VfxManager.add(new NovaVfx(id, VfxPayload.getVec(d, "center"), zones, d.getFloat("safeRadius"), d.getInt("charge"), d.getFloat("blast"), d.getInt("color")));
            }
            case VfxPayload.RUPTURE -> {
                ListTag list = d.getList("points", Tag.TAG_COMPOUND);
                Vec3[] points = new Vec3[list.size()];
                int[] delays = new int[list.size()];
                for (int i = 0; i < points.length; i++) {
                    CompoundTag point = list.getCompound(i);
                    points[i] = VfxPayload.getVec(point, "p");
                    delays[i] = point.getInt("delay");
                }
                VfxManager.add(new RuptureVfx(points, delays, d.getFloat("radius"), d.getInt("color")));
            }
            case VfxPayload.STORM_STRIKE -> VfxManager.add(new StormStrikeVfx(VfxPayload.getVec(d, "pos"), d.getInt("delay"), d.getFloat("radius"), d.getInt("color")));
            case VfxPayload.RING_WAVES -> VfxManager.add(new RingWavesVfx(VfxPayload.getVec(d, "center"), d.getInt("count"), d.getInt("interval"), d.getFloat("speed"), d.getFloat("maxRadius"), d.getInt("color")));
            case VfxPayload.LANCE -> VfxManager.add(new LanceVfx(VfxPayload.getVec(d, "origin"), VfxPayload.getVec(d, "end"), d.getInt("charge"), d.getInt("color")));
            case VfxPayload.DIVE -> VfxManager.add(new DiveVfx(id, VfxPayload.getVec(d, "mark"), d.getInt("warn"), d.getInt("dive"), d.getFloat("radius"), d.getInt("color")));
            case VfxPayload.SHOWCASE -> ShowcaseClient.handle(id, d);
            default -> {
            }
        }
    }
}
