package com.dragonblock.common.item;

import com.dragonblock.common.data.PlayerDBCData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ScouterItem extends Item {
    public ScouterItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // Raytrace entity up to 32 blocks
            Vec3 eyePos = player.getEyePosition();
            Vec3 viewVector = player.getViewVector(1.0F);
            Vec3 reachEnd = eyePos.add(viewVector.scale(32.0D));
            AABB searchBox = player.getBoundingBox().expandTowards(viewVector.scale(32.0D)).inflate(2.0D);

            EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                    player, eyePos, reachEnd, searchBox,
                    entity -> entity instanceof LivingEntity && entity.isAlive() && entity != player,
                    32.0D * 32.0D
            );

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.8f);

            if (hit != null && hit.getEntity() instanceof LivingEntity target) {
                long battlePower;
                String targetName = target.getName().getString();

                if (target instanceof Player targetPlayer) {
                    PlayerDBCData targetData = PlayerDBCData.get(targetPlayer);
                    battlePower = targetData.getBattlePower();
                    targetName = targetPlayer.getGameProfile().getName() + " [" + targetData.getRace().getDisplayName() + "]";
                } else {
                    double maxHp = target.getMaxHealth();
                    double attack = 2.0;
                    if (target.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE)) {
                        attack = target.getAttributeValue(Attributes.ATTACK_DAMAGE);
                    }
                    battlePower = (long) (maxHp * 8.0 + attack * 15.0);
                }

                ChatFormatting powerColor = battlePower > 5000 ? ChatFormatting.RED :
                        battlePower > 1000 ? ChatFormatting.GOLD : ChatFormatting.GREEN;

                player.displayClientMessage(
                        Component.literal("§7[§aSCOUTER§7] ")
                                .append(Component.literal("Objetivo: " + targetName + " | Poder de Pelea: ").withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(String.format("%,d", battlePower)).withStyle(powerColor, ChatFormatting.BOLD)),
                        false
                );
            } else {
                player.displayClientMessage(
                        Component.literal("§7[§aSCOUTER§7] §cNo se detectaron formas de vida en el rango visual."),
                        true
                );
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!player.level().isClientSide) {
            use(player.level(), player, hand);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Dispositivo de rastreo y medición de Ki del Ejército de Freezer.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Click derecho: Escanea el nivel de poder del objetivo al que apuntas.")
                .withStyle(ChatFormatting.GREEN));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
