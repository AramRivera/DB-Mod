package com.dragonblock.client;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.client.gui.DBCCharacterScreen;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Race;
import com.dragonblock.common.entity.ShenlongEntity;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.registry.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hands-off check of the dragon ritual for development, inert unless the JVM runs with
 * -Ddragonblock.autotest=dragon: it opens (or creates) the world "autotest", calls the dragon in front
 * of the player at night, saves screenshots of every phase to run/screenshots, then quits.
 */
@Mod.EventBusSubscriber(modid = DragonBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DevAutotest {
    private static final String MODE = System.getProperty("dragonblock.autotest", "");
    private static final boolean ON = !MODE.isEmpty();
    private static final int[] SHOTS = {100, 160, 230, 300, 360, 395, 410, 430};
    private static boolean opened = false;
    private static int ticks = 0;

    private DevAutotest() {}

    @SubscribeEvent
    public static void onScreen(ScreenEvent.Init.Post event) {
        if (!ON || opened || !(event.getScreen() instanceof TitleScreen)) return;
        opened = true;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getLevelSource().levelExists("autotest")) {
            mc.createWorldOpenFlows().openWorld("autotest", () -> {});
        } else {
            LevelSettings settings = new LevelSettings("autotest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("autotest", settings, WorldOptions.defaultWithRandomSeed(),
                    WorldPresets::createNormalWorldDimensions, event.getScreen());
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!ON || event.phase != TickEvent.Phase.END || mc.player == null || mc.getSingleplayerServer() == null) return;
        ticks++;
        if (MODE.equals("world")) {
            worldTour(mc);
            return;
        }
        if (mc.screen instanceof DBCCharacterScreen || mc.screen instanceof com.dragonblock.client.gui.WishScreen) mc.setScreen(null);
        if (ticks == 380) {
            // Ask for riches to watch the farewell
            for (var d : mc.level.getEntitiesOfClass(ShenlongEntity.class, mc.player.getBoundingBox().inflate(128))) {
                ModMessages.sendToServer(new com.dragonblock.common.network.packet.C2SWishPacket(d.getId(), 1));
            }
        }
        boolean close = ticks >= 330 && ticks < 370;
        boolean balls = ticks >= 66 && ticks < 95;
        mc.player.setYRot(0f);
        mc.player.setXRot(balls ? 40f : close ? 8f : -30f);
        if (ticks == 66 || ticks == 95) {
            // A close look at the seven balls on the ground, then out to the wide shot
            var uuid = mc.player.getUUID();
            boolean near = ticks == 66;
            mc.getSingleplayerServer().execute(() -> {
                ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);
                for (var d : sp.serverLevel().getEntitiesOfClass(ShenlongEntity.class, sp.getBoundingBox().inflate(160))) {
                    sp.teleportTo(sp.serverLevel(), d.getX(), d.getY() + (near ? 3.5 : 14), d.getZ() - (near ? 6 : 46), 0f, near ? 40f : -30f);
                }
            });
        }
        if (ticks == 90) Screenshot.grab(mc.gameDirectory, "autotest_balls.png", mc.getMainRenderTarget(), c -> {});
        if (ticks == 330 || ticks == 370) {
            // Fly up to look the dragon in the eye, then back to the wide shot
            var uuid = mc.player.getUUID();
            boolean up = ticks == 330;
            mc.getSingleplayerServer().execute(() -> {
                ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);
                for (var d : sp.serverLevel().getEntitiesOfClass(ShenlongEntity.class, sp.getBoundingBox().inflate(160))) {
                    sp.teleportTo(sp.serverLevel(), d.getX(), d.getY() + (up ? 52 : 14), d.getZ() - (up ? 26 : 46), 0f, up ? 8f : -30f);
                }
            });
        }
        if (ticks == 360) Screenshot.grab(mc.gameDirectory, "autotest_head.png", mc.getMainRenderTarget(), c -> {});

        if (ticks == 60) {
            var uuid = mc.player.getUUID();
            mc.getSingleplayerServer().execute(() -> {
                ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayer(uuid);
                if (sp == null) return;
                PlayerDBCData data = PlayerDBCData.get(sp);
                data.setRace(Race.SAIYAN);
                data.setRaceChosen(true);
                data.setTailOut(false); // no surprise Ozaru under the night sky
                data.saveToPlayer(sp);
                ModMessages.syncToTracking(sp);
                var level = sp.serverLevel();
                level.setDayTime(18000);
                BlockPos c = sp.blockPosition().offset(0, 0, 18);
                c = new BlockPos(c.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.getX(), c.getZ()), c.getZ());
                for (int i = 0; i < 7; i++) {
                    double a = Math.PI * 2 * i / 7;
                    level.setBlockAndUpdate(c.offset((int) Math.round(Math.cos(a) * 3), 0, (int) Math.round(Math.sin(a) * 3)),
                            ModBlocks.DRAGON_BALLS.get(i).get().defaultBlockState());
                }
                // Watch from above the canopy, 20 blocks away, facing the ring
                sp.getAbilities().flying = true;
                sp.onUpdateAbilities();
                sp.teleportTo(level, c.getX() + 0.5, c.getY() + 14, c.getZ() - 46 + 0.5, 0f, -30f);
                ShenlongEntity.trySummon(level, c, sp);
                DragonBlockMod.LOGGER.info("[autotest] summoned at {}", c);
            });
        }
        if (ticks > 60 && ticks % 20 == 0) {
            for (var d : mc.level.getEntitiesOfClass(ShenlongEntity.class, mc.player.getBoundingBox().inflate(128))) {
                DragonBlockMod.LOGGER.info("[autotest] dragon state={} age={} at {}", d.getState(), d.stateAge(), d.position());
            }
        }
        for (int shot : SHOTS) {
            if (ticks == shot) Screenshot.grab(mc.gameDirectory, "autotest_" + shot + ".png", mc.getMainRenderTarget(), c -> {});
        }
        if (ticks == 440) mc.setScreen(new InventoryScreen(mc.player));
        if (ticks == 450) Screenshot.grab(mc.gameDirectory, "autotest_inventory.png", mc.getMainRenderTarget(), c -> {});
        if (ticks == 460) mc.stop();
    }

    /** Talk to the master, die, meet the judge, visit the heavy planet, come back to life: a shot at every step. */
    private static void worldTour(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        var uuid = mc.player.getUUID();
        if (mc.screen instanceof DBCCharacterScreen) mc.setScreen(null);
        if (mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen) {
            mc.player.respawn();
            mc.setScreen(null);
        }
        java.util.function.Consumer<java.util.function.Consumer<ServerPlayer>> onServer = task -> server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(uuid);
            if (sp != null) task.accept(sp);
        });
        java.util.function.BiConsumer<ServerPlayer, com.dragonblock.common.npc.NpcKind> faceNpc = (sp, kind) -> {
            for (var npc : sp.serverLevel().getEntitiesOfClass(com.dragonblock.common.entity.NpcEntity.class, sp.getBoundingBox().inflate(200))) {
                if (npc.getKind() != kind) continue;
                double dist = 3.0 * kind.getScale() + 1;
                var look = npc.getLookAngle().multiply(1, 0, 1).normalize();
                sp.teleportTo(sp.serverLevel(), npc.getX() + look.x * dist, npc.getY(), npc.getZ() + look.z * dist,
                        (float) Math.toDegrees(Math.atan2(look.x, -look.z)), kind == com.dragonblock.common.npc.NpcKind.JUDGE ? -18f : 5f);
                com.dragonblock.common.npc.Dialogues.open(sp, npc);
                return;
            }
            DragonBlockMod.LOGGER.warn("[autotest] no {} found", kind);
        };
        switch (ticks) {
            case 60 -> onServer.accept(sp -> {
                PlayerDBCData data = PlayerDBCData.get(sp);
                data.setRace(Race.SAIYAN);
                data.setRaceChosen(true);
                data.setTailOut(false);
                data.saveToPlayer(sp);
                ModMessages.syncToTracking(sp);
                sp.setGameMode(GameType.SURVIVAL);
                sp.serverLevel().setDayTime(1000);
                faceNpc.accept(sp, com.dragonblock.common.npc.NpcKind.MASTER);
            });
            case 110 -> shot(mc, "world_master");
            case 120 -> onServer.accept(sp -> faceNpc.accept(sp, com.dragonblock.common.npc.NpcKind.SCIENTIST));
            case 170 -> shot(mc, "world_scientist");
            case 180 -> {
                mc.setScreen(null);
                onServer.accept(sp -> sp.hurt(sp.damageSources().fellOutOfWorld(), 10000f));
            }
            case 260 -> shot(mc, "world_otherworld");
            case 270 -> onServer.accept(sp -> faceNpc.accept(sp, com.dragonblock.common.npc.NpcKind.JUDGE));
            case 320 -> shot(mc, "world_judge");
            case 330 -> {
                mc.setScreen(null);
                onServer.accept(sp -> com.dragonblock.common.world.Worlds.send(sp, com.dragonblock.common.world.Worlds.CELESTIAL));
            }
            case 400 -> shot(mc, "world_celestial");
            case 410 -> onServer.accept(sp -> faceNpc.accept(sp, com.dragonblock.common.npc.NpcKind.CELESTIAL));
            case 460 -> shot(mc, "world_celestial_npc");
            case 470 -> {
                mc.setScreen(null);
                onServer.accept(sp -> {
                    PlayerDBCData data = PlayerDBCData.get(sp);
                    DragonBlockMod.LOGGER.info("[autotest] dead={} deathDim={} pos={}", data.isDead(), data.getDeathDim(), data.getDeathPos());
                    data.revive();
                    data.saveToPlayer(sp);
                    ModMessages.syncToTracking(sp);
                    com.dragonblock.common.world.Worlds.sendTo(sp, net.minecraft.world.level.Level.OVERWORLD, data.getDeathPos());
                });
            }
            case 530 -> shot(mc, "world_back");
            case 540 -> mc.stop();
            default -> {}
        }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "autotest_" + name + ".png", mc.getMainRenderTarget(), c -> {});
        DragonBlockMod.LOGGER.info("[autotest] shot {} in {}", name, mc.level.dimension().location());
    }
}
