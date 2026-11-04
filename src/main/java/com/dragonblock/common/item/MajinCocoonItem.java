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

/** A sealed pink cocoon. Breaking the seal wakes the glutton demon sleeping inside, right in front of you. */
public class MajinCocoonItem extends Item {
    public MajinCocoonItem() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            Vec3 at = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(10));
            DBEnemy majin = ModEntities.MAJIN.get().create(server);
            if (majin == null) return InteractionResultHolder.fail(stack);
            majin.moveTo(at.x, player.getY() + 1, at.z, player.getYRot() + 180f, 0f);
            majin.setTarget(player);
            server.addFreshEntity(majin);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, at.x, player.getY() + 1, at.z, 60, 1.2, 1.2, 1.2, 0.15);
            server.playSound(null, at.x, player.getY(), at.z, SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 3f, 0.4f);
            player.sendSystemMessage(Component.literal("[Majin Glotón] ").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD)
                    .append(Component.literal("¡Majin despierto! ¡Majin tiene HAMBRE! ¿Tú eres caramelo?").withStyle(ChatFormatting.LIGHT_PURPLE)));
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Un capullo rosa con un sello mágico.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Rompe el sello para despertar al demonio que duerme dentro.").withStyle(ChatFormatting.DARK_PURPLE));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
