package com.enderdragonplus.client;

import com.enderdragonplus.client.vfx.DragonAuraVfx;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public final class DragonStageClient {
    public static final int COLOR_STAGE_ONE = 0xB04CFF;
    public static final int COLOR_STAGE_TWO = 0xE04CF0;
    public static final int COLOR_STAGE_THREE = 0xFF2E5A;
    private static final Int2IntOpenHashMap STAGES = new Int2IntOpenHashMap();
    private static final Int2ObjectOpenHashMap<DragonAuraVfx> AURAS = new Int2ObjectOpenHashMap<>();

    private DragonStageClient() {
    }

    public static void set(int dragonId, int stage) {
        STAGES.put(dragonId, stage);
        DragonAuraVfx aura = AURAS.get(dragonId);
        if (aura == null || aura.finished()) {
            aura = new DragonAuraVfx(dragonId);
            AURAS.put(dragonId, aura);
            VfxManager.add(aura);
        }
    }

    public static int stage(int dragonId) {
        return STAGES.getOrDefault(dragonId, 0);
    }

    public static float glow(int dragonId) {
        DragonAuraVfx aura = AURAS.get(dragonId);
        if (aura == null) {
            return 0.0F;
        }
        return aura.strength();
    }

    public static float rage(int dragonId) {
        DragonAuraVfx aura = AURAS.get(dragonId);
        if (aura == null) {
            return 0.0F;
        }
        return aura.rage();
    }

    public static int color(int stage) {
        if (stage >= 3) {
            return COLOR_STAGE_THREE;
        }
        if (stage == 2) {
            return COLOR_STAGE_TWO;
        }
        return COLOR_STAGE_ONE;
    }

    public static void remove(int dragonId) {
        STAGES.remove(dragonId);
        AURAS.remove(dragonId);
    }

    public static void clear() {
        STAGES.clear();
        AURAS.clear();
    }
}
