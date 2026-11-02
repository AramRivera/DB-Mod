package com.dragonblock.common.item;

import com.dragonblock.common.entity.SpaceshipEntity;
import com.dragonblock.common.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** A capsule with a spaceship inside: use it on the ground and the ship pops out. */
public class ShipCapsuleItem extends Item {
    public ShipCapsuleItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos at = context.getClickedPos().relative(context.getClickedFace());
        SpaceshipEntity ship = new SpaceshipEntity(ModEntities.SPACESHIP.get(), level);
        ship.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, context.getRotation() + 180f, 0f);
        if (!level.noCollision(ship)) {
            if (context.getPlayer() != null) context.getPlayer().displayClientMessage(Component.literal("No hay sitio para la nave aquí."), true);
            return InteractionResult.FAIL;
        }
        level.addFreshEntity(ship);
        level.playSound(null, at, SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 1f, 0.5f);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, ship.getX(), ship.getY() + 1, ship.getZ(), 30, 1, 1, 1, 0.05);
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Úsala en el suelo: sale una nave espacial.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Clic derecho en la nave para subir y elegir planeta.").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Agachado y golpéala (vacía) para guardarla.").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
