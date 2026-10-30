package com.dragonblock.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class DragonRadarItem extends Item {
    public DragonRadarItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0f, 2.0f);

            // Search for Dragon Ball items in world
            AABB searchBox = player.getBoundingBox().inflate(128.0D);
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, searchBox,
                    entity -> entity.getItem().getItem() instanceof DragonBallItem);

            if (!items.isEmpty()) {
                ItemEntity nearest = items.get(0);
                double minDist = player.distanceToSqr(nearest);
                for (ItemEntity item : items) {
                    double dist = player.distanceToSqr(item);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = item;
                    }
                }

                int distanceBlocks = (int) Math.sqrt(minDist);
                BlockPos targetPos = nearest.blockPosition();
                String direction = getDirectionString(player.blockPosition(), targetPos);

                player.displayClientMessage(
                        Component.literal("§6[RADAR DRAGÓN] §e¡Señal detectada! ")
                                .append(Component.literal(nearest.getItem().getHoverName().getString()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                                .append(Component.literal(" a §a" + distanceBlocks + " bloques §ehacia el §b" + direction + "§e.")),
                        false
                );
            } else {
                player.displayClientMessage(
                        Component.literal("§6[RADAR DRAGÓN] §7No se detectan Esferas del Dragón cercanas en un radio de 128 bloques."),
                        true
                );
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private String getDirectionString(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();

        StringBuilder dir = new StringBuilder();
        if (dz < -10) dir.append("Norte ");
        else if (dz > 10) dir.append("Sur ");

        if (dx > 10) dir.append("Este");
        else if (dx < -10) dir.append("Oeste");

        String result = dir.toString().trim();
        return result.isEmpty() ? "Aquí mismo" : result;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Invento insignia de Bulma y la Corporación Cápsula.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Click derecho: Localiza las Esferas del Dragón cercanas.")
                .withStyle(ChatFormatting.YELLOW));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
