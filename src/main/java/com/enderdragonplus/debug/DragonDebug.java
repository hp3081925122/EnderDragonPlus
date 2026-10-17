package com.enderdragonplus.debug;

import com.enderdragonplus.EnderDragonPlus;
import com.enderdragonplus.dragon.DragonBrain;
import com.enderdragonplus.dragon.DragonBrainHolder;
import com.enderdragonplus.dragon.DragonPhases;
import com.enderdragonplus.dragon.SkillType;
import com.enderdragonplus.dragon.phase.SkillPhase;
import com.enderdragonplus.network.EdpNetwork;
import com.enderdragonplus.network.VfxPayload;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(modid = EnderDragonPlus.MODID)
public final class DragonDebug {
    private static Session session;

    private static final class Session {
        final MinecraftServer server;
        final ServerPlayer owner;
        final EnderDragon dragon;
        final CompoundTag original = new CompoundTag();
        final EnderDragonPhase<?> phase;
        final boolean noAi;
        final String run;
        final Path output;
        final List<String> ticks = new ArrayList<>();
        final List<String> events = new ArrayList<>();
        final long[] timings = new long[4];
        String label = "settle";
        int index = -2;
        int age;
        int ended = -1;
        long tickStart;

        Session(MinecraftServer server, ServerPlayer owner, EnderDragon dragon, Path output, String run) {
            this.server = server;
            this.owner = owner;
            this.dragon = dragon;
            this.output = output;
            this.run = run;
            this.phase = dragon.getPhaseManager().getCurrentPhase().getPhase();
            this.noAi = dragon.isNoAi();
            ((DragonBrainHolder) dragon).edp$getBrain().saveDebugState(this.original);
            this.ticks.add("epoch_ms,skill,age,total_ns,brain_ns,phase_ns,victim_query_ns,damage_ns");
            this.events.add("run=" + run + " dragon=" + dragon.getUUID() + " owner=" + owner.getUUID());
        }
    }

