package com.enderdragonplus.dragon.hazard;

import com.enderdragonplus.dragon.DragonBrain;
import net.minecraft.server.level.ServerLevel;

public abstract class Hazard {
    protected int age;

    public abstract boolean tick(ServerLevel level, DragonBrain brain);
}
