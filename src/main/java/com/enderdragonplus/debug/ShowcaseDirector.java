package com.enderdragonplus.debug;

import com.enderdragonplus.EdpConfig;
import com.enderdragonplus.EnderDragonPlus;
import com.enderdragonplus.dragon.DragonBrain;
import com.enderdragonplus.dragon.DragonBrainHolder;
import com.enderdragonplus.dragon.SkillType;
import com.enderdragonplus.dragon.phase.SkillPhase;
import com.enderdragonplus.network.VfxPayload;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ShowcaseDirector {
    public static final int ACTION_START = 0;
    public static final int ACTION_STOP = 1;
    public static final int ACTION_CAPTION = 2;
    public static final int SHOT_NORMAL = 0;
    public static final int SHOT_WIDE = 1;
    public static final int SHOT_CLOSE = 2;
    private static final int STEP_WAIT = 0;
    private static final int STEP_SKILL = 1;
    private static final int STEP_STAGE = 2;
    private static final int MOB_CAP = 90;
    private static final int SPAWN_INTERVAL = 4;
    private static final int SPAWN_BATCH = 4;
    private static final double SPAWN_MIN = 6.0;
    private static final double SPAWN_MAX = 44.0;
    private static final int GAP = 30;
    private static final int STEP_TIMEOUT = 900;
    private static final EntityType<?>[] MOBS = {
            EntityType.ZOMBIE, EntityType.HUSK, EntityType.SKELETON, EntityType.STRAY, EntityType.SPIDER,
            EntityType.CREEPER, EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.WITCH, EntityType.ZOMBIE_VILLAGER
    };

    @Nullable
    private static ShowcaseDirector active;

    private final ServerPlayer player;
    private final EnderDragon dragon;
    private final DragonBrain brain;
    private final boolean captions;
    private final GameType previousMode;
    private final boolean previousLoot;
    private final List<int[]> steps = new ArrayList<>();
    private final List<SkillType> skills = new ArrayList<>();
    private int index;
    private int timer;
    private int gap;
    private boolean started;
    private int ticks;
    private int mobCount;

    private ShowcaseDirector(ServerPlayer player, EnderDragon dragon, boolean captions) {
        this.player = player;
        this.dragon = dragon;
        this.brain = ((DragonBrainHolder) dragon).edp$getBrain();
        this.captions = captions;
        this.previousMode = player.gameMode.getGameModeForPlayer();
        this.previousLoot = player.serverLevel().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);
        this.steps.add(new int[]{STEP_WAIT, 120});
        this.addSkills(SkillType.VOID_BREATH, SkillType.METEOR, SkillType.RIFT_DASH, SkillType.VOID_RUPTURE, SkillType.WING_BLADES);
        this.steps.add(new int[]{STEP_STAGE, 2});
        this.addSkills(SkillType.BLACK_HOLE, SkillType.SCALE_BARRAGE, SkillType.ENDER_STORM, SkillType.RING_WAVES);
        this.steps.add(new int[]{STEP_STAGE, 3});
        this.addSkills(SkillType.VOID_LANCES, SkillType.CATACLYSM_DIVE);
        this.steps.add(new int[]{STEP_WAIT, 120});
    }

    private void addSkills(SkillType... types) {
        for (SkillType type : types) {
            this.steps.add(new int[]{STEP_SKILL, this.skills.size()});
            this.skills.add(type);
        }
    }

    public static boolean running() {
        return active != null;
    }

    @Nullable
    public static EnderDragon findDragon(ServerPlayer player) {
        EnderDragon best = null;
        double bestDist = Double.MAX_VALUE;
        for (EnderDragon dragon : player.serverLevel().getDragons()) {
            if (!dragon.isAlive() || !((DragonBrainHolder) dragon).edp$getBrain().active()) {
                continue;
            }
            double d = dragon.distanceToSqr(player);
            if (d < bestDist) {
                bestDist = d;
                best = dragon;
            }
        }
        return best;
    }

    public static void start(ServerPlayer player, EnderDragon dragon, boolean captions) {
        if (active != null) {
            active.stop("restarted");
        }
        ShowcaseDirector director = new ShowcaseDirector(player, dragon, captions);
        active = director;
        player.serverLevel().getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(false, player.server);
        player.setGameMode(GameType.SPECTATOR);
        director.brain.setShowcase(true);
        dragon.setHealth(dragon.getMaxHealth());
        director.brain.resetStages();
        dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
        CompoundTag data = new CompoundTag();
        data.putInt("action", ACTION_START);
        data.putBoolean("captions", captions);
        director.send(data);
    }

    public static void stopActive(String reason) {
        if (active != null) {
            active.stop(reason);
        }
    }

    public static void tickActive() {
        if (active != null) {
            active.tick();
        }
    }

    private void send(CompoundTag data) {
        if (this.player.connection != null) {
            PacketDistributor.sendToPlayer(this.player, new VfxPayload(VfxPayload.SHOWCASE, this.dragon.getId(), data));
        }
    }

    private void caption(String title, String subtitle, int shot) {
        CompoundTag data = new CompoundTag();
        data.putInt("action", ACTION_CAPTION);
        data.putString("title", title);
        data.putString("subtitle", subtitle);
        data.putInt("color", this.brain.stageColor());
        data.putInt("shot", shot);
        data.putBoolean("show", this.captions);
        this.send(data);
    }

    private void stop(String reason) {
        active = null;
        ServerLevel level = this.player.serverLevel();
        this.brain.setShowcase(false);
        List<? extends Mob> mobs = level.getEntities(EntityTypeTest.forClass(Mob.class), e -> e.getTags().contains(DragonBrain.SHOWCASE_TAG));
        for (Mob mob : mobs) {
            mob.discard();
        }
        level.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(this.previousLoot, level.getServer());
        if (!this.player.isRemoved()) {
            this.player.setGameMode(this.previousMode);
        }
        CompoundTag data = new CompoundTag();
        data.putInt("action", ACTION_STOP);
        this.send(data);
    }

    private void tick() {
        if (!this.dragon.isAlive() || this.dragon.isRemoved() || this.player.isRemoved() || this.player.level() != this.dragon.level()) {
            this.stop("dragon or player unavailable");
            return;
        }
        this.ticks++;
        this.spawnMobs();
        if (this.index >= this.steps.size()) {
            this.stop("finished");
            return;
        }
        int[] step = this.steps.get(this.index);
        if (step[0] == STEP_WAIT) {
            this.timer++;
            if (this.timer >= step[1]) {
                this.next();
            }
            return;
        }
        if (!this.started) {
            if (step[0] == STEP_SKILL) {
                this.beginSkill(this.skills.get(step[1]));
            } else {
                this.beginStage(step[1]);
            }
            return;
        }
        this.timer++;
        boolean busy = this.dragon.getPhaseManager().getCurrentPhase() instanceof SkillPhase;
        if (this.timer > 10 && !busy) {
            this.gap++;
        }
        if (this.gap >= GAP || this.timer >= STEP_TIMEOUT) {
            if (this.timer >= STEP_TIMEOUT) {
                EnderDragonPlus.LOGGER.warn("Showcase step {} timed out", this.index);
            }
            this.next();
        }
    }

    private void next() {
        this.index++;
        this.timer = 0;
        this.gap = 0;
        this.started = false;
    }

    private void beginSkill(SkillType type) {
        LivingEntity target = this.pickTarget();
        if (target == null) {
            this.timer++;
            if (this.timer > 200) {
                EnderDragonPlus.LOGGER.warn("Showcase skipped {} because no mob target was found", type);
                this.next();
            }
            return;
        }
        this.started = true;
        this.timer = 0;
        int shot = SHOT_NORMAL;
        if (type == SkillType.RIFT_DASH || type == SkillType.WING_BLADES || type == SkillType.VOID_BREATH || type == SkillType.VOID_LANCES) {
            shot = SHOT_CLOSE;
        } else if (type == SkillType.METEOR || type == SkillType.RING_WAVES || type == SkillType.CATACLYSM_DIVE || type == SkillType.ENDER_STORM || type == SkillType.BLACK_HOLE) {
            shot = SHOT_WIDE;
        }
        this.brain.startSkill(type, target);
        this.caption(title(type), subtitle(type), shot);

    }

    private void beginStage(int stage) {
        double ratio = EdpConfig.STAGE_TWO_HEALTH.get();
        String title = "第二阶段";
        String subtitle = "STAGE II  ·  护盾升华";
        if (stage >= 3) {
            ratio = EdpConfig.STAGE_THREE_HEALTH.get();
            title = "狂暴 · 终焉新星";
            subtitle = "STAGE III  ·  FINAL NOVA";
        }
        this.dragon.setHealth((float) Math.max(1.0, this.dragon.getMaxHealth() * (ratio - 0.01)));
        this.started = true;
        this.timer = 0;
        this.caption(title, subtitle, SHOT_WIDE);
    }

    @Nullable
    private LivingEntity pickTarget() {
        List<LivingEntity> targets = this.brain.combatTargets();
        if (targets.isEmpty()) {
            return null;
        }
        Vec3 ground = new Vec3(this.dragon.getX(), this.brain.groundY(this.dragon.getX(), this.dragon.getZ(), this.dragon.getY()), this.dragon.getZ());
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        RandomSource random = this.dragon.getRandom();
        for (LivingEntity target : targets) {
            double score = target.position().distanceTo(ground) + random.nextDouble() * 12.0;
            if (score < bestScore) {
                bestScore = score;
                best = target;
            }
        }
        return best;
    }

    private void spawnMobs() {
        ServerLevel level = this.player.serverLevel();
        if (this.ticks % 20 == 1) {
            this.mobCount = level.getEntitiesOfClass(Mob.class, new AABB(this.dragon.blockPosition()).inflate(160.0, 128.0, 160.0), e -> e.getTags().contains(DragonBrain.SHOWCASE_TAG)).size();
        }
        if (this.ticks % SPAWN_INTERVAL != 0 || this.mobCount >= MOB_CAP) {
            return;
        }
        RandomSource random = this.dragon.getRandom();
        double reference = this.brain.arenaCenter().y;
        for (int i = 0; i < SPAWN_BATCH && this.mobCount < MOB_CAP; i++) {
            for (int attempt = 0; attempt < 6; attempt++) {
                float angle = random.nextFloat() * Mth.TWO_PI;
                double radius = SPAWN_MIN + random.nextDouble() * (SPAWN_MAX - SPAWN_MIN);
                int x = Mth.floor(this.dragon.getX() + Mth.cos(angle) * radius);
                int z = Mth.floor(this.dragon.getZ() + Mth.sin(angle) * radius);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (y <= level.getMinBuildHeight() + 1 || y > reference + 8.0 || y < reference - 20.0) {
                    continue;
                }
                EntityType<?> type = MOBS[random.nextInt(MOBS.length)];
                Entity entity = type.spawn(level, new BlockPos(x, y, z), MobSpawnType.COMMAND);
                if (entity instanceof Mob mob) {
                    mob.addTag(DragonBrain.SHOWCASE_TAG);
                    mob.setPersistenceRequired();
                    this.mobCount++;
                }
                break;
            }
        }
    }

    private static String title(SkillType type) {
        return switch (type) {
            case VOID_BREATH -> "虚空吐息";
            case METEOR -> "末影陨星";
            case RIFT_DASH -> "空间裂隙冲刺";
            case BLACK_HOLE -> "黑洞";
            case SCALE_BARRAGE -> "龙鳞弹幕";
            case FINAL_NOVA -> "终焉新星";
            case VOID_RUPTURE -> "虚空地脉";
            case WING_BLADES -> "翼刃风暴";
            case ENDER_STORM -> "末影雷暴";
            case RING_WAVES -> "裂空环波";
            case VOID_LANCES -> "虚空长枪";
            case CATACLYSM_DIVE -> "陨灭俯冲";
        };
    }

    private static String subtitle(SkillType type) {
        return switch (type) {
            case VOID_BREATH -> "VOID BREATH";
            case METEOR -> "ENDER METEOR";
            case RIFT_DASH -> "RIFT DASH";
            case BLACK_HOLE -> "BLACK HOLE";
            case SCALE_BARRAGE -> "SCALE BARRAGE";
            case FINAL_NOVA -> "FINAL NOVA";
            case VOID_RUPTURE -> "VOID RUPTURE";
            case WING_BLADES -> "WING BLADES";
            case ENDER_STORM -> "ENDER STORM";
            case RING_WAVES -> "RIFT RING WAVES";
            case VOID_LANCES -> "VOID LANCES";
            case CATACLYSM_DIVE -> "CATACLYSM DIVE";
        };
    }
}