    private DragonDebug() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("edpdebug").requires(source -> source.hasPermission(2))
                .then(Commands.literal("start").executes(context -> start(context.getSource(), null))
                        .then(Commands.argument("dragon", EntityArgument.entity()).executes(context ->
                                start(context.getSource(), EntityArgument.getEntity(context, "dragon")))))
                .then(Commands.literal("stop").executes(context -> {
                    if (session == null) {
                        context.getSource().sendFailure(Component.translatable("enderdragonplus.debug.not_running"));
                        return 0;
                    }
                    finish("stopped");
                    return 1;
                }))
                .then(Commands.literal("status").executes(context -> {
                    Session s = session;
                    context.getSource().sendSuccess(() -> s == null
                            ? Component.translatable("enderdragonplus.debug.not_running")
                            : Component.translatable("enderdragonplus.debug.status", skillName(s.label), Math.max(0, s.index + 1), 13, s.age), false);
                    return s == null ? 0 : 1;
                })));
    }

    private static int start(CommandSourceStack source, Entity requested) throws CommandSyntaxException {
        if (session != null) {
            source.sendFailure(Component.translatable("enderdragonplus.debug.already_running"));
            return 0;
        }
        ServerPlayer owner = source.getPlayerOrException();
        EnderDragon dragon = requested instanceof EnderDragon d ? d : null;
        if (requested == null) {
            double distance = 150.0 * 150.0;
            for (EnderDragon candidate : source.getLevel().getDragons()) {
                double next = candidate.distanceToSqr(owner);
                if (!candidate.isDeadOrDying() && next < distance) {
                    dragon = candidate;
                    distance = next;
                }
            }
        }
        if (dragon == null || dragon.isDeadOrDying() || dragon.level() != owner.level()
                || dragon.distanceToSqr(owner) > 150.0 * 150.0) {
            source.sendFailure(Component.translatable("enderdragonplus.debug.no_dragon"));
            return 0;
        }
        if (dragon.getPhaseManager().getCurrentPhase() instanceof SkillPhase) {
            source.sendFailure(Component.translatable("enderdragonplus.debug.busy"));
            return 0;
        }
        String run = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-" + UUID.randomUUID().toString().substring(0, 8);
        try {
            session = new Session(source.getServer(), owner, dragon, DebugOutput.directory(run), run);
            CompoundTag stage = new CompoundTag();
            stage.putBoolean("EdpEnhanced", true);
            stage.putBoolean("EdpStageTwo", true);
            stage.putBoolean("EdpStageThree", true);
            DragonBrain brain = ((DragonBrainHolder) dragon).edp$getBrain();
            brain.load(stage);
            dragon.setNoAi(false);
            dragon.getPhaseManager().setPhase(EnderDragonPhase.HOVERING);
            EdpNetwork.sendToPlayersTrackingEntity(dragon, brain.stagePayload());
            EdpNetwork.sendToPlayer(owner, brain.stagePayload());
            control(session, "begin");
            source.sendSuccess(() -> Component.translatable("enderdragonplus.debug.started", run), false);
            EnderDragonPlus.LOGGER.info("EDP debug started run={} dragon={} owner={}", run, dragon.getUUID(), owner.getUUID());
            return 1;
        } catch (Exception exception) {
            EnderDragonPlus.LOGGER.error("EDP debug could not start", exception);
            if (session != null) {
                finish("start_failed");
            }
            source.sendFailure(Component.translatable("enderdragonplus.debug.failed"));
            return 0;
        }
    }

    public static boolean controls(EnderDragon dragon) {
        return session != null && session.dragon == dragon;
    }

    public static ServerPlayer target(EnderDragon dragon) {
        return controls(dragon) ? session.owner : null;
    }

    public static boolean isTarget(EnderDragon dragon, LivingEntity entity) {
        return controls(dragon) && session.owner == entity;
    }

    public static long begin(EnderDragon dragon) {
        return controls(dragon) ? System.nanoTime() : 0L;
    }

    public static void end(int metric, long started) {
        if (started != 0L && session != null) {
            session.timings[metric] += System.nanoTime() - started;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tickStart(TickEvent.ServerTickEvent event) {
        Session s = session;
        if (event.phase == TickEvent.Phase.START && s != null && s.server == event.getServer()) {
            java.util.Arrays.fill(s.timings, 0L);
            s.tickStart = System.nanoTime();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tickEnd(TickEvent.ServerTickEvent event) {
        Session s = session;
        if (event.phase != TickEvent.Phase.END || s == null || s.server != event.getServer()) {
            return;
        }
        long elapsed = s.tickStart == 0 ? 0 : System.nanoTime() - s.tickStart;
        s.ticks.add(System.currentTimeMillis() + "," + s.label + "," + s.age + "," + elapsed + ","
                + s.timings[0] + "," + s.timings[1] + "," + s.timings[2] + "," + s.timings[3]);
        if (s.dragon.isRemoved() || s.dragon.isDeadOrDying() || s.owner.isRemoved() || !s.owner.isAlive()
                || s.owner.level() != s.dragon.level() || s.server.getPlayerList().getPlayer(s.owner.getUUID()) != s.owner) {
            finish("entity_or_player_unavailable");
            return;
        }
        if (s.owner.distanceToSqr(s.dragon) > 300.0 * 300.0) {
            finish("player_out_of_range");
            return;
        }
        s.age++;
        if (s.index == -2 && s.age >= 160) {
            s.index = -1;
            s.age = 0;
            s.label = "baseline";
            control(s, "phase");
        } else if (s.index == -1 && s.age >= 100) {
            advance(s);
        } else if (s.index >= 0) {
            if (s.ended < 0 && !(s.dragon.getPhaseManager().getCurrentPhase() instanceof SkillPhase)) {
                s.ended = s.age;
                s.events.add("END " + s.label + " age=" + s.age);
            }
            if (s.age > 12000) {
                finish("skill_timeout");
            } else if (s.ended >= 0 && s.age >= s.ended + 160) {
                advance(s);
            }
        }
    }

    private static void advance(Session s) {
        s.index++;
        if (s.index > SkillType.values().length) {
            finish("completed");
            return;
        }
        s.age = 0;
        s.ended = -1;
        s.label = s.index == SkillType.values().length ? "SHIELD" : SkillType.values()[s.index].name();
        control(s, "phase");
        DragonBrain brain = ((DragonBrainHolder) s.dragon).edp$getBrain();
        if (s.index == SkillType.values().length) {
            s.dragon.getPhaseManager().setPhase(DragonPhases.SHIELD_ASCENSION);
        } else {
            brain.startSkill(SkillType.values()[s.index], s.owner);
        }
        s.events.add("START " + s.label + " targets=" + brain.combatPlayers().size());
        s.owner.sendSystemMessage(Component.translatable("enderdragonplus.debug.phase", s.index + 1, 13, skillName(s.label)));
        EnderDragonPlus.LOGGER.info("EDP debug phase run={} skill={} targets={}", s.run, s.label, brain.combatPlayers().size());
    }

    private static Component skillName(String skill) {
        return Component.translatable("enderdragonplus.skill." + skill.toLowerCase(java.util.Locale.ROOT));
    }

    private static void control(Session s, String action) {
        CompoundTag data = new CompoundTag();
        data.putString("action", action);
        data.putString("run", s.run);
        data.putString("skill", s.label);
        EdpNetwork.sendToPlayer(s.owner, new VfxPayload(VfxPayload.DEBUG_PROFILE, s.dragon.getId(), data));
    }

    private static void finish(String reason) {
        Session s = session;
        if (s == null) {
            return;
        }
        session = null;
        try {
            s.events.add("FINISH reason=" + reason);
            Files.write(s.output.resolve("server-ticks.csv"), s.ticks, StandardCharsets.UTF_8);
            Files.write(s.output.resolve("server-events.txt"), s.events, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            EnderDragonPlus.LOGGER.error("EDP debug could not write server results", exception);
        } finally {
            if (!s.dragon.isRemoved()) {
                DragonBrain brain = ((DragonBrainHolder) s.dragon).edp$getBrain();
                brain.loadDebugState(s.original);
                s.dragon.setNoAi(s.noAi);
                if (!s.dragon.isDeadOrDying()) {
                    s.dragon.getPhaseManager().setPhase(s.phase);
                }
                EdpNetwork.sendToPlayersTrackingEntity(s.dragon, brain.stagePayload());
            }
            s.label = reason;
            if (s.server.getPlayerList().getPlayer(s.owner.getUUID()) == s.owner) {
                control(s, "end");
                s.owner.sendSystemMessage(Component.translatable("enderdragonplus.debug.finished", Component.translatable("enderdragonplus.debug.reason." + reason), s.output.toString()));
            }
            EnderDragonPlus.LOGGER.info("EDP debug finished run={} reason={} output={}", s.run, reason, s.output);
        }
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent event) {
        if (session != null && session.server == event.getServer()) {
            finish("server_stopping");
        }
    }
}
