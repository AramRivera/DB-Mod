package com.dragonblock.common.registry;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.block.DragonBallBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, DragonBlockMod.MODID);

    public static final RegistryObject<DragonBallBlock> DRAGON_BALL_1 = BLOCKS.register("dragon_ball_1", () -> new DragonBallBlock(1));
    public static final RegistryObject<DragonBallBlock> DRAGON_BALL_2 = BLOCKS.register("dragon_ball_2", () -> new DragonBallBlock(2));
    public static final RegistryObject<DragonBallBlock> DRAGON_BALL_3 = BLOCKS.register("dragon_ball_3", () -> new DragonBallBlock(3));
    public static final RegistryObject<DragonBallBlock> DRAGON_BALL_4 = BLOCKS.register("dragon_ball_4", () -> new DragonBallBlock(4));
    public static final RegistryObject<DragonBallBlock> DRAGON_BALL_5 = BLOCKS.register("dragon_ball_5", () -> new DragonBallBlock(5));
    public static final RegistryObject<DragonBallBlock> DRAGON_BALL_6 = BLOCKS.register("dragon_ball_6", () -> new DragonBallBlock(6));
    public static final RegistryObject<DragonBallBlock> DRAGON_BALL_7 = BLOCKS.register("dragon_ball_7", () -> new DragonBallBlock(7));
}
