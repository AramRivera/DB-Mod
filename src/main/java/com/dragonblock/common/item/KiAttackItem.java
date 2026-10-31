package com.dragonblock.common.item;

import com.dragonblock.common.combat.KiAttacks;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.network.ModMessages;
import net.minecraft.server.level.ServerPlayer;
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
    public static final float KI_COST = KiAttacks.BLAST_COST;

    public KiAttackItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player instanceof ServerPlayer serverPlayer) {
            PlayerDBCData data = PlayerDBCData.get(serverPlayer);
            KiAttacks.fireBlast(serverPlayer, data);
            data.saveToPlayer(serverPlayer);
            ModMessages.syncToPlayer(serverPlayer);
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
