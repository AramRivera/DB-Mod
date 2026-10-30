package com.dragonblock.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class DragonBallItem extends Item {
    private final int stars;

    public DragonBallItem(int stars) {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
        this.stars = stars;
    }

    public int getStars() {
        return stars;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        StringBuilder starsStr = new StringBuilder();
        for (int i = 0; i < stars; i++) {
            starsStr.append("★ ");
        }
        tooltip.add(Component.literal(starsStr.toString().trim()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Una de las 7 Esferas místicas de la Tierra.").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Reúne las 7 para invocar al dragón Shenlong.").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
