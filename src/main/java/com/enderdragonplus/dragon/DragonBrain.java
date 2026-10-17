package com.enderdragonplus.dragon;

import com.enderdragonplus.EdpConfig;
import com.enderdragonplus.dragon.hazard.Hazard;
import com.enderdragonplus.dragon.phase.SkillPhase;
import com.enderdragonplus.network.VfxPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.enderdragonplus.network.EdpNetwork;

public final class DragonBrain {
    public static final int COLOR_STAGE_ONE = 0xB04CFF;
    public static final int COLOR_STAGE_TWO = 0xE04CF0;
    public static final int COLOR_STAGE_THREE = 0xFF2E5A;
    private static final EntityTypeTest<Entity, LivingEntity> VICTIM_TYPE = EntityTypeTest.forClass(LivingEntity.class);

    private final EnderDragon dragon;
    private final int[] cooldowns = new int[SkillType.values().length];
    private final List<Hazard> hazards = new ArrayList<>();
    private final List<LivingEntity> firstVictimResult = new ArrayList<>(1);
    private final Predicate<LivingEntity> victimFilter;
    private int globalCooldown = 100;
    private boolean stageTwoReached;
    private boolean stageThreeReached;
    private boolean enhanced;
    @Nullable
    private LivingEntity target;

    public DragonBrain(EnderDragon dragon) {
        this.dragon = dragon;
        this.victimFilter = e -> e != this.dragon && e.isAlive() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(e);
    }

    public EnderDragon dragon() {
        return this.dragon;
    }

    public ServerLevel level() {
        return (ServerLevel) this.dragon.level();
    }

    public void tick() {
        ServerLevel level = this.level();
        this.hazards.removeIf(h -> !h.tick(level, this));
        for (int i = 0; i < this.cooldowns.length; i++) {
            if (this.cooldowns[i] > 0) {
                this.cooldowns[i]--;
            }
        }
        if (this.globalCooldown > 0) {
            this.globalCooldown--;
        }
        DragonPhaseInstance current = this.dragon.getPhaseManager().getCurrentPhase();
        if (current.getPhase() == EnderDragonPhase.DYING) {
            this.hazards.clear();
            return;
        }
        if (current instanceof SkillPhase) {
            return;
        }
        float ratio = this.dragon.getHealth() / this.dragon.getMaxHealth();
        if (!this.stageTwoReached && ratio <= EdpConfig.STAGE_TWO_HEALTH.get()) {
            this.stageTwoReached = true;
            EdpNetwork.sendToPlayersTrackingEntity(this.dragon, this.stagePayload());
            this.dragon.getPhaseManager().setPhase(DragonPhases.SHIELD_ASCENSION);
            return;
        }
        if (!this.stageThreeReached && ratio <= EdpConfig.STAGE_THREE_HEALTH.get()) {
            this.stageThreeReached = true;
            this.stageTwoReached = true;
            EdpNetwork.sendToPlayersTrackingEntity(this.dragon, this.stagePayload());
            if (EdpConfig.FINAL_NOVA.enabled.get()) {
                this.startSkill(SkillType.FINAL_NOVA, this.pickTarget());
                return;
            }
        }
        if (current.getPhase() == EnderDragonPhase.HOLDING_PATTERN && this.globalCooldown <= 0) {
            LivingEntity candidate = this.pickTarget();
            if (candidate != null) {
                this.tryStartSkill(candidate);
            }
        }
    }

    public int stage() {
        if (this.stageThreeReached) {
            return 3;
        }
        if (this.stageTwoReached) {
            return 2;
        }
        return 1;
    }

    public VfxPayload stagePayload() {
        CompoundTag data = new CompoundTag();
        data.putInt("stage", this.stage());
        return new VfxPayload(VfxPayload.STAGE, this.dragon.getId(), data);
    }

    public int stageColor() {
        int stage = this.stage();
        if (stage == 3) {
            return COLOR_STAGE_THREE;
        }
        if (stage == 2) {
            return COLOR_STAGE_TWO;
        }
        return COLOR_STAGE_ONE;
    }

    public double cooldownMultiplier() {
        if (this.stage() == 3) {
            return EdpConfig.RAGE_COOLDOWN_MULTIPLIER.get();
        }
        return 1.0;
    }

    public boolean active() {
        return !EdpConfig.REQUIRE_SPECIAL_SUMMON.get() || this.enhanced;
    }

    public void markEnhanced() {
        this.enhanced = true;
    }

    public boolean tryStartSkill(LivingEntity candidate) {
        if (!this.active()) {
            return false;
        }
        if (this.globalCooldown > 0) {
            return false;
        }
        int stage = this.stage();
        List<SkillType> options = new ArrayList<>();
        int total = 0;
        for (SkillType type : SkillType.values()) {
            EdpConfig.Skill config = type.config();
            if (config.enabled.get() && stage >= type.minStage && this.cooldowns[type.ordinal()] <= 0 && config.weight.get() > 0) {
                options.add(type);
                total += config.weight.get();
            }
        }
        if (options.isEmpty()) {
            return false;
        }
        int roll = this.dragon.getRandom().nextInt(total);
        SkillType chosen = options.get(options.size() - 1);
        for (SkillType type : options) {
            roll -= type.config().weight.get();
            if (roll < 0) {
                chosen = type;
                break;
            }
        }
        this.startSkill(chosen, candidate);
        return true;
    }

    public void startSkill(SkillType type, @Nullable LivingEntity candidate) {
        this.target = candidate;
        double multiplier = this.cooldownMultiplier();
        this.cooldowns[type.ordinal()] = (int) (type.config().cooldown.get() * multiplier);
        this.globalCooldown = (int) (EdpConfig.ACTION_INTERVAL.get() * multiplier);

        this.dragon.getPhaseManager().setPhase(type.phase());
    }

