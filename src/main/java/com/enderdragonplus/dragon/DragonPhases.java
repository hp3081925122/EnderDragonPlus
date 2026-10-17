package com.enderdragonplus.dragon;

import com.enderdragonplus.dragon.phase.BlackHolePhase;
import com.enderdragonplus.dragon.phase.VoidRupturePhase;
import com.enderdragonplus.dragon.phase.WingBladesPhase;
import com.enderdragonplus.dragon.phase.EnderStormPhase;
import com.enderdragonplus.dragon.phase.RingWavesPhase;
import com.enderdragonplus.dragon.phase.VoidLancesPhase;
import com.enderdragonplus.dragon.phase.CataclysmDivePhase;
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
    public static final EnderDragonPhase<VoidRupturePhase> VOID_RUPTURE = EnderDragonPhaseInvoker.edp$create(VoidRupturePhase.class, "EdpVoidRupture");
    public static final EnderDragonPhase<WingBladesPhase> WING_BLADES = EnderDragonPhaseInvoker.edp$create(WingBladesPhase.class, "EdpWingBlades");
    public static final EnderDragonPhase<EnderStormPhase> ENDER_STORM = EnderDragonPhaseInvoker.edp$create(EnderStormPhase.class, "EdpEnderStorm");
    public static final EnderDragonPhase<RingWavesPhase> RING_WAVES = EnderDragonPhaseInvoker.edp$create(RingWavesPhase.class, "EdpRingWaves");
    public static final EnderDragonPhase<VoidLancesPhase> VOID_LANCES = EnderDragonPhaseInvoker.edp$create(VoidLancesPhase.class, "EdpVoidLances");
    public static final EnderDragonPhase<CataclysmDivePhase> CATACLYSM_DIVE = EnderDragonPhaseInvoker.edp$create(CataclysmDivePhase.class, "EdpCataclysmDive");

    private DragonPhases() {
    }

    public static void init() {
    }
}
