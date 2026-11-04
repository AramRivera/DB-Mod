package com.dragonblock.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Where every dragon ball block sits in a dimension, saved with the world, so the radar can point to a
 * ball no matter how far it was scattered.
 */
public class DragonBallTracker extends SavedData {
    private final Map<Integer, List<Long>> placed = new TreeMap<>();

    public static DragonBallTracker get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(DragonBallTracker::new, DragonBallTracker::load, null), "dragonblock_dragon_balls");
    }

    private static DragonBallTracker load(CompoundTag tag, HolderLookup.Provider registries) {
        DragonBallTracker t = new DragonBallTracker();
        for (String key : tag.getAllKeys()) {
            List<Long> list = new ArrayList<>();
            for (long l : tag.getLongArray(key)) list.add(l);
            t.placed.put(Integer.parseInt(key), list);
        }
        return t;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        placed.forEach((stars, list) -> tag.put(String.valueOf(stars), new LongArrayTag(list)));
        return tag;
    }

    public void put(int stars, BlockPos pos) {
        List<Long> list = placed.computeIfAbsent(stars, k -> new ArrayList<>());
        if (!list.contains(pos.asLong())) list.add(pos.asLong());
        setDirty();
    }

    public void remove(int stars, BlockPos pos) {
        List<Long> list = placed.get(stars);
        if (list != null && list.remove(pos.asLong())) setDirty();
    }

    /** Every placed ball as (stars, position). */
    public List<Map.Entry<Integer, BlockPos>> all() {
        List<Map.Entry<Integer, BlockPos>> out = new ArrayList<>();
        placed.forEach((stars, list) -> list.forEach(l -> out.add(Map.entry(stars, BlockPos.of(l)))));
        return out;
    }
}
