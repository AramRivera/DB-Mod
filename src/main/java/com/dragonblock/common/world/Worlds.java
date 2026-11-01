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
 * structure files: the master's island hut and the inventor's lab by the Earth spawn, the judge's plaza
 * over the clouds of the Other World, and the celestial master's tiny planet.
 */
public final class Worlds {
    public static final ResourceKey<Level> OTHERWORLD = key("otherworld");
    public static final ResourceKey<Level> CELESTIAL = key("celestial");

    /** Where you appear in each built world. */
    public static final Vec3 OTHERWORLD_ARRIVAL = new Vec3(0.5, 61, 8.5);
    public static final Vec3 CELESTIAL_ARRIVAL = new Vec3(0.5, 81, 3.5);

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

    public static void ensureBuilt(ServerLevel level) {
        Built built = level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Built::new, Built::load, null), "dragonblock_built");
        if (built.done) return;
        if (level.dimension() == OTHERWORLD) buildOtherworld(level);
        else if (level.dimension() == CELESTIAL) buildCelestial(level);
        else if (level.dimension() == Level.OVERWORLD) buildMasterIsland(level);
        else return;
        built.done = true;
        built.setDirty();
    }

    /** Remembers that a world's landmarks were built. */
    private static final class Built extends SavedData {
        boolean done;

        static Built load(CompoundTag tag, HolderLookup.Provider registries) {
            Built b = new Built();
            b.done = tag.getBoolean("Done");
            return b;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putBoolean("Done", done);
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
            int x = (int) Math.round(Mth.sin(z * 0.07f) * 7);
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
}
