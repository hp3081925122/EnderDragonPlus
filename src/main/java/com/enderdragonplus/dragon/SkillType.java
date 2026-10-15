package com.enderdragonplus.dragon;

import com.enderdragonplus.EdpConfig;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;

public enum SkillType {
    VOID_BREATH(1),
    METEOR(1),
    RIFT_DASH(1),
    BLACK_HOLE(2),
    SCALE_BARRAGE(2),
    FINAL_NOVA(3);

    public final int minStage;

    SkillType(int minStage) {
        this.minStage = minStage;
    }

    public EdpConfig.Skill config() {
        return switch (this) {
            case VOID_BREATH -> EdpConfig.VOID_BREATH;
            case METEOR -> EdpConfig.METEOR;
            case RIFT_DASH -> EdpConfig.RIFT_DASH;
            case BLACK_HOLE -> EdpConfig.BLACK_HOLE;
            case SCALE_BARRAGE -> EdpConfig.SCALE_BARRAGE;
            case FINAL_NOVA -> EdpConfig.FINAL_NOVA;
        };
    }

    public EnderDragonPhase<?> phase() {
        return switch (this) {
            case VOID_BREATH -> DragonPhases.VOID_BREATH;
            case METEOR -> DragonPhases.METEOR;
            case RIFT_DASH -> DragonPhases.RIFT_DASH;
            case BLACK_HOLE -> DragonPhases.BLACK_HOLE;
            case SCALE_BARRAGE -> DragonPhases.SCALE_BARRAGE;
            case FINAL_NOVA -> DragonPhases.FINAL_NOVA;
        };
    }
}
