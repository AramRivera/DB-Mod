package com.dragonblock.common.event;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.npc.Dialogues;
import com.dragonblock.common.npc.NpcDialogues;
import com.dragonblock.common.world.Worlds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Death and the Other World, the heavy planet's gravity, landmarks on startup, and quest kill counts. */
@Mod.EventBusSubscriber(modid = DragonBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WorldEvents {
    private static final ResourceLocation HEAVY = ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "heavy_planet");

    private WorldEvents() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        Worlds.onServerStarted(event.getServer());
    }

    /** Dying anywhere but the afterlife sends your soul to the judge (remembering where your body fell). */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (inAfterlife(player)) return;
            PlayerDBCData data = PlayerDBCData.get(player);
            data.die(player.level().dimension().location().toString(), player.blockPosition(), player.level().getGameTime());
            data.saveToPlayer(player);
            return;
        }
        // The master's trial counts hostile creatures you defeat
        if (event.getEntity() instanceof Enemy && event.getSource().getEntity() instanceof ServerPlayer killer) {
            PlayerDBCData data = PlayerDBCData.get(killer);
            if (!NpcDialogues.QUEST_TRIAL.equals(data.getQuest()) || data.getQuestProgress() >= NpcDialogues.TRIAL_KILLS) return;
            data.addQuestProgress(1);
            data.saveToPlayer(killer);
            int done = data.getQuestProgress();
            killer.displayClientMessage(Component.literal("Prueba del Maestro Kaido: " + done + " / " + NpcDialogues.TRIAL_KILLS
                    + (done >= NpcDialogues.TRIAL_KILLS ? " · ¡vuelve con él!" : "")).withStyle(ChatFormatting.GOLD), true);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered() || !(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerDBCData data = PlayerDBCData.get(player);
        if (!data.isDead()) return;
        player.server.execute(() -> {
            Worlds.send(player, Worlds.OTHERWORLD);
            ModMessages.syncToTracking(player);
            player.displayClientMessage(Component.literal("Has muerto. Tu alma llega al Otro Mundo... habla con el Gran Juez.")
                    .withStyle(ChatFormatting.GOLD), false);
        });
    }

    /** A soul that logged out somewhere odd (a crash mid-teleport) is put back in the afterlife. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PlayerDBCData.get(player).isDead() && !inAfterlife(player)) {
            Worlds.send(player, Worlds.OTHERWORLD);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Dialogues.forget(event.getEntity().getUUID());
    }

    public static boolean inAfterlife(ServerPlayer player) {
        return player.level().dimension() == Worlds.OTHERWORLD || player.level().dimension() == Worlds.CELESTIAL;
    }

    /** True on the celestial master's planet, where training counts double. */
    public static boolean onHeavyPlanet(ServerPlayer player) {
        return player.level().dimension() == Worlds.CELESTIAL;
    }

    /** Any way into a mod world (ship, instant transmission, commands) builds its landmarks first. */
    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) Worlds.ensureBuilt(player.serverLevel());
    }

    /** The tiny planet's crushing gravity: you fall faster, jump less and move slower. */
    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 10 != 0) return;
        // Falling off the clouds or the little planet: caught and set back, not killed
        if (player.getY() < player.level().getMinBuildHeight() + 8 && (player.level().dimension() == Worlds.OTHERWORLD || onHeavyPlanet(player))) {
            Worlds.send(player, player.level().dimension());
            player.resetFallDistance();
        }
        if (Worlds.atRoadEnd(player)) {
            Worlds.send(player, Worlds.CELESTIAL);
            player.displayClientMessage(Component.literal("Al final del camino... un pequeño planeta y una gravedad aplastante.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
        boolean heavy = onHeavyPlanet(player);
        modifier(player, Attributes.GRAVITY, heavy ? 1.5 : 0);
        modifier(player, Attributes.JUMP_STRENGTH, heavy ? -0.35 : 0);
        modifier(player, Attributes.MOVEMENT_SPEED, heavy ? -0.3 : 0);
    }

    private static void modifier(ServerPlayer player, Holder<Attribute> attribute, double amount) {
        var inst = player.getAttribute(attribute);
        if (inst == null) return;
        var current = inst.getModifier(HEAVY);
        if (current != null && current.amount() == amount) return;
        inst.removeModifier(HEAVY);
        if (amount != 0) inst.addTransientModifier(new AttributeModifier(HEAVY, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
