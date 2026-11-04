package com.dragonblock.common.item;

import com.dragonblock.common.entity.DBEnemy;
import com.dragonblock.common.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** A signal beacon taken from the empire's soldiers. Activating it calls their emperor down on you. */
public class ImperialBeaconItem extends Item {
    public ImperialBeaconItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            Vec3 at = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(10));
            DBEnemy emperor = ModEntities.EMPEROR.get().create(server);
            if (emperor == null) return InteractionResultHolder.fail(stack);
            emperor.moveTo(at.x, player.getY() + 1, at.z, player.getYRot() + 180f, 0f);
            emperor.setTarget(player);
            server.addFreshEntity(emperor);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, at.x, player.getY() + 1, at.z, 1, 0, 0, 0, 0);
            server.playSound(null, at.x, player.getY(), at.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5f, 0.8f);
            player.sendSystemMessage(Component.literal("[Kryos, Emperador Galáctico] ").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
                    .append(Component.literal("¿Tú has llamado al emperador? Qué valor... o qué estupidez.").withStyle(ChatFormatting.LIGHT_PURPLE)));
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Baliza de los soldados del Imperio.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Actívala para desafiar a su emperador. Prepárate bien.").withStyle(ChatFormatting.DARK_PURPLE));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
