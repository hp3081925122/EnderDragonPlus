package com.enderdragonplus.dragon;

import com.enderdragonplus.EdpConfig;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;

public enum SkillType {
    VOID_BREATH(1),
    METEOR(1),
    RIFT_DASH(1),
    BLACK_HOLE(2),
    SCALE_BARRAGE(2),
    FINAL_NOVA(3),
    VOID_RUPTURE(1),
    WING_BLADES(1),
    ENDER_STORM(2),
    RING_WAVES(2),
    VOID_LANCES(3),
    CATACLYSM_DIVE(3);

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
            case VOID_RUPTURE -> EdpConfig.VOID_RUPTURE;
            case WING_BLADES -> EdpConfig.WING_BLADES;
            case ENDER_STORM -> EdpConfig.ENDER_STORM;
            case RING_WAVES -> EdpConfig.RING_WAVES;
            case VOID_LANCES -> EdpConfig.VOID_LANCES;
            case CATACLYSM_DIVE -> EdpConfig.CATACLYSM_DIVE;
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
            case VOID_RUPTURE -> DragonPhases.VOID_RUPTURE;
            case WING_BLADES -> DragonPhases.WING_BLADES;
            case ENDER_STORM -> DragonPhases.ENDER_STORM;
            case RING_WAVES -> DragonPhases.RING_WAVES;
            case VOID_LANCES -> DragonPhases.VOID_LANCES;
            case CATACLYSM_DIVE -> DragonPhases.CATACLYSM_DIVE;
        };
    }
}
