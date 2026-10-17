package com.enderdragonplus.debug;

import com.enderdragonplus.EnderDragonPlus;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = EnderDragonPlus.MODID)
public final class ShowcaseCommand {
    private ShowcaseCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("edpshowcase")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("start")
                        .executes(context -> start(context, true))
                        .then(Commands.argument("captions", BoolArgumentType.bool())
                                .executes(context -> start(context, BoolArgumentType.getBool(context, "captions")))))
                .then(Commands.literal("scene")
                        .executes(context -> scene(context, false))
                        .then(Commands.argument("captions", BoolArgumentType.bool())
                                .executes(context -> scene(context, BoolArgumentType.getBool(context, "captions")))))
                .then(Commands.literal("stop")
                        .executes(context -> {
                            if (!ShowcaseDirector.running()) {
                                context.getSource().sendFailure(Component.literal("当前没有正在进行的展示"));
                                return 0;
                            }
                            ShowcaseDirector.stopActive("command");
                            context.getSource().sendSuccess(() -> Component.literal("展示已停止"), false);
                            return 1;
                        })));
    }

    private static int start(CommandContext<CommandSourceStack> context, boolean captions) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        EnderDragon dragon = ShowcaseDirector.findDragon(player);
        if (dragon == null) {
            context.getSource().sendFailure(Component.literal("当前维度没有找到存活的加强版末影龙"));
            return 0;
        }
        ShowcaseDirector.start(player, dragon, captions);
        context.getSource().sendSuccess(() -> Component.literal("展示开始，字幕：" + captions + "。请不要操作鼠标键盘，/edpshowcase stop 可中止"), false);
        return 1;
    }

    private static int scene(CommandContext<CommandSourceStack> context, boolean captions) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (!ShowcaseDirector.startScene(player, captions)) {
            context.getSource().sendFailure(Component.literal("末影龙召唤失败"));
            return 0;
        }
        context.getSource().sendSuccess(() -> Component.literal("场地已清空，末影龙已在上空就位，展示开始（字幕：" + captions + "）。镜头不会被锁定，请开始录制；结束后会提示清理完成"), false);
        return 1;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ShowcaseDirector.tickActive();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ShowcaseDirector.stopActive("server stopping");
    }
}
