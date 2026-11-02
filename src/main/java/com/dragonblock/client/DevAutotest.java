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
        // Keep running when the window loses focus, or the integrated server pauses mid-test
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (MODE.equals("world")) {
            worldTour(mc);
            return;
        }
        if (MODE.equals("ship")) {
            shipTrip(mc);
            return;
        }
        if (MODE.equals("enemies")) {
            enemyLineup(mc);
            return;
        }
        if (MODE.equals("bestiary")) {
            if (ticks <= 60) enemyLineup(mc); // same sky arena
            else bestiary(mc);
            return;
        }
        if (MODE.equals("expand")) {
            expansionTour(mc);
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

    /** Park a ship, board it, fly to Namek and land: liftoff, hyperspace, arrival and the planet itself. */
    private static void shipTrip(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        var uuid = mc.player.getUUID();
        if (mc.screen instanceof DBCCharacterScreen || mc.screen instanceof com.dragonblock.client.gui.TravelScreen) mc.setScreen(null);
        java.util.function.Consumer<java.util.function.Consumer<ServerPlayer>> onServer = task -> server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(uuid);
            if (sp != null) task.accept(sp);
        });
        switch (ticks) {
            case 60 -> onServer.accept(sp -> {
                PlayerDBCData data = PlayerDBCData.get(sp);
                data.setRace(Race.SAIYAN);
                data.setRaceChosen(true);
                data.setTailOut(false);
                data.saveToPlayer(sp);
                ModMessages.syncToTracking(sp);
                sp.serverLevel().setDayTime(1000);
                var ship = new com.dragonblock.common.entity.SpaceshipEntity(com.dragonblock.common.registry.ModEntities.SPACESHIP.get(), sp.serverLevel());
                var at = sp.position().add(sp.getLookAngle().multiply(1, 0, 1).normalize().scale(7));
                int y = sp.serverLevel().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(at.x), (int) Math.floor(at.z));
                ship.moveTo(at.x, y, at.z, sp.getYRot(), 0f);
                sp.serverLevel().addFreshEntity(ship);
                sp.setXRot(10f);
            });
            case 80 -> shot(mc, "ship_parked");
            case 90 -> {
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                onServer.accept(sp -> {
                    for (var ship : sp.serverLevel().getEntitiesOfClass(com.dragonblock.common.entity.SpaceshipEntity.class, sp.getBoundingBox().inflate(20))) {
                        sp.startRiding(ship, true);
                        var here = com.dragonblock.common.world.Planet.of(sp.level().dimension());
                        ship.launch(sp, here == com.dragonblock.common.world.Planet.NAMEK ? com.dragonblock.common.world.Planet.EARTH
                                : com.dragonblock.common.world.Planet.NAMEK);
                    }
                });
            }
            case 125 -> shot(mc, "ship_liftoff");
            case 137 -> shot(mc, "ship_warp");
            case 160 -> shot(mc, "ship_arrive");
            case 235 -> shot(mc, "ship_landed");
            case 240 -> {
                onServer.accept(ServerPlayer::stopRiding);
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            }
            case 260 -> {
                mc.player.setXRot(-5f);
                shot(mc, "namek_view");
            }
            case 300 -> {
                DragonBlockMod.LOGGER.info("[autotest] after landing: health={} dim={}", mc.player.getHealth(), mc.level.dimension().location());
                shot(mc, "ship_after");
            }
            case 310 -> mc.stop();
            default -> {}
        }
    }

    /** The three fighters attacking a (protected) player, then the emperor's transformation at half health. */
    private static void enemyLineup(Minecraft mc) {
        if (ticks > 62) {
            mc.player.setYRot(0f);
            mc.player.setXRot(8f);
        }
        var server = mc.getSingleplayerServer();
        var uuid = mc.player.getUUID();
        if (mc.screen instanceof DBCCharacterScreen) mc.setScreen(null);
        java.util.function.Consumer<java.util.function.Consumer<ServerPlayer>> onServer = task -> server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(uuid);
            if (sp != null) task.accept(sp);
        });
        java.util.function.BiConsumer<ServerPlayer, java.util.List<net.minecraft.world.entity.EntityType<com.dragonblock.common.entity.DBEnemy>>> spawn = (sp, types) -> {
            var look = sp.getLookAngle().multiply(1, 0, 1).normalize();
            var side = new net.minecraft.world.phys.Vec3(-look.z, 0, look.x);
            for (int i = 0; i < types.size(); i++) {
                var at = sp.position().add(look.scale(9)).add(side.scale((i - (types.size() - 1) / 2.0) * 3));
                var e = types.get(i).create(sp.serverLevel());
                e.moveTo(at.x, 201, at.z, sp.getYRot() + 180f, 0f);
                e.setTarget(sp);
                sp.serverLevel().addFreshEntity(e);
            }
        };
        switch (ticks) {
            case 60 -> onServer.accept(sp -> {
                if (sp.level().dimension() != net.minecraft.world.level.Level.OVERWORLD) {
                    sp.teleportTo(sp.server.overworld(), sp.server.overworld().getSharedSpawnPos().getX(), 120, sp.server.overworld().getSharedSpawnPos().getZ(), 0f, 0f);
                }
                PlayerDBCData data = PlayerDBCData.get(sp);
                data.revive();
                data.setRace(Race.SAIYAN);
                data.setRaceChosen(true);
                data.setTailOut(false);
                data.saveToPlayer(sp);
                ModMessages.syncToTracking(sp);
                sp.setGameMode(GameType.SURVIVAL);
                sp.getAbilities().invulnerable = true;
                sp.getAbilities().mayfly = true;
                sp.getAbilities().flying = true;
                sp.onUpdateAbilities();
                sp.serverLevel().setDayTime(1000);
                // A stone arena up in the sky: nothing in the way of the camera
                int cx = sp.getBlockX(), cz = sp.getBlockZ();
                for (int x = -20; x <= 20; x++)
                    for (int z = -20; z <= 20; z++)
                        sp.serverLevel().setBlockAndUpdate(new BlockPos(cx + x, 200, cz + z), net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
                sp.getAbilities().flying = false;
                sp.onUpdateAbilities();
                sp.teleportTo(sp.serverLevel(), cx + 0.5, 201, cz - 6 + 0.5, 0f, 8f);
                sp.server.setDifficulty(net.minecraft.world.Difficulty.NORMAL, true); // peaceful deletes monsters
            });
            case 80 -> onServer.accept(sp -> spawn.accept(sp, java.util.List.of(com.dragonblock.common.registry.ModEntities.BANDIT.get(),
                    com.dragonblock.common.registry.ModEntities.SOLDIER.get(), com.dragonblock.common.registry.ModEntities.ANDROID.get())));
            case 100 -> shot(mc, "enemies_lineup");
            case 150 -> shot(mc, "enemies_attack");
            case 170 -> onServer.accept(sp -> {
                for (var e : sp.serverLevel().getEntitiesOfClass(com.dragonblock.common.entity.DBEnemy.class, sp.getBoundingBox().inflate(40))) e.discard();
                spawn.accept(sp, java.util.List.of(com.dragonblock.common.registry.ModEntities.EMPEROR.get()));
            });
            case 200 -> shot(mc, "emperor");
            case 210 -> onServer.accept(sp -> {
                for (var e : sp.serverLevel().getEntitiesOfClass(com.dragonblock.common.entity.DBEnemy.class, sp.getBoundingBox().inflate(40))) {
                    e.hurt(sp.damageSources().playerAttack(sp), e.getMaxHealth() * 0.55f);
                }
            });
            case 240 -> shot(mc, "emperor_transforming");
            case 300 -> {
                for (var e : mc.level.getEntitiesOfClass(com.dragonblock.common.entity.DBEnemy.class, mc.player.getBoundingBox().inflate(40))) {
                    DragonBlockMod.LOGGER.info("[autotest] {} phase={} hp={}", e.getKind(), e.getPhase(), e.getHealth());
                }
                shot(mc, "emperor_gold");
            }
            case 320 -> mc.stop();
            default -> {}
        }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "autotest_" + name + ".png", mc.getMainRenderTarget(), c -> {});
        DragonBlockMod.LOGGER.info("[autotest] shot {} in {}", name, mc.level.dimension().location());
    }

    /**
     * The big-server expansion: a view from above of every new place, the safe-zone badge, a hit on a
     * player who is talking (must be shielded) and an instant transmission to the Namekian elder.
     */
    private static void expansionTour(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        var uuid = mc.player.getUUID();
        if (mc.screen instanceof DBCCharacterScreen) mc.setScreen(null);
        java.util.function.Consumer<java.util.function.Consumer<ServerPlayer>> onServer = task -> server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(uuid);
            if (sp != null) task.accept(sp);
        });
        // Hover over a spot: dz blocks south of it and dy above, looking north and down
        java.util.function.BiConsumer<ServerPlayer, Object[]> overlook = (sp, a) -> {
            @SuppressWarnings("unchecked")
            var dim = (net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>) a[0];
            var level = server.getLevel(dim);
            com.dragonblock.common.world.Worlds.ensureBuilt(level);
            BlockPos at = (BlockPos) a[1];
            sp.getAbilities().flying = true;
            sp.onUpdateAbilities();
            sp.teleportTo(level, at.getX() + 0.5, at.getY() + (int) a[2], at.getZ() + (int) a[3] + 0.5, 180f, (float) a[4]);
        };
        java.util.function.Function<com.dragonblock.common.npc.NpcKind, BlockPos> spot = kind -> {
            var s = com.dragonblock.common.world.Landmarks.find(server, kind);
            DragonBlockMod.LOGGER.info("[autotest] landmark {} = {}", kind, s);
            return s == null ? BlockPos.ZERO : s.pos();
        };
        var namek = com.dragonblock.common.world.Planet.NAMEK.getDimension();
        var earth = net.minecraft.world.level.Level.OVERWORLD;
        switch (ticks) {
            case 60 -> onServer.accept(sp -> {
                PlayerDBCData data = PlayerDBCData.get(sp);
                data.setRace(Race.SAIYAN);
                data.setRaceChosen(true);
                data.setTailOut(false);
                data.learn(com.dragonblock.common.data.Technique.TELEPORT);
                for (var k : new com.dragonblock.common.npc.NpcKind[]{com.dragonblock.common.npc.NpcKind.MASTER, com.dragonblock.common.npc.NpcKind.CELESTIAL,
                        com.dragonblock.common.npc.NpcKind.ANNOUNCER, com.dragonblock.common.npc.NpcKind.GUARDIAN, com.dragonblock.common.npc.NpcKind.ELDER}) {
                    data.setFlag(com.dragonblock.common.combat.InstantTransmission.KNOWN + k.id());
                }
                data.saveToPlayer(sp);
                ModMessages.syncToTracking(sp);
                sp.serverLevel().setDayTime(1000);
                DragonBlockMod.LOGGER.info("[autotest] ring at {}", com.dragonblock.common.world.Tournament.ring(server));
                overlook.accept(sp, new Object[]{earth, spot.apply(com.dragonblock.common.npc.NpcKind.ANNOUNCER).north(16), 34, 48, 30f});
            });
            case 140 -> shot(mc, "expand_arena");
            case 150 -> onServer.accept(sp -> overlook.accept(sp, new Object[]{earth, spot.apply(com.dragonblock.common.npc.NpcKind.GUARDIAN), 22, 50, 22f}));
            case 210 -> shot(mc, "expand_palace");
            case 220 -> onServer.accept(sp -> overlook.accept(sp, new Object[]{earth, spot.apply(com.dragonblock.common.npc.NpcKind.GUARDIAN).atY(150), 0, 70, -25f}));
            case 270 -> shot(mc, "expand_tower");
            case 280 -> onServer.accept(sp -> overlook.accept(sp, new Object[]{com.dragonblock.common.world.Worlds.OTHERWORLD, new BlockPos(0, 61, -8), 16, 34, 12f}));
            case 345 -> shot(mc, "expand_otherworld");
            case 350 -> onServer.accept(sp -> overlook.accept(sp, new Object[]{com.dragonblock.common.world.Worlds.OTHERWORLD,
                    new BlockPos(com.dragonblock.common.world.Worlds.roadX(com.dragonblock.common.world.Worlds.ROAD_END), 61, com.dragonblock.common.world.Worlds.ROAD_END), 12, -30, 20f}));
            case 400 -> {
                mc.player.setYRot(0f);
                shot(mc, "expand_roadend");
            }
            case 410 -> onServer.accept(sp -> overlook.accept(sp, new Object[]{com.dragonblock.common.world.Worlds.CELESTIAL, new BlockPos(0, 80, 0), 22, 50, 25f}));
            case 470 -> shot(mc, "expand_celestial");
            case 480 -> onServer.accept(sp -> {
                var level = server.getLevel(namek);
                BlockPos plaza = com.dragonblock.common.world.Planet.NAMEK.landing(level);
                overlook.accept(sp, new Object[]{namek, plaza.north(10), 30, 50, 28f});
            });
            case 550 -> shot(mc, "expand_namek");
            case 560 -> onServer.accept(sp -> {
                overlook.accept(sp, new Object[]{earth, spot.apply(com.dragonblock.common.npc.NpcKind.MASTER), 1, 4, 0f});
                sp.getAbilities().flying = false;
                sp.onUpdateAbilities();
                sp.setGameMode(GameType.SURVIVAL);
            });
            case 600 -> onServer.accept(sp -> {
                for (var npc : sp.serverLevel().getEntitiesOfClass(com.dragonblock.common.entity.NpcEntity.class, sp.getBoundingBox().inflate(20))) {
                    if (npc.getKind() == com.dragonblock.common.npc.NpcKind.MASTER) com.dragonblock.common.npc.Dialogues.open(sp, npc);
                }
                var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(sp.serverLevel());
                float before = sp.getHealth();
                boolean hit = sp.hurt(sp.damageSources().mobAttack(zombie), 6f);
                DragonBlockMod.LOGGER.info("[autotest] talking={} safe={} zombie hit landed={} health {} -> {}",
                        com.dragonblock.common.npc.Dialogues.isTalking(sp), com.dragonblock.common.world.SafeZones.isSafe(sp.level(), sp.position()), hit, before, sp.getHealth());
            });
            case 640 -> shot(mc, "expand_talk");
            case 650 -> mc.setScreen(null);
            case 680 -> shot(mc, "expand_safezone");
            case 690 -> onServer.accept(sp -> {
                PlayerDBCData data = PlayerDBCData.get(sp);
                data.setCurrentKi(data.getMaxKi());
                com.dragonblock.common.combat.InstantTransmission.open(sp, 0);
            });
            case 740 -> shot(mc, "expand_transmission");
            case 750 -> onServer.accept(sp -> com.dragonblock.common.npc.Dialogues.choose(sp, sp.getId(), 4)); // the elder, on Namek
            case 820 -> {
                onServer.accept(sp -> DragonBlockMod.LOGGER.info("[autotest] after transmission: {} at {}", sp.level().dimension().location(), sp.blockPosition()));
                shot(mc, "expand_arrival");
            }
            case 830 -> mc.stop();
            default -> {}
        }
    }

    /** The new enemies in the sky arena: lineup, the demon splitting, the invader's ape under the full moon, a sprout's fuse. */
    private static void bestiary(Minecraft mc) {
        mc.player.setYRot(0f);
        mc.player.setXRot(ticks >= 290 && ticks < 390 ? -14f : 8f);
        var server = mc.getSingleplayerServer();
        var uuid = mc.player.getUUID();
        java.util.function.Consumer<java.util.function.Consumer<ServerPlayer>> onServer = task -> server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(uuid);
            if (sp != null) task.accept(sp);
        });
        java.util.function.BiConsumer<ServerPlayer, java.util.List<net.minecraft.world.entity.EntityType<com.dragonblock.common.entity.DBEnemy>>> spawn = (sp, types) -> {
            for (int i = 0; i < types.size(); i++) {
                var e = types.get(i).create(sp.serverLevel());
                e.moveTo(sp.getX() + (i - (types.size() - 1) / 2.0) * 6, 201, sp.getZ() + 12, 180f, 0f);
                e.setTarget(sp);
                sp.serverLevel().addFreshEntity(e);
            }
        };
        java.util.function.Consumer<ServerPlayer> clear = sp -> {
            for (var e : sp.serverLevel().getEntitiesOfClass(com.dragonblock.common.entity.DBEnemy.class, sp.getBoundingBox().inflate(60))) e.discard();
        };
        java.util.function.Consumer<String> log = tag -> {
            for (var e : mc.level.getEntitiesOfClass(com.dragonblock.common.entity.DBEnemy.class, mc.player.getBoundingBox().inflate(60))) {
                DragonBlockMod.LOGGER.info("[autotest] {}: {} phase={} hp={}/{} scale={}", tag, e.getKind(), e.getPhase(), e.getHealth(), e.getMaxHealth(), e.getScale());
            }
        };
        java.util.function.Consumer<ServerPlayer> freeze = sp -> {
            for (var e : sp.serverLevel().getEntitiesOfClass(com.dragonblock.common.entity.DBEnemy.class, sp.getBoundingBox().inflate(60))) {
                e.setNoAi(true);
                e.setYRot(140f); // three-quarter view
                e.setYBodyRot(140f);
                e.setYHeadRot(140f);
            }
        };
        switch (ticks) {
            case 62 -> onServer.accept(sp -> {
                // Away from the sky palace's shadow
                int cx = sp.getBlockX() + 70, cz = sp.getBlockZ() - 40;
                for (int x = -24; x <= 24; x++)
                    for (int z = -24; z <= 24; z++)
                        sp.serverLevel().setBlockAndUpdate(new BlockPos(cx + x, 200, cz + z), net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
                sp.teleportTo(sp.serverLevel(), cx + 0.5, 201, cz - 6 + 0.5, 0f, 8f);
            });
            case 80 -> onServer.accept(sp -> spawn.accept(sp, java.util.List.of(com.dragonblock.common.registry.ModEntities.SPROUT.get(),
                    com.dragonblock.common.registry.ModEntities.DINO.get(), com.dragonblock.common.registry.ModEntities.RAIDER.get())));
            case 132 -> onServer.accept(freeze);
            case 140 -> shot(mc, "bestiary_lineup");
            case 150 -> onServer.accept(sp -> {
                clear.accept(sp);
                spawn.accept(sp, java.util.List.of(com.dragonblock.common.registry.ModEntities.MAJIN.get()));
            });
            case 172 -> onServer.accept(freeze);
            case 180 -> shot(mc, "bestiary_majin");
            case 190 -> onServer.accept(sp -> {
                for (var e : sp.serverLevel().getEntitiesOfClass(com.dragonblock.common.entity.DBEnemy.class, sp.getBoundingBox().inflate(40))) {
                    e.hurt(sp.damageSources().playerAttack(sp), e.getMaxHealth() * 0.4f);
                }
            });
            case 225 -> {
                log.accept("split");
                shot(mc, "bestiary_split");
            }
            case 240 -> onServer.accept(sp -> {
                clear.accept(sp);
                sp.serverLevel().setDayTime(18000); // midnight of day 0: full moon
                spawn.accept(sp, java.util.List.of(com.dragonblock.common.registry.ModEntities.RAIDER.get()));
            });
            case 300 -> shot(mc, "bestiary_raider_transform");
            case 365 -> onServer.accept(sp -> {
                freeze.accept(sp);
                sp.serverLevel().setDayTime(6000); // daylight for the picture
                for (var e : sp.serverLevel().getEntitiesOfClass(com.dragonblock.common.entity.DBEnemy.class, sp.getBoundingBox().inflate(60))) {
                    e.teleportTo(sp.getX(), 201, sp.getZ() + 16);
                }
            });
            case 380 -> {
                log.accept("ape");
                shot(mc, "bestiary_ape");
            }
            case 390 -> onServer.accept(sp -> {
                clear.accept(sp);
                sp.serverLevel().setDayTime(1000);
                var e = com.dragonblock.common.registry.ModEntities.SPROUT.get().create(sp.serverLevel());
                e.moveTo(sp.getX(), 201, sp.getZ() + 1.8, 180f, 0f);
                e.setTarget(sp);
                sp.serverLevel().addFreshEntity(e);
                e.hurt(sp.damageSources().playerAttack(sp), e.getMaxHealth() * 0.6f);
            });
            case 410 -> shot(mc, "bestiary_fuse");
            case 450 -> {
                log.accept("after fuse (sprout should be gone)");
                mc.stop();
            }
            default -> {}
        }
    }
}
