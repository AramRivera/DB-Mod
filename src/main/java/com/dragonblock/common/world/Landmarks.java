package com.dragonblock.common.world;

import com.dragonblock.common.entity.NpcEntity;
import com.dragonblock.common.npc.NpcKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.EnumMap;
import java.util.Map;

/**
 * Where each character lives, in any world, kept in the server's saved data so instant transmission can
 * reach them while their chunks are unloaded. Worlds records them when it places a character; each
 * character also re-notes its spot now and then, which covers saves built before this existed.
 */
public final class Landmarks extends SavedData {
    public record Spot(ResourceKey<Level> dim, BlockPos pos) {}

    private final Map<NpcKind, Spot> spots = new EnumMap<>(NpcKind.class);

    private static Landmarks of(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Landmarks::new, Landmarks::load, null), "dragonblock_landmarks");
    }

    public static Spot find(MinecraftServer server, NpcKind kind) {
        return of(server).spots.get(kind);
    }

    /** The authoritative home of a character Worlds just placed. */
    public static void set(MinecraftServer server, NpcKind kind, ResourceKey<Level> dim, BlockPos pos) {
        Landmarks l = of(server);
        l.spots.put(kind, new Spot(dim, pos));
        l.setDirty();
    }

    /** A character noting where it stands: only fills a missing spot or follows small moves, so a stray copy can't steal it. */
    public static void note(NpcEntity npc) {
        if (npc.getServer() == null) return;
        Landmarks l = of(npc.getServer());
        Spot old = l.spots.get(npc.getKind());
        BlockPos pos = npc.blockPosition();
        if (old != null && (old.dim() != npc.level().dimension() || old.pos().distSqr(pos) > 64 * 64 || old.pos().equals(pos))) return;
        l.spots.put(npc.getKind(), new Spot(npc.level().dimension(), pos));
        l.setDirty();
    }

    static Landmarks load(CompoundTag tag, HolderLookup.Provider registries) {
        Landmarks l = new Landmarks();
        for (NpcKind kind : NpcKind.values()) {
            if (!tag.contains(kind.id())) continue;
            CompoundTag t = tag.getCompound(kind.id());
            ResourceLocation dim = ResourceLocation.tryParse(t.getString("Dim"));
            if (dim != null) l.spots.put(kind, new Spot(ResourceKey.create(Registries.DIMENSION, dim), BlockPos.of(t.getLong("Pos"))));
        }
        return l;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        spots.forEach((kind, spot) -> {
            CompoundTag t = new CompoundTag();
            t.putString("Dim", spot.dim().location().toString());
            t.putLong("Pos", spot.pos().asLong());
            tag.put(kind.id(), t);
        });
        return tag;
    }
}
