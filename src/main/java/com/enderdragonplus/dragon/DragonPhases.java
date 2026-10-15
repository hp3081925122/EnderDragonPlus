package com.enderdragonplus.dragon;

import com.enderdragonplus.EnderDragonPlus;
import com.enderdragonplus.dragon.phase.BlackHolePhase;
import com.enderdragonplus.dragon.phase.FinalNovaPhase;
import com.enderdragonplus.dragon.phase.MeteorPhase;
import com.enderdragonplus.dragon.phase.RiftDashPhase;
import com.enderdragonplus.dragon.phase.ScaleBarragePhase;
import com.enderdragonplus.dragon.phase.ShieldAscensionPhase;
import com.enderdragonplus.dragon.phase.VoidBreathPhase;
import com.enderdragonplus.mixin.EnderDragonPhaseInvoker;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;

public final class DragonPhases {
    public static final EnderDragonPhase<VoidBreathPhase> VOID_BREATH = EnderDragonPhaseInvoker.edp$create(VoidBreathPhase.class, "EdpVoidBreath");
    public static final EnderDragonPhase<MeteorPhase> METEOR = EnderDragonPhaseInvoker.edp$create(MeteorPhase.class, "EdpMeteor");
    public static final EnderDragonPhase<RiftDashPhase> RIFT_DASH = EnderDragonPhaseInvoker.edp$create(RiftDashPhase.class, "EdpRiftDash");
    public static final EnderDragonPhase<ShieldAscensionPhase> SHIELD_ASCENSION = EnderDragonPhaseInvoker.edp$create(ShieldAscensionPhase.class, "EdpShieldAscension");
    public static final EnderDragonPhase<BlackHolePhase> BLACK_HOLE = EnderDragonPhaseInvoker.edp$create(BlackHolePhase.class, "EdpBlackHole");
    public static final EnderDragonPhase<ScaleBarragePhase> SCALE_BARRAGE = EnderDragonPhaseInvoker.edp$create(ScaleBarragePhase.class, "EdpScaleBarrage");
    public static final EnderDragonPhase<FinalNovaPhase> FINAL_NOVA = EnderDragonPhaseInvoker.edp$create(FinalNovaPhase.class, "EdpFinalNova");

    private DragonPhases() {
    }

    public static void init() {
        EnderDragonPlus.LOGGER.info("Registered custom dragon phases, total phase count {}", EnderDragonPhase.getCount());
    }
}
