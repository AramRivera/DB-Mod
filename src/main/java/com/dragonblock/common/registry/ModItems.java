package com.dragonblock.common.registry;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.item.*;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, DragonBlockMod.MODID);

    public static final RegistryObject<Item> SENZU_BEAN =
            ITEMS.register("senzu_bean", SenzuBeanItem::new);

    public static final RegistryObject<Item> SCOUTER =
            ITEMS.register("scouter", ScouterItem::new);

    public static final RegistryObject<Item> DRAGON_RADAR =
            ITEMS.register("dragon_radar", DragonRadarItem::new);

    public static final RegistryObject<Item> TRAINING_WEIGHTS =
            ITEMS.register("training_weights", TrainingWeightsItem::new);

    public static final RegistryObject<Item> WRIST_WEIGHTS = ITEMS.register("wrist_weights",
            () -> new AccessoryItem(AccessoryItem.Slot.WRISTS, "Puestas: +20% de TP en combate, golpes un poco más lentos."));

    public static final RegistryObject<Item> COMBAT_BELT = ITEMS.register("combat_belt",
            () -> new AccessoryItem(AccessoryItem.Slot.BELT, "Puesto: el ki se regenera un 25% más rápido."));

    public static final RegistryObject<Item> IMPERIAL_BEACON = ITEMS.register("imperial_beacon", ImperialBeaconItem::new);

    public static final RegistryObject<Item> SHIP_CAPSULE = ITEMS.register("ship_capsule", ShipCapsuleItem::new);

    public static final RegistryObject<Item> BALL_CAPSULE = ITEMS.register("ball_capsule", BallCapsuleItem::new);

    public static final RegistryObject<Item> KI_BLAST_ITEM =
            ITEMS.register("ki_blast", KiAttackItem::new);

    // 7 Dragon Balls
    public static final RegistryObject<Item> DRAGON_BALL_1 = ITEMS.register("dragon_ball_1", () -> new DragonBallItem(ModBlocks.DRAGON_BALLS.get(0).get(), 1));
    public static final RegistryObject<Item> DRAGON_BALL_2 = ITEMS.register("dragon_ball_2", () -> new DragonBallItem(ModBlocks.DRAGON_BALLS.get(1).get(), 2));
    public static final RegistryObject<Item> DRAGON_BALL_3 = ITEMS.register("dragon_ball_3", () -> new DragonBallItem(ModBlocks.DRAGON_BALLS.get(2).get(), 3));
    public static final RegistryObject<Item> DRAGON_BALL_4 = ITEMS.register("dragon_ball_4", () -> new DragonBallItem(ModBlocks.DRAGON_BALLS.get(3).get(), 4));
    public static final RegistryObject<Item> DRAGON_BALL_5 = ITEMS.register("dragon_ball_5", () -> new DragonBallItem(ModBlocks.DRAGON_BALLS.get(4).get(), 5));
    public static final RegistryObject<Item> DRAGON_BALL_6 = ITEMS.register("dragon_ball_6", () -> new DragonBallItem(ModBlocks.DRAGON_BALLS.get(5).get(), 6));
    public static final RegistryObject<Item> DRAGON_BALL_7 = ITEMS.register("dragon_ball_7", () -> new DragonBallItem(ModBlocks.DRAGON_BALLS.get(6).get(), 7));
}
