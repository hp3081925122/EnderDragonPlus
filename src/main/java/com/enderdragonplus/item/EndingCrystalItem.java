package com.enderdragonplus.item;

import com.enderdragonplus.EdpConfig;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public class EndingCrystalItem extends Item {
    public static final String TAG = "EdpEndingCrystal";

    public EndingCrystalItem(Item.Properties properties) {
        super(properties);
    }

    public static boolean isEndingCrystal(Entity entity) {
        return entity.getPersistentData().getBooleanOr(TAG, false);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK)) {
            return InteractionResult.FAIL;
        }
        BlockPos above = pos.above();
        if (!level.isEmptyBlock(above)) {
            return InteractionResult.FAIL;
        }
        double x = above.getX();
        double y = above.getY();
        double z = above.getZ();
        List<Entity> blocking = level.getEntities(null, new AABB(x, y, z, x + 1.0, y + 2.0, z + 1.0));
        if (!blocking.isEmpty()) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel serverLevel) {
            EndCrystal crystal = new EndCrystal(level, x + 0.5, y, z + 0.5);
            crystal.setShowBottom(false);
            crystal.getPersistentData().putBoolean(TAG, true);
            level.addFreshEntity(crystal);
            level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, above);
            EnderDragonFight fight = serverLevel.getDragonFight();
            if (fight != null) {
                fight.tryRespawn();
            }
        }
        context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.enderdragonplus.ending_crystal.desc").withStyle(ChatFormatting.LIGHT_PURPLE));
        if (!EdpConfig.REQUIRE_SPECIAL_SUMMON.get()) {
            tooltip.accept(Component.translatable("item.enderdragonplus.ending_crystal.always").withStyle(ChatFormatting.GRAY));
        }
    }
}
