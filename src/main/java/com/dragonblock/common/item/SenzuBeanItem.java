package com.dragonblock.common.item;

import com.dragonblock.common.data.PlayerDBCData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class SenzuBeanItem extends Item {
    public SenzuBeanItem() {
        super(new Item.Properties()
                .stacksTo(16)
                .food(new FoodProperties.Builder()
                        .nutrition(20)
                        .saturationModifier(1.0f)
                        .alwaysEdible()
                        .build()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof Player player) {
            // Restore full health
            player.setHealth(player.getMaxHealth());
            player.removeAllEffects();

            // Restore full DBC Ki
            PlayerDBCData data = PlayerDBCData.get(player);
            data.setCurrentKi(data.getMaxKi());
            data.saveToPlayer(player);

            player.displayClientMessage(
                    Component.literal("¡Has consumido una Semilla del Ermitaño! Tu salud y Ki están al 100%!")
                            .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                    true
            );
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Cultivada por el Maestro Karin en la Torre Sagrada.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Restaura instantáneamente toda la salud, hambre y Ki.")
                .withStyle(ChatFormatting.YELLOW));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
