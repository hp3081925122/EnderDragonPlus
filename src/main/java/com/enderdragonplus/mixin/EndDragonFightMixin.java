package com.enderdragonplus.mixin;

import com.enderdragonplus.EnderDragonPlus;
import com.enderdragonplus.dragon.DragonBrainHolder;
import com.enderdragonplus.item.EndingCrystalItem;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.dimension.end.DragonRespawnStage;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonFight.class)
public abstract class EndDragonFightMixin {
    @Shadow
    private List<EntityReference<EndCrystal>> respawnCrystals;

    @Shadow
    private ServerLevel level;

    @Unique
    private boolean edp$pendingEnhanced;

    @Inject(method = "setRespawnStage", at = @At("HEAD"))
    private void edp$checkCrystals(DragonRespawnStage stage, CallbackInfo ci) {
        if (stage != DragonRespawnStage.END || this.respawnCrystals == null) {
            return;
        }
        for (EntityReference<EndCrystal> reference : this.respawnCrystals) {
            EndCrystal crystal = reference.getEntity(this.level, EndCrystal.class);
            if (crystal != null && EndingCrystalItem.isEndingCrystal(crystal)) {
                this.edp$pendingEnhanced = true;
                break;
            }
        }
        EnderDragonPlus.LOGGER.debug("Dragon respawn finishing, ending crystal present: {}", this.edp$pendingEnhanced);
    }

    @Inject(method = "setRespawnStage", at = @At("RETURN"))
    private void edp$clearPending(DragonRespawnStage stage, CallbackInfo ci) {
        this.edp$pendingEnhanced = false;
    }

    @ModifyArg(method = "createNewDragon", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private Entity edp$markDragon(Entity entity) {
        if (this.edp$pendingEnhanced && entity instanceof DragonBrainHolder holder) {
            holder.edp$getBrain().markEnhanced();
            EnderDragonPlus.LOGGER.info("Enhanced ender dragon summoned with ending crystal");
        }
        return entity;
    }
}
