package com.enderdragonplus.mixin;

import com.enderdragonplus.dragon.DragonBrainHolder;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonHoldingPatternPhase;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DragonHoldingPatternPhase.class)
public abstract class DragonHoldingPatternPhaseMixin extends AbstractDragonPhaseInstance {
    protected DragonHoldingPatternPhaseMixin(EnderDragon dragon) {
        super(dragon);
    }

    @Inject(method = "strafePlayer", at = @At("HEAD"), cancellable = true)
    private void edp$replaceStrafe(Player player, CallbackInfo ci) {
        if (((DragonBrainHolder) this.dragon).edp$getBrain().tryStartSkill(player)) {
            ci.cancel();
        }
    }
}
