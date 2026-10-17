package com.enderdragonplus;

import net.minecraftforge.common.ForgeConfigSpec;

public final class EdpConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;

    public static final ForgeConfigSpec.BooleanValue REQUIRE_SPECIAL_SUMMON;
    public static final ForgeConfigSpec.DoubleValue MAX_HEALTH;
    public static final ForgeConfigSpec.DoubleValue ATTACK_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue HEAD_CONTACT_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue WING_CONTACT_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue KNOCKBACK_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue STAGE_TWO_HEALTH;
    public static final ForgeConfigSpec.DoubleValue STAGE_THREE_HEALTH;
    public static final ForgeConfigSpec.IntValue ACTION_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue RAGE_COOLDOWN_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue COMBAT_RANGE;

    public static final Skill VOID_BREATH;
    public static final ForgeConfigSpec.DoubleValue BREATH_SCORCH_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue BREATH_TRACK_SPEED;

    public static final Skill METEOR;
    public static final ForgeConfigSpec.IntValue METEOR_COUNT;
    public static final ForgeConfigSpec.DoubleValue METEOR_RADIUS;
    public static final ForgeConfigSpec.BooleanValue METEOR_BREAK_BLOCKS;

    public static final Skill RIFT_DASH;

    public static final ForgeConfigSpec.IntValue SHIELD_DURATION;
    public static final ForgeConfigSpec.DoubleValue SHIELD_BURST_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue SHIELD_REVIVE_CRYSTALS;
    public static final ForgeConfigSpec.DoubleValue SHIELD_CRYSTAL_EXPLODE_RATIO;
    public static final ForgeConfigSpec.BooleanValue STAGE_HEAL_CAP;

    public static final Skill BLACK_HOLE;
    public static final ForgeConfigSpec.DoubleValue BLACK_HOLE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue BLACK_HOLE_PULL;

    public static final Skill SCALE_BARRAGE;
    public static final ForgeConfigSpec.IntValue BARRAGE_WAVES;
    public static final ForgeConfigSpec.IntValue BARRAGE_PER_WAVE;

    public static final Skill FINAL_NOVA;
    public static final ForgeConfigSpec.IntValue NOVA_CHARGE_TICKS;
    public static final ForgeConfigSpec.IntValue NOVA_SAFE_ZONES;
    public static final ForgeConfigSpec.DoubleValue NOVA_SAFE_RADIUS;

    public static final Skill VOID_RUPTURE;
    public static final Skill WING_BLADES;
    public static final ForgeConfigSpec.IntValue WING_BLADES_PER_VOLLEY;
    public static final Skill ENDER_STORM;
    public static final ForgeConfigSpec.IntValue STORM_STRIKES;
    public static final Skill RING_WAVES;
    public static final Skill VOID_LANCES;
    public static final ForgeConfigSpec.IntValue LANCE_COUNT;
    public static final Skill CATACLYSM_DIVE;
    public static final ForgeConfigSpec.DoubleValue DIVE_RADIUS;

    public static final ForgeConfigSpec.BooleanValue ENABLE_DISTORTION;
    public static final ForgeConfigSpec.BooleanValue OPTIMIZE_HEAVY_EFFECTS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SCREEN_EFFECTS;
    public static final ForgeConfigSpec.DoubleValue CAMERA_SHAKE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("末影龙基础属性").push("dragon");
        REQUIRE_SPECIAL_SUMMON = b.comment("是否需要用终焉水晶特殊召唤才会出现加强版末影龙；false 时所有末影龙都是加强版").define("requireSpecialSummon", false);
        MAX_HEALTH = b.comment("末影龙最大生命值").defineInRange("maxHealth", 400.0, 1.0, 1000000.0);
        ATTACK_DAMAGE = b.comment("末影龙基础攻击力，所有技能伤害 = 基础攻击力 × 技能倍率").defineInRange("attackDamage", 10.0, 0.0, 10000.0);
        HEAD_CONTACT_MULTIPLIER = b.comment("头部撞击伤害倍率").defineInRange("headContactMultiplier", 1.0, 0.0, 100.0);
        WING_CONTACT_MULTIPLIER = b.comment("翅膀拍击伤害倍率").defineInRange("wingContactMultiplier", 0.5, 0.0, 100.0);
        KNOCKBACK_MULTIPLIER = b.comment("击退整体倍率，实际击退还会乘以 (1 - 击退抗性)").defineInRange("knockbackMultiplier", 0.5, 0.0, 10.0);
        STAGE_TWO_HEALTH = b.comment("进入第二阶段的血量百分比").defineInRange("stageTwoHealth", 0.5, 0.0, 1.0);
        STAGE_THREE_HEALTH = b.comment("进入第三阶段（狂暴）的血量百分比").defineInRange("stageThreeHealth", 0.25, 0.0, 1.0);
        ACTION_INTERVAL = b.comment("两次技能之间的最短间隔（tick）").defineInRange("actionInterval", 120, 0, 10000);
        RAGE_COOLDOWN_MULTIPLIER = b.comment("狂暴阶段的冷却倍率").defineInRange("rageCooldownMultiplier", 0.6, 0.05, 10.0);
        COMBAT_RANGE = b.comment("索敌范围").defineInRange("combatRange", 150.0, 16.0, 1024.0);
        b.pop();

        b.comment("技能配置").push("skills");
        VOID_BREATH = new Skill(b, "voidBreath", "虚空吐息", 300, 0.35, 10);
        BREATH_SCORCH_MULTIPLIER = b.comment("虚空灼痕每次伤害倍率").defineInRange("voidBreath_scorchMultiplier", 0.15, 0.0, 100.0);
        BREATH_TRACK_SPEED = b.comment("吐息落点追踪玩家的速度（格/tick），玩家疾跑约为 0.28").defineInRange("voidBreath_trackSpeed", 0.27, 0.0, 5.0);

        METEOR = new Skill(b, "meteor", "末影陨星", 360, 1.2, 10);
        METEOR_COUNT = b.comment("陨星数量").defineInRange("meteor_count", 6, 1, 64);
        METEOR_RADIUS = b.comment("陨星爆炸半径").defineInRange("meteor_radius", 4.5, 0.5, 32.0);
        METEOR_BREAK_BLOCKS = b.comment("陨星是否破坏方块").define("meteor_breakBlocks", false);

        RIFT_DASH = new Skill(b, "riftDash", "空间裂隙冲刺", 260, 1.5, 8);

        SHIELD_DURATION = b.comment("转阶段护盾持续时间（tick）").defineInRange("shield_duration", 120, 20, 2400);
        SHIELD_BURST_MULTIPLIER = b.comment("护盾破碎冲击伤害倍率").defineInRange("shield_burstMultiplier", 0.8, 0.0, 100.0);
        SHIELD_REVIVE_CRYSTALS = b.comment("护盾升华时是否复活所有黑曜石柱上的末影水晶").define("shield_reviveCrystals", true);
        SHIELD_CRYSTAL_EXPLODE_RATIO = b.comment("护盾升华结束后随机引爆的水晶比例").defineInRange("shield_crystalExplodeRatio", 0.5, 0.0, 1.0);
        STAGE_HEAL_CAP = b.comment("进入第二阶段后，末影水晶回血不会超过第二阶段血量阈值").define("stageHealCap", true);

        BLACK_HOLE = new Skill(b, "blackHole", "黑洞", 600, 0.4, 7);
        BLACK_HOLE_RADIUS = b.comment("黑洞牵引半径").defineInRange("blackHole_radius", 22.0, 2.0, 128.0);
        BLACK_HOLE_PULL = b.comment("黑洞牵引强度").defineInRange("blackHole_pull", 0.09, 0.0, 2.0);

        SCALE_BARRAGE = new Skill(b, "scaleBarrage", "龙鳞弹幕", 320, 0.5, 9);
        BARRAGE_WAVES = b.comment("弹幕波数").defineInRange("scaleBarrage_waves", 3, 1, 20);
        BARRAGE_PER_WAVE = b.comment("每波弹体数量").defineInRange("scaleBarrage_perWave", 18, 1, 128);

        FINAL_NOVA = new Skill(b, "finalNova", "终焉新星", 900, 3.0, 4);
        NOVA_CHARGE_TICKS = b.comment("终焉新星蓄力时间（tick）").defineInRange("finalNova_chargeTicks", 100, 20, 2400);
        NOVA_SAFE_ZONES = b.comment("安全区数量").defineInRange("finalNova_safeZones", 3, 1, 16);
        NOVA_SAFE_RADIUS = b.comment("安全区半径").defineInRange("finalNova_safeRadius", 4.0, 1.0, 32.0);

        VOID_RUPTURE = new Skill(b, "voidRupture", "虚空地脉", 280, 0.9, 9);

        WING_BLADES = new Skill(b, "wingBlades", "翼刃风暴", 300, 0.8, 9);
        WING_BLADES_PER_VOLLEY = b.comment("翼刃风暴每轮月刃数量").defineInRange("wingBlades_perVolley", 5, 1, 32);

        ENDER_STORM = new Skill(b, "enderStorm", "末影雷暴", 420, 0.9, 8);
        STORM_STRIKES = b.comment("末影雷暴落雷次数").defineInRange("enderStorm_strikes", 12, 1, 64);

        RING_WAVES = new Skill(b, "ringWaves", "裂空环波", 380, 1.0, 8);

        VOID_LANCES = new Skill(b, "voidLances", "虚空长枪", 360, 1.4, 9);
        LANCE_COUNT = b.comment("虚空长枪数量").defineInRange("voidLances_count", 5, 1, 32);

        CATACLYSM_DIVE = new Skill(b, "cataclysmDive", "陨灭俯冲", 480, 2.2, 7);
        DIVE_RADIUS = b.comment("陨灭俯冲落地冲击半径").defineInRange("cataclysmDive_radius", 9.0, 2.0, 32.0);
        b.pop();
        COMMON_SPEC = b.build();

        ForgeConfigSpec.Builder c = new ForgeConfigSpec.Builder();
        c.comment("客户端特效").push("vfx");
        OPTIMIZE_HEAVY_EFFECTS = c.comment("优化大型技能的几何细分、重叠光层及视野外冲击波和印记；龙鳞弹幕保持原样 / Optimize heavy effects; scale barrage remains unchanged").define("optimizeHeavyEffects", true);
        ENABLE_DISTORTION = c.comment("启用空间扭曲特效（需要拷贝画面，光影包下可能失效）").define("enableDistortion", true);
        ENABLE_SCREEN_EFFECTS = c.comment("启用全屏色差、白闪与暗角").define("enableScreenEffects", true);
        CAMERA_SHAKE = c.comment("镜头震动强度倍率").defineInRange("cameraShake", 1.0, 0.0, 5.0);
        c.pop();
        CLIENT_SPEC = c.build();
    }

    private EdpConfig() {
    }

    public static final class Skill {
        public final ForgeConfigSpec.BooleanValue enabled;
        public final ForgeConfigSpec.IntValue cooldown;
        public final ForgeConfigSpec.DoubleValue damage;
        public final ForgeConfigSpec.IntValue weight;

        Skill(ForgeConfigSpec.Builder b, String key, String name, int cooldown, double damage, int weight) {
            this.enabled = b.comment("启用" + name).define(key + "_enabled", true);
            this.cooldown = b.comment(name + "冷却（tick）").defineInRange(key + "_cooldown", cooldown, 0, 100000);
            this.damage = b.comment(name + "伤害倍率").defineInRange(key + "_damageMultiplier", damage, 0.0, 1000.0);
            this.weight = b.comment(name + "选择权重").defineInRange(key + "_weight", weight, 0, 1000);
        }
    }
}