    @Nullable
    public LivingEntity validTarget() {
        if (this.target != null && this.target.isAlive() && !this.target.isRemoved() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(this.target)
                && this.target.distanceToSqr(this.dragon) < this.rangeSqr()) {
            return this.target;
        }
        this.target = this.pickTarget();
        return this.target;
    }

    @Nullable
    public LivingEntity pickTarget() {
        List<LivingEntity> targets = this.combatTargets();
        if (targets.isEmpty()) {
            return null;
        }
        return targets.get(this.dragon.getRandom().nextInt(targets.size()));
    }

    public List<LivingEntity> combatTargets() {
        return new ArrayList<>(this.combatPlayers());
    }

    public List<ServerPlayer> combatPlayers() {
        double range = this.rangeSqr();
        return this.level().getPlayers(p -> p.isAlive() && !p.isSpectator() && !p.isCreative() && p.distanceToSqr(this.dragon) < range);
    }

    private double rangeSqr() {
        double range = EdpConfig.COMBAT_RANGE.get();
        return range * range;
    }

    public double attack() {
        return this.dragon.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    public boolean damage(LivingEntity victim, double multiplier) {
        return victim.hurt(this.dragon.damageSources().mobAttack(this.dragon), (float) (this.attack() * multiplier));
    }

    public static void push(Entity entity, double x, double y, double z) {
        double scale = EdpConfig.KNOCKBACK_MULTIPLIER.get();
        if (entity instanceof LivingEntity living) {
            scale *= Math.max(0.0, 1.0 - living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        }
        if (scale <= 0.0) {
            return;
        }
        entity.push(x * scale, y * scale, z * scale);
        entity.hurtMarked = true;
    }

    public static void pull(LivingEntity entity, Vec3 velocity) {
        double scale = Math.max(0.0, 1.0 - entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        if (scale <= 0.0) {
            return;
        }
        entity.push(velocity.x * scale, velocity.y * scale, velocity.z * scale);
        entity.fallDistance = 0.0F;
        entity.hurtMarked = true;
    }

    public void knockFrom(LivingEntity victim, Vec3 from, double horizontal, double vertical) {
        Vec3 d = new Vec3(victim.getX() - from.x, 0.0, victim.getZ() - from.z);
        if (d.lengthSqr() < 1.0E-4) {
            float a = this.dragon.getRandom().nextFloat() * Mth.TWO_PI;
            d = new Vec3(Mth.cos(a), 0.0, Mth.sin(a));
        }
        d = d.normalize();
        push(victim, d.x * horizontal, vertical, d.z * horizontal);
    }

    public List<LivingEntity> victims(AABB box) {
        return this.level().getEntitiesOfClass(LivingEntity.class, box, this.victimFilter);
    }

    @Nullable
    public LivingEntity firstVictim(AABB box) {
        if (!this.firstVictimResult.isEmpty()) {
            this.firstVictimResult.clear();
        }
        this.level().getEntities(VICTIM_TYPE, box, this.victimFilter, this.firstVictimResult, 1);
        return this.firstVictimResult.isEmpty() ? null : this.firstVictimResult.get(0);
    }

    public Vec3 arenaCenter() {
        BlockPos podium = EndPodiumFeature.getLocation(this.dragon.getFightOrigin());
        double sum = 0.0;
        int count = 0;
        for (int i = 0; i < 8; i++) {
            float angle = i * Mth.TWO_PI / 8.0F;
            int y = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, podium.getX() + Mth.floor(Mth.cos(angle) * 10.0F), podium.getZ() + Mth.floor(Mth.sin(angle) * 10.0F));
            if (y > this.level().getMinBuildHeight() + 1) {
                sum += y;
                count++;
            }
        }
        double groundY;
        if (count > 0) {
            groundY = sum / count;
        } else {
            groundY = this.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, podium).getY();
        }
        return new Vec3(podium.getX() + 0.5, groundY, podium.getZ() + 0.5);
    }

    public double groundY(double x, double z, double fallback) {
        int y = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
        if (y <= this.level().getMinBuildHeight() + 1) {
            return fallback;
        }
        return y;
    }

    public Vec3 mouthPos() {
        Vec3 head = new Vec3(this.dragon.head.getX(), this.dragon.head.getY(0.5), this.dragon.head.getZ());
        return EdpMath.mouth(head, this.dragon.getYRot());
    }

    public Vec3 bodyCenter() {
        return this.dragon.position().add(0.0, 2.0, 0.0);
    }

    public void sendVfx(int type, CompoundTag data) {
        EdpNetwork.sendToPlayersNear(this.level(), null, this.dragon.getX(), this.dragon.getY(), this.dragon.getZ(), 320.0, new VfxPayload(type, this.dragon.getId(), data));
    }

    public void playSound(Vec3 pos, SoundEvent sound, float volume, float pitch) {
        this.level().playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.HOSTILE, volume, pitch);
    }

    public void addHazard(Hazard hazard) {
        this.hazards.add(hazard);
    }

    public void save(CompoundTag tag) {
        tag.putBoolean("EdpStageTwo", this.stageTwoReached);
        tag.putBoolean("EdpStageThree", this.stageThreeReached);
        tag.putBoolean("EdpEnhanced", this.enhanced);
    }

    public void load(CompoundTag tag) {
        this.stageTwoReached = tag.getBoolean("EdpStageTwo");
        this.stageThreeReached = tag.getBoolean("EdpStageThree");
        this.enhanced = tag.getBoolean("EdpEnhanced");
    }
}
