package com.enderdragonplus.client;

import com.enderdragonplus.EdpConfig;
import com.enderdragonplus.EnderDragonPlus;
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
import net.minecraft.world.phys.Vec3;

public final class ClientVfxHandler {
    private ClientVfxHandler() {
    }

    public static void handle(VfxPayload payload) {
        CompoundTag d = payload.data();
        int id = payload.entityId();
        if (EdpConfig.DEBUG_LOGGING.get()) {
            EnderDragonPlus.LOGGER.info("Received vfx packet type {} for entity {}", payload.kind(), id);
        }
        switch (payload.kind()) {
            case VfxPayload.BREATH_CHARGE -> VfxManager.add(new BreathChargeVfx(id, d.getIntOr("duration", 0), d.getIntOr("color", 0)));
            case VfxPayload.BREATH_BEAM -> VfxManager.add(new BreathBeamVfx(id, VfxPayload.getVec(d, "aim"), d.getIntOr("duration", 0), d.getDoubleOr("length", 0.0), d.getIntOr("color", 0)));
            case VfxPayload.STAGE -> DragonStageClient.set(id, d.getIntOr("stage", 0));
            case VfxPayload.BREATH_AIM -> BreathBeamVfx.updateAim(id, VfxPayload.getVec(d, "aim"));
            case VfxPayload.METEOR_SHOWER -> VfxManager.add(new MeteorShowerVfx(d));
            case VfxPayload.RIFT_DASH -> VfxManager.add(new RiftDashVfx(id, VfxPayload.getVec(d, "from"), VfxPayload.getVec(d, "to"), d.getIntOr("telegraph", 0), d.getIntOr("dash", 0), d.getIntOr("color", 0)));
            case VfxPayload.SHIELD -> VfxManager.add(new ShieldVfx(id, d.getIntOr("duration", 0), d.getFloatOr("radius", 0.0F), d.getIntOr("color", 0)));
            case VfxPayload.SHOCKWAVE -> {
                Vec3 pos = VfxPayload.getVec(d, "pos");
                float radius = d.getFloatOr("radius", 0.0F);
                VfxManager.add(new ShockwaveVfx(pos, radius, d.getIntOr("duration", 0), d.getIntOr("color", 0), d.getBooleanOr("sphere", false)));
                CameraShake.addAt(pos, d.getFloatOr("shake", 0.0F), radius * 3.0F, 20);
                if (VfxInstance.distanceToPlayer(pos) < radius * 2.0F) {
                    ScreenFx.flash(d.getFloatOr("flash", 0.0F));
                    ScreenFx.chromatic(d.getFloatOr("flash", 0.0F));
                }
            }
            case VfxPayload.BLACK_HOLE -> VfxManager.add(new BlackHoleVfx(VfxPayload.getVec(d, "pos"), d.getIntOr("form", 0), d.getIntOr("active", 0), d.getIntOr("collapse", 0), d.getFloatOr("radius", 0.0F), d.getIntOr("color", 0)));
            case VfxPayload.BARRAGE -> VfxManager.add(new BarrageVfx(d.getIntOr("id", 0), VfxPayload.getVec(d, "origin"), EdpMath.unpackDirections(d.getIntArray("dirs").orElseGet(() -> new int[0])), d.getDoubleOr("speed", 0.0), d.getIntOr("life", 0), d.getIntOr("color", 0), d.getIntOr("style", 0)));
            case VfxPayload.BARRAGE_HIT -> BarrageVfx.hit(d.getIntOr("id", 0), d.getIntOr("index", 0));
            case VfxPayload.NOVA -> {
                ListTag list = d.getListOrEmpty("zones");
                Vec3[] zones = new Vec3[list.size()];
                for (int i = 0; i < zones.length; i++) {
                    zones[i] = VfxPayload.getVec(list.getCompoundOrEmpty(i), "p");
                }
                VfxManager.add(new NovaVfx(id, VfxPayload.getVec(d, "center"), zones, d.getFloatOr("safeRadius", 0.0F), d.getIntOr("charge", 0), d.getFloatOr("blast", 0.0F), d.getIntOr("color", 0)));
            }
            case VfxPayload.RUPTURE -> {
                ListTag list = d.getListOrEmpty("points");
                Vec3[] points = new Vec3[list.size()];
                int[] delays = new int[list.size()];
                for (int i = 0; i < points.length; i++) {
                    CompoundTag point = list.getCompoundOrEmpty(i);
                    points[i] = VfxPayload.getVec(point, "p");
                    delays[i] = point.getIntOr("delay", 0);
                }
                VfxManager.add(new RuptureVfx(points, delays, d.getFloatOr("radius", 0.0F), d.getIntOr("color", 0)));
            }
            case VfxPayload.STORM_STRIKE -> VfxManager.add(new StormStrikeVfx(VfxPayload.getVec(d, "pos"), d.getIntOr("delay", 0), d.getFloatOr("radius", 0.0F), d.getIntOr("color", 0)));
            case VfxPayload.RING_WAVES -> VfxManager.add(new RingWavesVfx(VfxPayload.getVec(d, "center"), d.getIntOr("count", 0), d.getIntOr("interval", 0), d.getFloatOr("speed", 0.0F), d.getFloatOr("maxRadius", 0.0F), d.getIntOr("color", 0)));
            case VfxPayload.LANCE -> VfxManager.add(new LanceVfx(VfxPayload.getVec(d, "origin"), VfxPayload.getVec(d, "end"), d.getIntOr("charge", 0), d.getIntOr("color", 0)));
            case VfxPayload.DIVE -> VfxManager.add(new DiveVfx(id, VfxPayload.getVec(d, "mark"), d.getIntOr("warn", 0), d.getIntOr("dive", 0), d.getFloatOr("radius", 0.0F), d.getIntOr("color", 0)));
            default -> {
            }
        }
    }
}
