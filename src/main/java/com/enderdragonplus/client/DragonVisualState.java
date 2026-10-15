package com.enderdragonplus.client;

import com.enderdragonplus.EnderDragonPlus;
import net.minecraft.util.context.ContextKey;

public record DragonVisualState(int stage, float glow, float rage) {
    public static final ContextKey<DragonVisualState> KEY = new ContextKey<>(EnderDragonPlus.id("dragon_visuals"));
    public static final DragonVisualState NONE = new DragonVisualState(0, 0, 0);
    public static DragonVisualState of(int id) {
        return new DragonVisualState(DragonStageClient.stage(id), DragonStageClient.glow(id), DragonStageClient.rage(id));
    }
}
