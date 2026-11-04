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

public class ScouterItem extends AccessoryItem {
    public ScouterItem() {
        super(Slot.VISOR, "Puesto: mide el poder de lo que miras, sigue el ki más fuerte cerca y estalla si lee un poder abrumador.");
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
    }

    public static final String ID = "dragonblock:scouter";

    /** What a scouter reads: a fighter's battle power, or an estimate from a creature's toughness. */
    public static long readPower(LivingEntity target) {
        if (target instanceof Player p) return PlayerDBCData.get(p).getBattlePower();
        return (long) (target.getMaxHealth() * 10 + target.getArmorValue() * 20);
    }

    /** The living thing the wearer is looking at, up to {@code range} blocks, or null. */
    public static LivingEntity lookedAt(Player player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, eye.add(look.scale(range)),
                player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.5), e -> e instanceof LivingEntity && e.isAlive() && e != player,
                range * range);
        return hit != null && hit.getEntity() instanceof LivingEntity l && player.hasLineOfSight(l) ? l : null;
    }

    /**
     * Server, twice a second while worn: reading a power far beyond the wearer's overloads the scouter,
     * which bursts and is gone.
     */
    public static void tickWorn(net.minecraft.server.level.ServerPlayer player, PlayerDBCData data) {
        if (!data.wears(ID)) return;
        LivingEntity target = lookedAt(player, 64);
        if (target == null) return;
        long power = readPower(target);
        if (power < Math.max(20000L, data.getBattlePower() * 25L)) return;
        for (int i = 0; i < 4; i++) if (ID.equals(data.getAccessory(i))) data.setAccessory(i, "");
        data.saveToPlayer(player);
        com.dragonblock.common.network.ModMessages.syncToTracking(player);
        com.dragonblock.common.network.ModMessages.broadcastAnim(player, com.dragonblock.common.network.packet.S2CAnimPacket.Anim.SCOUTER_BREAK, 0);
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 1.4f);
        player.level().playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.4f, 2.0f);
        player.displayClientMessage(Component.literal("¡El scouter no soportó semejante poder: " + String.format("%,d", power) + "!")
                .withStyle(ChatFormatting.RED), true);
    }
}
