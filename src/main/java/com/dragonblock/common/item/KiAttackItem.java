package com.dragonblock.common.item;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.entity.KiBlastEntity;
import com.dragonblock.common.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class KiAttackItem extends Item {
    public static final float KI_COST = 15.0f;

    public KiAttackItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            PlayerDBCData data = PlayerDBCData.get(player);
            if (data.getCurrentKi() >= KI_COST) {
                data.setCurrentKi(data.getCurrentKi() - KI_COST);
                data.saveToPlayer(player);

                // Calculate damage based on Willpower and current form
                float damage = 8.0f + (data.getEffectiveWil() * 0.75f);
                float explosionPower = 1.2f + Math.min(2.5f, data.getEffectiveWil() * 0.05f);

                KiBlastEntity blast = new KiBlastEntity(level, player, damage, explosionPower);
                blast.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.8F, 0.5F);
                level.addFreshEntity(blast);

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.KI_BLAST.get(), SoundSource.PLAYERS, 1.2f, 1.0f);

                player.getCooldowns().addCooldown(this, 10);
            } else {
                player.displayClientMessage(
                        Component.literal("§c¡No tienes suficiente Ki para disparar una ráfaga! (Necesitas 15 Ki)"),
                        true
                );
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Técnica básica de manipulación de energía Ki.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Click derecho: Dispara una ráfaga de Ki concentrada (Costo: 15 Ki).")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("El daño aumenta según tu atributo de Voluntad (WIL) y tu transformación.")
                .withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
