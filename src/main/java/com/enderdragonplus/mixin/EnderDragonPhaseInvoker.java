package com.enderdragonplus.mixin;

import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EnderDragonPhase.class)
public interface EnderDragonPhaseInvoker {
    @Invoker("create")
    static <T extends DragonPhaseInstance> EnderDragonPhase<T> edp$create(Class<T> phaseClass, String name) {
        throw new AssertionError();
    }
}
