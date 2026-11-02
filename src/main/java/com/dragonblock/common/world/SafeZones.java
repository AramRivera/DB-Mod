package com.dragonblock.common.world;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.entity.NpcEntity;
import com.dragonblock.common.npc.Dialogues;
import com.dragonblock.common.npc.NpcKind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Places where players can't fight: the whole Other World (souls rest there) and a ring around every
 * character (NpcKind.getSafeRadius), so towns and masters' homes are calm for a crowded server. Inside,
 * players don't hurt each other, techniques and explosions don't touch them nor the buildings, and only
 * operators can break or place blocks. Anyone reading a character's line is shielded from everything.
 * The zones depend only on the world and the characters loaded around you, so the client can show them
 * without any extra network traffic.
 */
@Mod.EventBusSubscriber(modid = DragonBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SafeZones {
    /** Largest NpcKind safe or guard radius: how far to look for characters around a point. */
    private static final int REACH = 48;

    private SafeZones() {}

    /** No fighting between players here. */
    public static boolean isSafe(Level level, Vec3 pos) {
        return within(level, pos, NpcKind::getSafeRadius);
    }

    /** No breaking or placing blocks here (a little wider: the tournament arena too). */
    public static boolean isGuarded(Level level, Vec3 pos) {
        return within(level, pos, NpcKind::getGuardRadius);
    }

    private static boolean within(Level level, Vec3 pos, ToIntFunction<NpcKind> radius) {
        if (level.dimension() == Worlds.OTHERWORLD) return true;
        return !level.getEntitiesOfClass(NpcEntity.class, new AABB(pos, pos).inflate(REACH), npc -> {
            int r = radius.applyAsInt(npc.getKind());
            return r > 0 && npc.distanceToSqr(pos) < r * r;
        }).isEmpty();
    }

    /** True when {@code attacker} may not harm {@code target} because of a zone or a conversation. */
    public static boolean protects(Entity attacker, Entity target) {
        if (target instanceof ServerPlayer p && Dialogues.isTalking(p)) return true;
        if (attacker instanceof ServerPlayer p && Dialogues.isTalking(p)) return true;
        if (!(attacker instanceof Player) || !(target instanceof Player) || attacker == target) return false;
        return isSafe(target.level(), target.position()) || isSafe(attacker.level(), attacker.position());
    }

    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && !event.getEntity().level().isClientSide && protects(attacker, event.getEntity())) event.setCanceled(true);
    }

    /** Ki blasts and other blasts spare the buildings of a zone and the players standing in it. */
    @SubscribeEvent
    public static void onDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.dimension() == Worlds.OTHERWORLD) {
            event.getAffectedBlocks().clear();
            event.getAffectedEntities().removeIf(e -> e instanceof Player);
            return;
        }
        Vec3 c = event.getExplosion().center();
        // One lookup per blast, then plain distance checks for its (possibly thousands of) blocks
        List<NpcEntity> zones = level.getEntitiesOfClass(NpcEntity.class, new AABB(c, c).inflate(REACH + event.getExplosion().radius() * 2),
                npc -> npc.getKind().getGuardRadius() > 0);
        if (zones.isEmpty()) return;
        event.getAffectedBlocks().removeIf(pos -> inAny(zones, Vec3.atCenterOf(pos), NpcKind::getGuardRadius));
        event.getAffectedEntities().removeIf(e -> e instanceof Player && inAny(zones, e.position(), NpcKind::getSafeRadius));
    }

    private static boolean inAny(List<NpcEntity> zones, Vec3 pos, ToIntFunction<NpcKind> radius) {
        for (NpcEntity npc : zones) {
            int r = radius.applyAsInt(npc.getKind());
            if (r > 0 && npc.distanceToSqr(pos) < r * r) return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!mayBuild(event.getPlayer(), event.getPos())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && !mayBuild(player, event.getPos())) event.setCanceled(true);
    }

    private static boolean mayBuild(Player player, BlockPos pos) {
        return player.level().isClientSide || player.hasPermissions(2) || !isGuarded(player.level(), Vec3.atCenterOf(pos));
    }
}
