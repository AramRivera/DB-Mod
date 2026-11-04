package com.dragonblock.common.registry;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.block.DragonBallBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.stream.IntStream;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, DragonBlockMod.MODID);

    /** Index 0 is the one-star ball. */
    public static final List<RegistryObject<Block>> DRAGON_BALLS = IntStream.rangeClosed(1, 7)
            .mapToObj(i -> BLOCKS.<Block>register("dragon_ball_" + i, () -> new DragonBallBlock(i)))
            .toList();
}
