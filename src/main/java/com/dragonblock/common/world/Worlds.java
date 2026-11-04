package com.dragonblock.common.world;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.entity.NpcEntity;
import com.dragonblock.common.npc.NpcKind;
import com.dragonblock.common.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

/**
 * The places of the mod and the jumps between them. Each landmark is built in code the first time its
 * world is visited (remembered in the world's saved data), so they exist in every save without
 * structure files: the master's hut, the inventor's lab, the tournament arena and the tower of the sky
 * palace on Earth; the judge's hall over the clouds of the Other World and its long golden road; the
 * celestial master's planet; the village of the green planet.
 */
public final class Worlds {
    public static final ResourceKey<Level> OTHERWORLD = key("otherworld");
    public static final ResourceKey<Level> CELESTIAL = key("celestial");

    /** Where you appear in each built world. */
    public static final Vec3 OTHERWORLD_ARRIVAL = new Vec3(0.5, 61, 8.5);
    public static final Vec3 CELESTIAL_ARRIVAL = new Vec3(0.5, 81, 3.5);
    /** The golden road of the Other World ends here, at the gate to the celestial master's planet. */
    public static final int ROAD_END = 700;

    /** The road's winding path: its center x at a given z. */
    public static int roadX(int z) {
        return (int) Math.round(Mth.sin(z * 0.07f) * 7);
    }

    private Worlds() {}

