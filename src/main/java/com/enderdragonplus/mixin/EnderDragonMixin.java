package com.enderdragonplus.mixin;

import com.enderdragonplus.EdpConfig;
import com.enderdragonplus.dragon.DragonBrain;
import com.enderdragonplus.dragon.DragonBrainHolder;
import com.enderdragonplus.dragon.phase.SkillPhase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin extends Mob implements DragonBrainHolder {
    @Unique
    private DragonBrain edp$brain;

    protected EnderDragonMixin(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    @Override
    public DragonBrain edp$getBrain() {
        if (this.edp$brain == null) {
            this.edp$brain = new DragonBrain((EnderDragon) (Object) this);
        }
        return this.edp$brain;
    }

    @Inject(method = "hurt(Lnet/minecraft/world/entity/boss/EnderDragonPart;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void edp$showcaseImmune(EnderDragonPart part, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!this.level().isClientSide && this.edp$getBrain().showcase() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            cir.setReturnValue(false);
        }
    }

    @ModifyArg(method = "checkCrystals", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;setHealth(F)V"))
    private float edp$capCrystalHeal(float health) {
        if (this.level().isClientSide || !EdpConfig.STAGE_HEAL_CAP.get() || !this.edp$getBrain().active() || this.edp$getBrain().stage() < 2) {
            return health;
        }
        float cap = (float) (this.getMaxHealth() * EdpConfig.STAGE_TWO_HEALTH.get());
        return Math.min(health, Math.max(cap, this.getHealth()));
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void edp$tickBrain(CallbackInfo ci) {
        if (!this.level().isClientSide && !this.isDeadOrDying() && !this.isNoAi() && this.edp$getBrain().active()) {
            this.edp$getBrain().tick();
        }
    }

    @ModifyArg(method = "hurt(Ljava/util/List;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), index = 1)
    private float edp$headDamage(float original) {
        if (!this.edp$getBrain().active()) {
            return original;
        }
        return (float) (this.getAttributeValue(Attributes.ATTACK_DAMAGE) * EdpConfig.HEAD_CONTACT_MULTIPLIER.get());
    }

    @ModifyArg(method = "knockBack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"), index = 1)
    private float edp$wingDamage(float original) {
        if (!this.edp$getBrain().active()) {
            return original;
        }
        return (float) (this.getAttributeValue(Attributes.ATTACK_DAMAGE) * EdpConfig.WING_CONTACT_MULTIPLIER.get());
    }

    @Redirect(method = "knockBack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(DDD)V"))
    private void edp$scaledPush(Entity entity, double x, double y, double z) {
        if (!this.edp$getBrain().active()) {
            entity.push(x, y, z);
            return;
        }
        DragonBrain.push(entity, x, y, z);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void edp$save(CompoundTag tag, CallbackInfo ci) {
        EnderDragon self = (EnderDragon) (Object) this;
        if (self.getPhaseManager().getCurrentPhase() instanceof SkillPhase) {
            tag.putInt("DragonPhase", EnderDragonPhase.HOLDING_PATTERN.getId());
        }
        this.edp$getBrain().save(tag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void edp$load(CompoundTag tag, CallbackInfo ci) {
        this.edp$getBrain().load(tag);
    }
}
