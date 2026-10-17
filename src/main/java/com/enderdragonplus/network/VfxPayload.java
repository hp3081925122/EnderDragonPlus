package com.enderdragonplus.network;

import com.enderdragonplus.EnderDragonPlus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

public record VfxPayload(int kind, int entityId, CompoundTag data) {
    public static final int BREATH_CHARGE = 0;
    public static final int BREATH_BEAM = 1;
    public static final int METEOR_SHOWER = 2;
    public static final int RIFT_DASH = 3;
    public static final int SHIELD = 4;
    public static final int SHOCKWAVE = 5;
    public static final int BLACK_HOLE = 6;
    public static final int BARRAGE = 7;
    public static final int BARRAGE_HIT = 8;
    public static final int NOVA = 9;
    public static final int BREATH_AIM = 10;
    public static final int STAGE = 11;
    public static final int RUPTURE = 12;
    public static final int STORM_STRIKE = 13;
    public static final int RING_WAVES = 14;
    public static final int LANCE = 15;
    public static final int DIVE = 16;
    public static final int DEBUG_PROFILE = 17;

    public static void encode(VfxPayload payload, FriendlyByteBuf buffer) {
        buffer.writeVarInt(payload.kind());
        buffer.writeVarInt(payload.entityId());
        buffer.writeNbt(payload.data());
    }

    public static VfxPayload decode(FriendlyByteBuf buffer) {
        int kind = buffer.readVarInt();
        int entityId = buffer.readVarInt();
        CompoundTag data = buffer.readNbt();
        return new VfxPayload(kind, entityId, data == null ? new CompoundTag() : data);
    }

    public static void putVec(CompoundTag tag, String key, Vec3 vec) {
        tag.putDouble(key + "X", vec.x);
        tag.putDouble(key + "Y", vec.y);
        tag.putDouble(key + "Z", vec.z);
    }

    public static Vec3 getVec(CompoundTag tag, String key) {
        return new Vec3(tag.getDouble(key + "X"), tag.getDouble(key + "Y"), tag.getDouble(key + "Z"));
    }
}