    private static ResourceKey<Level> key(String name) {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, name));
    }

    /** Sends a player to a mod world, building it first if nobody has been there yet. */
    public static void send(ServerPlayer player, ResourceKey<Level> dim) {
        ServerLevel level = player.server.getLevel(dim);
        if (level == null) return;
        ensureBuilt(level);
        Vec3 at = dim == OTHERWORLD ? OTHERWORLD_ARRIVAL : CELESTIAL_ARRIVAL;
        player.teleportTo(level, at.x, at.y, at.z, 180f, 0f);
    }

    /** Back to a position in any world, lifted to open air if the spot got buried. */
    public static void sendTo(ServerPlayer player, ResourceKey<Level> dim, BlockPos pos) {
        ServerLevel level = player.server.getLevel(dim);
        if (level == null) level = player.server.overworld();
        BlockPos safe = pos;
        while (safe.getY() < level.getMaxBuildHeight() - 2
                && (!level.getBlockState(safe).getCollisionShape(level, safe).isEmpty() || !level.getBlockState(safe.above()).getCollisionShape(level, safe).isEmpty())) {
            safe = safe.above();
        }
        player.teleportTo(level, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, player.getYRot(), 0f);
    }

    /**
     * Landmarks come in versions so worlds saved before an expansion get only what is new (and their
     * characters aren't placed twice): 1 the first places, 2 the big server expansion.
     */
    private static final int VERSION = 2;

    private static Built built(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Built::new, Built::load, null), "dragonblock_built");
    }

    public static void ensureBuilt(ServerLevel level) {
        Built built = built(level);
        if (built.version >= VERSION) return;
        ResourceKey<Level> dim = level.dimension();
        if (dim == OTHERWORLD) {
            if (built.version < 1) buildOtherworld(level);
            expandOtherworld(level);
        } else if (dim == CELESTIAL) {
            if (built.version < 1) buildCelestial(level);
            expandCelestial(level);
        } else if (dim == Level.OVERWORLD) {
            if (built.version < 1) buildMasterIsland(level);
            buildArena(level);
            buildSkyPalace(level);
        } else if (dim == Planet.NAMEK.getDimension()) {
            built.anchor = buildNamekVillage(level);
        } else return;
        built.version = VERSION;
        built.setDirty();
    }

    /** A world's main arrival spot chosen when it was built (Namek's village plaza), or null. */
    public static BlockPos anchor(ServerLevel level) {
        return built(level).anchor;
    }

    /** Remembers which version of a world's landmarks was built. */
    private static final class Built extends SavedData {
        int version;
        BlockPos anchor;

        static Built load(CompoundTag tag, HolderLookup.Provider registries) {
            Built b = new Built();
            b.version = tag.contains("Version") ? tag.getInt("Version") : tag.getBoolean("Done") ? 1 : 0;
            if (tag.contains("Anchor")) b.anchor = BlockPos.of(tag.getLong("Anchor"));
            return b;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putInt("Version", version);
            if (anchor != null) tag.putLong("Anchor", anchor.asLong());
            return tag;
        }
    }

    public static void onServerStarted(MinecraftServer server) {
        ensureBuilt(server.overworld());
    }

    // ------------------------------------------------------------------ landmarks

    private static void set(ServerLevel l, int x, int y, int z, BlockState s) {
        l.setBlock(new BlockPos(x, y, z), s, 2);
    }

    private static void box(ServerLevel l, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(l, x, y, z, s);
    }

    private static void npc(ServerLevel l, NpcKind kind, double x, double y, double z, float yaw) {
        NpcEntity npc = new NpcEntity(ModEntities.NPC.get(), l);
        npc.setKind(kind);
        npc.moveTo(x, y, z, yaw, 0f);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        l.addFreshEntity(npc);
        Landmarks.set(l.getServer(), kind, l.dimension(), npc.blockPosition());
    }

    /** A white hut with a red tiled roof and the inventor's dome lab, a short walk from the spawn. */
    private static void buildMasterIsland(ServerLevel l) {
        BlockPos spawn = l.getSharedSpawnPos();
        int cx = spawn.getX() + 14, cz = spawn.getZ() + 14;
        int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
        // Level ground under both buildings
        box(l, cx - 5, y - 4, cz - 5, cx + 18, y - 1, cz + 7, Blocks.DIRT.defaultBlockState());
        box(l, cx - 5, y - 1, cz - 5, cx + 18, y - 1, cz + 7, Blocks.GRASS_BLOCK.defaultBlockState());
        box(l, cx - 5, y, cz - 5, cx + 18, y + 9, cz + 7, Blocks.AIR.defaultBlockState());

        // Hut: 7x7, white walls, oak frame, red roof stepping in
        box(l, cx - 3, y - 1, cz - 3, cx + 3, y - 1, cz + 3, Blocks.OAK_PLANKS.defaultBlockState());
        box(l, cx - 3, y, cz - 3, cx + 3, y + 2, cz + 3, Blocks.WHITE_TERRACOTTA.defaultBlockState());
        box(l, cx - 2, y, cz - 2, cx + 2, y + 2, cz + 2, Blocks.AIR.defaultBlockState());
        for (int[] c : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) box(l, cx + c[0], y, cz + c[1], cx + c[0], y + 2, cz + c[1], Blocks.STRIPPED_OAK_LOG.defaultBlockState());
        box(l, cx, y, cz + 3, cx, y + 1, cz + 3, Blocks.AIR.defaultBlockState()); // doorway
        set(l, cx - 2, y + 1, cz + 3, Blocks.GLASS_PANE.defaultBlockState());
        set(l, cx + 2, y + 1, cz + 3, Blocks.GLASS_PANE.defaultBlockState());
        for (int i = 0; i < 4; i++) box(l, cx - 4 + i, y + 3 + i, cz - 4 + i, cx + 4 - i, y + 3 + i, cz + 4 - i, Blocks.RED_TERRACOTTA.defaultBlockState());
        set(l, cx - 2, y, cz - 2, Blocks.CRAFTING_TABLE.defaultBlockState());
        set(l, cx + 2, y + 2, cz - 2, Blocks.LANTERN.defaultBlockState());

        // Lab: a white dome with a band of blue glass
        int lx = cx + 12;
        for (int dx = -5; dx <= 5; dx++)
            for (int dy = 0; dy <= 5; dy++)
                for (int dz = -5; dz <= 5; dz++) {
                    double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (d <= 5.2 && d > 4.2) set(l, lx + dx, y + dy, cz + dz, (dy == 2 ? Blocks.LIGHT_BLUE_STAINED_GLASS : Blocks.WHITE_CONCRETE).defaultBlockState());
                }
        box(l, lx - 4, y - 1, cz - 4, lx + 4, y - 1, cz + 4, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        box(l, lx, y, cz + 4, lx, y + 1, cz + 5, Blocks.AIR.defaultBlockState()); // entrance
        set(l, lx, y + 4, cz, Blocks.SEA_LANTERN.defaultBlockState());

        npc(l, NpcKind.MASTER, cx + 0.5, y, cz + 5.5, 0f);
        npc(l, NpcKind.SCIENTIST, lx + 0.5, y, cz + 6.5, 0f);
    }

    /** The judge's plaza over the clouds: quartz and gold, a giant desk, a red carpet and the winding road. */
    private static void buildOtherworld(ServerLevel l) {
        int y = 61;
        for (int x = -17; x <= 17; x++)
            for (int z = -17; z <= 17; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d <= 16) set(l, x, y - 1, z, (d > 15 ? Blocks.GOLD_BLOCK : Blocks.SMOOTH_QUARTZ).defaultBlockState());
            }
        box(l, -1, y - 1, -12, 1, y - 1, 16, Blocks.RED_WOOL.defaultBlockState()); // carpet to the desk
        // The judge's desk: the giant shows from the chest up behind it, and it is thin enough to talk across
        box(l, -7, y, -14, 7, y + 1, -13, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        box(l, -7, y + 2, -14, 7, y + 2, -13, Blocks.DARK_OAK_SLAB.defaultBlockState());
        set(l, -5, y + 3, -14, Blocks.BOOKSHELF.defaultBlockState());
        set(l, 5, y + 3, -14, Blocks.LANTERN.defaultBlockState());
        for (int[] c : new int[][]{{-12, -8}, {12, -8}, {-12, 8}, {12, 8}}) {
            box(l, c[0], y, c[1], c[0], y + 8, c[1], Blocks.QUARTZ_PILLAR.defaultBlockState());
            set(l, c[0], y + 9, c[1], Blocks.GLOWSTONE.defaultBlockState());
        }
        // The long golden road winding off across the clouds
        for (int z = 17; z < 260; z++) {
            int x = roadX(z);
            box(l, x - 1, y - 1, z, x + 1, y - 1, z, Blocks.YELLOW_CONCRETE.defaultBlockState());
        }
        npc(l, NpcKind.JUDGE, 0.5, y, -16.5, 0f);
    }

    /** A planet you can walk around in a minute: grass over dirt, a little house and a tree on top. */
    private static void buildCelestial(ServerLevel l) {
        int cy = 64, r = 16;
        for (int x = -r; x <= r; x++)
            for (int y = -r; y <= r; y++)
                for (int z = -r; z <= r; z++) {
                    double d = Math.sqrt(x * x + y * y + z * z);
                    if (d > r + 0.5) continue;
                    BlockState s = d > r - 0.6 ? (y > -6 ? Blocks.GRASS_BLOCK : Blocks.STONE).defaultBlockState()
                            : d > r - 3 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState();
                    set(l, x, cy + y, z, s);
                }
        int top = cy + r + 1;
        box(l, -3, top - 1, -3, 3, top - 1, 3, Blocks.GRASS_BLOCK.defaultBlockState());
        box(l, -2, top, -4, 2, top + 2, -1, Blocks.QUARTZ_BLOCK.defaultBlockState());
        box(l, -1, top, -3, 1, top + 1, -2, Blocks.AIR.defaultBlockState());
        set(l, 0, top, -1, Blocks.AIR.defaultBlockState());
        set(l, 0, top + 1, -1, Blocks.AIR.defaultBlockState());
        box(l, -3, top + 3, -5, 3, top + 3, 0, Blocks.RED_TERRACOTTA.defaultBlockState());
        box(l, -2, top + 4, -4, 2, top + 4, -1, Blocks.RED_TERRACOTTA.defaultBlockState());
        box(l, 6, top - 2, 3, 6, top + 2, 3, Blocks.OAK_LOG.defaultBlockState());
        for (int x = -2; x <= 2; x++)
            for (int y = 0; y <= 2; y++)
                for (int z = -2; z <= 2; z++) if (x * x + y * y + z * z <= 5) set(l, 6 + x, top + 3 + y, 3 + z, Blocks.OAK_LEAVES.defaultBlockState());
        npc(l, NpcKind.CELESTIAL, -1.5, top, 1.5, 0f);
    }

    // ------------------------------------------------------------------ version 2: room for a full server

    private static int ground(ServerLevel l, int x, int z) {
        l.getChunk(x >> 4, z >> 4);
        return l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    /** Fills a disc (horizontal circle) of radius r at height y. */
    private static void disc(ServerLevel l, int cx, int y, int cz, double r, BlockState s) {
        int ri = (int) Math.ceil(r);
        for (int x = -ri; x <= ri; x++)
            for (int z = -ri; z <= ri; z++) if (x * x + z * z <= r * r) set(l, cx + x, y, cz + z, s);
    }

    /** A hollow half-sphere of radius r standing on (cx, y, cz), one block thick. */
    private static void dome(ServerLevel l, int cx, int y, int cz, double r, BlockState s) {
        int ri = (int) Math.ceil(r);
        for (int x = -ri; x <= ri; x++)
            for (int dy = 0; dy <= ri; dy++)
                for (int z = -ri; z <= ri; z++) {
                    double d = Math.sqrt(x * x + dy * dy + z * z);
                    if (d <= r + 0.5 && d > r - 0.5) set(l, cx + x, y + dy, cz + z, s);
                    else if (d <= r - 0.5) set(l, cx + x, y + dy, cz + z, Blocks.AIR.defaultBlockState());
                }
    }

    /** The master's hut on Earth: the hub everything else is placed around. */
    private static BlockPos hub(ServerLevel l) {
        Landmarks.Spot s = Landmarks.find(l.getServer(), NpcKind.MASTER);
        if (s != null && s.dim() == Level.OVERWORLD) return s.pos().north(5);
        BlockPos spawn = l.getSharedSpawnPos();
        return new BlockPos(spawn.getX() + 14, 0, spawn.getZ() + 14);
    }

    /**
     * The martial arts tournament: a raised stone ring on a paved square, stands on both sides, a red
     * hall at the back and a gate with two pagoda towers. The host waits by the ring (see Tournament).
     */
    private static void buildArena(ServerLevel l) {
        BlockPos hub = hub(l);
        int ax = hub.getX(), az = hub.getZ() - 70, h = Tournament.HALF;
        // Level at the average height of the square: cut the high side, fill the low side down to the ground
        int sum = 0, n = 0;
        for (int x = -30; x <= 30; x += 6)
            for (int z = -30; z <= 30; z += 6, n++) sum += ground(l, ax + x, az + z);
        int y = sum / n;
        box(l, ax - 30, y, az - 30, ax + 30, y + 26, az + 30, Blocks.AIR.defaultBlockState());
        for (int x = -30; x <= 30; x++)
            for (int z = -30; z <= 30; z++) {
                int g = Math.min(ground(l, ax + x, az + z), y - 2);
                box(l, ax + x, g - 3, az + z, ax + x, y - 2, az + z, Blocks.STONE.defaultBlockState());
            }
        box(l, ax - 30, y - 1, az - 30, ax + 30, y - 1, az + 30, Blocks.POLISHED_ANDESITE.defaultBlockState());
        box(l, ax - 1, y - 1, az + h, ax + 1, y - 1, az + 30, Blocks.SMOOTH_QUARTZ.defaultBlockState()); // walk to the gate
        // The ring: 5x5 tiles in two stones, quartz edge
        for (int x = -h; x <= h; x++)
            for (int z = -h; z <= h; z++) {
                boolean edge = Math.abs(x) == h || Math.abs(z) == h;
                boolean tile = ((x + h) / 5 + (z + h) / 5) % 2 == 0;
                set(l, ax + x, y, az + z, (edge ? Blocks.QUARTZ_BLOCK : tile ? Blocks.SMOOTH_STONE : Blocks.POLISHED_DIORITE).defaultBlockState());
            }
        // Stands east and west: four steps in red and white
        for (int side = -1; side <= 1; side += 2)
            for (int k = 0; k < 4; k++) {
                int x0 = ax + side * (h + 6 + k * 2), x1 = x0 + side;
                box(l, x0, y, az - h, x1, y + k, az + h, (k % 2 == 0 ? Blocks.RED_CONCRETE : Blocks.WHITE_CONCRETE).defaultBlockState());
            }
        // The hall at the back
        int hz = az - h - 9;
        box(l, ax - 16, y, hz - 4, ax + 16, y + 7, hz, Blocks.RED_TERRACOTTA.defaultBlockState());
        box(l, ax - 15, y, hz - 3, ax + 15, y + 6, hz - 1, Blocks.AIR.defaultBlockState());
        box(l, ax - 2, y, hz, ax + 2, y + 4, hz, Blocks.AIR.defaultBlockState());
        for (int i = 0; i < 4; i++) box(l, ax - 18 + i, y + 8 + i, hz - 6 + i, ax + 18 - i, y + 8 + i, hz + 2 - i, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        box(l, ax - 3, y + 12, hz - 2, ax + 3, y + 12, hz - 2, Blocks.GOLD_BLOCK.defaultBlockState());
        // The gate: two pagoda towers and a beam between them
        for (int side = -1; side <= 1; side += 2) {
            int tx = ax + side * 7, tz = az + h + 12;
            box(l, tx - 2, y, tz - 2, tx + 2, y + 10, tz + 2, Blocks.WHITE_TERRACOTTA.defaultBlockState());
            for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) box(l, tx + c[0], y, tz + c[1], tx + c[0], y + 10, tz + c[1], Blocks.RED_TERRACOTTA.defaultBlockState());
            for (int i = 0; i < 3; i++) box(l, tx - 4 + i, y + 11 + i * 2, tz - 4 + i, tx + 4 - i, y + 11 + i * 2, tz + 4 - i, Blocks.DARK_OAK_PLANKS.defaultBlockState());
            set(l, tx, y + 16, tz, Blocks.GOLD_BLOCK.defaultBlockState());
        }
        box(l, ax - 5, y + 9, az + h + 12, ax + 5, y + 10, az + h + 12, Blocks.RED_TERRACOTTA.defaultBlockState());
        set(l, ax, y + 10, az + h + 11, Blocks.GOLD_BLOCK.defaultBlockState());
        // Lamps at the corners of the ring
        for (int[] c : new int[][]{{-h - 2, -h - 2}, {h + 2, -h - 2}, {-h - 2, h + 2}, {h + 2, h + 2}}) {
            box(l, ax + c[0], y, az + c[1], ax + c[0], y + 4, az + c[1], Blocks.DARK_OAK_FENCE.defaultBlockState());
            set(l, ax + c[0], y + 5, az + c[1], Blocks.LANTERN.defaultBlockState());
        }
        npc(l, NpcKind.ANNOUNCER, ax + 0.5, y, az + h + 4.5, 180f);
    }

    /**
     * A white tower from the ground to the clouds, with a ladder for those who climb it the hard way,
     * holding up the sky palace: a round white platform with a domed temple, palms and the guardian.
     */
    private static void buildSkyPalace(ServerLevel l) {
        BlockPos hub = hub(l);
        int tx = hub.getX() - 50, tz = hub.getZ() + 10, base = ground(l, tx, tz), top = 228;
        BlockState white = Blocks.WHITE_CONCRETE.defaultBlockState(), quartz = Blocks.SMOOTH_QUARTZ.defaultBlockState();
        box(l, tx - 1, base - 3, tz - 1, tx + 1, top - 1, tz + 1, white);
        // Platform with a bowl underneath
        for (int k = 0; k <= 8; k++) disc(l, tx, top - k, tz, 26 - k * 2.8, k == 0 ? quartz : white);
        disc(l, tx, top, tz, 25.5, quartz);
        for (int a = 0; a < 360; a += 2) { // gold rim
            double r = Math.toRadians(a);
            set(l, tx + (int) Math.round(Math.cos(r) * 26), top, tz + (int) Math.round(Math.sin(r) * 26), Blocks.GOLD_BLOCK.defaultBlockState());
        }
        box(l, tx - 30, top + 1, tz - 30, tx + 30, top + 20, tz + 30, Blocks.AIR.defaultBlockState());
        // Ladder on the south face, all the way up through the platform
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, net.minecraft.core.Direction.SOUTH);
        for (int y = base; y <= top; y++) {
            set(l, tx, y, tz + 2, ladder);
            if (y >= top - 8) set(l, tx, y, tz + 1, white);
        }
        // The temple: round walls, a dome and a golden tip
        int px = tx, pz = tz - 10;
        for (int y = top + 1; y <= top + 5; y++)
            for (int a = 0; a < 360; a += 3) {
                double r = Math.toRadians(a);
                set(l, px + (int) Math.round(Math.cos(r) * 8), y, pz + (int) Math.round(Math.sin(r) * 8), white);
            }
        dome(l, px, top + 6, pz, 8, white);
        box(l, px, top + 14, pz, px, top + 16, pz, Blocks.GOLD_BLOCK.defaultBlockState());
        box(l, px - 1, top + 1, pz + 7, px + 1, top + 4, pz + 9, Blocks.AIR.defaultBlockState()); // doorway
        for (int[] c : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) set(l, px + c[0], top + 1, pz + c[1], Blocks.GLOWSTONE.defaultBlockState());
        // Palms around the platform
        for (int[] c : new int[][]{{-16, 6}, {16, 6}, {-12, 16}, {12, 16}, {-19, -6}, {19, -6}}) {
            int x = tx + c[0], z = tz + c[1];
            box(l, x, top + 1, z, x, top + 6, z, Blocks.JUNGLE_LOG.defaultBlockState());
            for (int[] d : new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {2, 0}, {-2, 0}, {0, 2}, {0, -2}}) set(l, x + d[0], top + 7, z + d[1], Blocks.JUNGLE_LEAVES.defaultBlockState());
            for (int[] d : new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}}) set(l, x + d[0], top + 6, z + d[1], Blocks.JUNGLE_LEAVES.defaultBlockState());
        }
        npc(l, NpcKind.GUARDIAN, tx + 0.5, top + 1, tz + 0.5, 180f);
    }

    /** The judge's plaza grows into a great hall over a sea of clouds, and the golden road runs far longer. */
    private static void expandOtherworld(ServerLevel l) {
        int y = 61;
        for (int x = -31; x <= 31; x++)
            for (int z = -31; z <= 31; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > 30.5) continue;
                boolean ring = (d > 29.5) || (d > 21 && d < 22);
                set(l, x, y - 1, z, (ring ? Blocks.GOLD_BLOCK : Blocks.SMOOTH_QUARTZ).defaultBlockState());
            }
        box(l, -1, y - 1, -12, 1, y - 1, 30, Blocks.RED_WOOL.defaultBlockState());
        // The hall behind the desk
        box(l, -22, y, -26, 22, y + 18, -24, Blocks.QUARTZ_BRICKS.defaultBlockState());
        for (int side = -1; side <= 1; side += 2) box(l, side * 22, y, -26, side * 22, y + 18, -15, Blocks.QUARTZ_BRICKS.defaultBlockState());
        for (int x = -20; x <= 20; x += 8) box(l, x, y, -23, x, y + 18, -23, Blocks.QUARTZ_PILLAR.defaultBlockState());
        for (int i = 0; i < 5; i++) box(l, -24 + i, y + 19 + i, -28 + i, 24 - i, y + 19 + i, -13 - i, Blocks.RED_TERRACOTTA.defaultBlockState());
        box(l, -4, y + 24, -21, 4, y + 24, -20, Blocks.GOLD_BLOCK.defaultBlockState());
        for (int x = -16; x <= 16; x += 8) set(l, x, y + 12, -23, Blocks.GLOWSTONE.defaultBlockState());
        // Sea of clouds below and around
        java.util.Random rnd = new java.util.Random(1234);
        for (int i = 0; i < 70; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, dist = 36 + rnd.nextDouble() * 110;
            int cx = (int) (Math.cos(a) * dist), cz = (int) (Math.sin(a) * dist), cy = y - 4 - rnd.nextInt(10);
            if (Math.abs(cx - roadX(cz)) < 8 && cz > 0) continue;
            int rx = 4 + rnd.nextInt(6), ry = 2 + rnd.nextInt(2), rz = 3 + rnd.nextInt(5);
            for (int x = -rx; x <= rx; x++)
                for (int dy = -ry; dy <= ry; dy++)
                    for (int z = -rz; z <= rz; z++)
                        if ((double) x * x / (rx * rx) + (double) dy * dy / (ry * ry) + (double) z * z / (rz * rz) <= 1) set(l, cx + x, cy + dy, cz + z, Blocks.WHITE_WOOL.defaultBlockState());
        }
        // The road, on to the far gate, with a lantern post every fifty blocks
        for (int z = 31; z <= ROAD_END; z++) {
            int x = roadX(z);
            box(l, x - 1, y - 1, z, x + 1, y - 1, z, Blocks.YELLOW_CONCRETE.defaultBlockState());
            if (z % 50 == 0) {
                box(l, x + 2, y - 1, z, x + 2, y + 2, z, Blocks.GOLD_BLOCK.defaultBlockState());
                set(l, x + 2, y + 3, z, Blocks.LANTERN.defaultBlockState());
            }
        }
        int ex = roadX(ROAD_END);
        disc(l, ex, y - 1, ROAD_END + 5, 5.5, Blocks.GOLD_BLOCK.defaultBlockState());
        disc(l, ex, y - 1, ROAD_END + 5, 2.5, Blocks.AMETHYST_BLOCK.defaultBlockState());
        for (int a = 0; a <= 180; a += 4) { // arch over the gate
            double r = Math.toRadians(a);
            set(l, ex + (int) Math.round(Math.cos(r) * 5), y + (int) Math.round(Math.sin(r) * 6), ROAD_END + 5, Blocks.GOLD_BLOCK.defaultBlockState());
        }
    }

    /** True when a player stands on the gate at the end of the road. */
    public static boolean atRoadEnd(ServerPlayer p) {
        return p.level().dimension() == OTHERWORLD && Math.abs(p.getZ() - (ROAD_END + 5.5)) < 2.5 && Math.abs(p.getX() - roadX(ROAD_END) - 0.5) < 2.5;
    }

    /** The celestial planet, twice as wide: a hollow shell around the old one, with more to see on top. */
    private static void expandCelestial(ServerLevel l) {
        int cy = 50, r = 30;
        for (int x = -r; x <= r; x++)
            for (int y = -r; y <= r; y++)
                for (int z = -r; z <= r; z++) {
                    double d = Math.sqrt(x * x + y * y + z * z);
                    if (d > r + 0.5 || d <= r - 3) continue;
                    BlockState s = d > r - 0.6 ? (y > -10 ? Blocks.GRASS_BLOCK : Blocks.STONE).defaultBlockState()
                            : d > r - 2 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState();
                    set(l, x, cy + y, z, s);
                }
        // The old planet's top sits exactly on the new top: rebuild the house over it
        int top = 81;
        box(l, -4, top, -6, 4, top + 6, 2, Blocks.AIR.defaultBlockState());
        box(l, -3, top - 1, -3, 3, top - 1, 3, Blocks.GRASS_BLOCK.defaultBlockState());
        box(l, -2, top, -4, 2, top + 2, -1, Blocks.QUARTZ_BLOCK.defaultBlockState());
        box(l, -1, top, -3, 1, top + 1, -2, Blocks.AIR.defaultBlockState());
        set(l, 0, top, -1, Blocks.AIR.defaultBlockState());
        set(l, 0, top + 1, -1, Blocks.AIR.defaultBlockState());
        box(l, -3, top + 3, -5, 3, top + 3, 0, Blocks.RED_TERRACOTTA.defaultBlockState());
        box(l, -2, top + 4, -4, 2, top + 4, -1, Blocks.RED_TERRACOTTA.defaultBlockState());
        // Trees on the slopes, a path around the house and a sparring circle
        for (int[] t : new int[][]{{10, 4}, {-12, 6}, {6, -14}, {-8, -12}, {15, -6}}) {
            int x = t[0], z = t[1];
            int gy = cy + (int) Math.floor(Math.sqrt(Math.max(0, r * r - x * x - z * z)));
            box(l, x, gy + 1, z, x, gy + 5, z, Blocks.OAK_LOG.defaultBlockState());
            for (int dx = -2; dx <= 2; dx++)
                for (int dy = 0; dy <= 2; dy++)
                    for (int dz = -2; dz <= 2; dz++) if (dx * dx + dy * dy + dz * dz <= 5) set(l, x + dx, gy + 5 + dy, z + dz, Blocks.OAK_LEAVES.defaultBlockState());
        }
        for (int a = 0; a < 360; a += 3) {
            double rad = Math.toRadians(a);
            int x = (int) Math.round(Math.cos(rad) * 7), z = (int) Math.round(Math.sin(rad) * 7);
            int gy = cy + (int) Math.floor(Math.sqrt(Math.max(0, r * r - x * x - z * z)));
            set(l, x, gy, z, Blocks.DIRT_PATH.defaultBlockState());
        }
    }

    /**
     * A village of white domes around a plaza on the green planet, the elder's great dome on a mound to
     * the north, and villagers. Returns the plaza center, where ships land.
     */
    private static BlockPos buildNamekVillage(ServerLevel l) {
        BlockPos c = Planet.dryLand(l);
        int y = c.getY();
        // Level the village ground
        box(l, c.getX() - 34, y, c.getZ() - 40, c.getX() + 34, y + 20, c.getZ() + 34, Blocks.AIR.defaultBlockState());
        box(l, c.getX() - 34, y - 4, c.getZ() - 40, c.getX() + 34, y - 2, c.getZ() + 34, Blocks.DIRT.defaultBlockState());
        box(l, c.getX() - 34, y - 1, c.getZ() - 40, c.getX() + 34, y - 1, c.getZ() + 34, Blocks.GRASS_BLOCK.defaultBlockState());
        disc(l, c.getX(), y - 1, c.getZ(), 8.5, Blocks.DIRT_PATH.defaultBlockState());
        // Houses in a ring, doors facing the plaza
        for (int k = 1; k <= 7; k++) { // every 45 degrees except the north (270), where the elder lives
            double a = Math.toRadians(270 + k * 45);
            int hx = c.getX() + (int) Math.round(Math.cos(a) * 19), hz = c.getZ() + (int) Math.round(Math.sin(a) * 19);
            dome(l, hx, y, hz, 4, Blocks.WHITE_CONCRETE.defaultBlockState());
            disc(l, hx, y - 1, hz, 3.5, Blocks.SMOOTH_SANDSTONE.defaultBlockState());
            boolean alongX = Math.abs(Math.cos(a)) > Math.abs(Math.sin(a)) + 1e-3;
            int dx = alongX ? (int) -Math.signum(Math.cos(a)) : 0, dz = alongX ? 0 : (int) -Math.signum(Math.sin(a));
            box(l, hx + dx * 4, y, hz + dz * 4, hx + dx * 4, y + 1, hz + dz * 4, Blocks.AIR.defaultBlockState());
            for (int[] w : new int[][]{{4, 0}, {-4, 0}, {0, 4}, {0, -4}}) {
                if (w[0] == dx * 4 && w[1] == dz * 4) continue;
                set(l, hx + w[0], y + 2, hz + w[1], Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
            }
            set(l, hx, y + 3, hz, Blocks.SEA_LANTERN.defaultBlockState());
        }
        // The elder's house: a great dome on a stepped mound, a wide door to the south
        int ex = c.getX(), ez = c.getZ() - 28;
        for (int k = 0; k < 3; k++) disc(l, ex, y + k, ez, 11 - k, Blocks.GRASS_BLOCK.defaultBlockState());
        dome(l, ex, y + 3, ez, 8, Blocks.WHITE_CONCRETE.defaultBlockState());
        box(l, ex - 1, y + 3, ez + 7, ex + 1, y + 6, ez + 9, Blocks.AIR.defaultBlockState());
        set(l, ex, y + 8, ez, Blocks.SEA_LANTERN.defaultBlockState());
        for (int[] d : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) set(l, ex + d[0], y + 2, ez + d[1], Blocks.SEA_LANTERN.defaultBlockState());
        npc(l, NpcKind.ELDER, ex + 0.5, y + 3, ez + 0.5, 0f);
        npc(l, NpcKind.VILLAGER, c.getX() + 3.5, y, c.getZ() + 2.5, 135f);
        npc(l, NpcKind.VILLAGER, c.getX() - 4.5, y, c.getZ() - 1.5, -90f);
        npc(l, NpcKind.VILLAGER, c.getX() + 0.5, y, c.getZ() + 6.5, 180f);
        return c;
    }
}
