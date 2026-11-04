package com.dragonblock.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** A dragon ball: carried as an item, set down as a block to gather the seven and call the dragon. */
public class DragonBallItem extends BlockItem {
    private final int stars;

    public DragonBallItem(Block block, int stars) {
        super(block, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
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
        tooltip.add(Component.literal("Pon las siete en círculo, al mismo nivel y sin tocarse, y usa una.").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Una vez puesta, solo la Cápsula de esferas la recoge.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
