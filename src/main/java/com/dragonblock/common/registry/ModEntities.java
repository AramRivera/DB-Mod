package com.dragonblock.common.registry;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.entity.KiBlastEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DragonBlockMod.MODID);

    public static final RegistryObject<EntityType<KiBlastEntity>> KI_BLAST =
            ENTITY_TYPES.register("ki_blast", () ->
                    EntityType.Builder.<KiBlastEntity>of(KiBlastEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(4)
                            .updateInterval(2)
                            .build("ki_blast")
            );

    public static final RegistryObject<EntityType<com.dragonblock.common.entity.ShenlongEntity>> SHENLONG =
            ENTITY_TYPES.register("shenlong", () ->
                    EntityType.Builder.<com.dragonblock.common.entity.ShenlongEntity>of(com.dragonblock.common.entity.ShenlongEntity::new, MobCategory.MISC)
                            .sized(2f, 2f)
                            .clientTrackingRange(16)
                            .updateInterval(20)
                            .noSave()
                            .build("shenlong")
            );

    public static final RegistryObject<EntityType<com.dragonblock.common.entity.NpcEntity>> NPC =
            ENTITY_TYPES.register("npc", () ->
                    EntityType.Builder.<com.dragonblock.common.entity.NpcEntity>of(com.dragonblock.common.entity.NpcEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(10)
                            .build("npc")
            );

    public static final RegistryObject<EntityType<com.dragonblock.common.entity.SpaceshipEntity>> SPACESHIP =
            ENTITY_TYPES.register("spaceship", () ->
                    EntityType.Builder.<com.dragonblock.common.entity.SpaceshipEntity>of(com.dragonblock.common.entity.SpaceshipEntity::new, MobCategory.MISC)
                            .sized(3.6f, 4.0f)
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build("spaceship")
            );

    public static final RegistryObject<EntityType<com.dragonblock.common.entity.DBEnemy>> BANDIT =
            ENTITY_TYPES.register("bandit", () ->
                    EntityType.Builder.<com.dragonblock.common.entity.DBEnemy>of((type, level) -> new com.dragonblock.common.entity.DBEnemy(type, level,
                                    com.dragonblock.common.entity.EnemyKind.BANDIT), MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(10)
                            .build("bandit")
            );

    public static final RegistryObject<EntityType<com.dragonblock.common.entity.DBEnemy>> SOLDIER =
            ENTITY_TYPES.register("soldier", () ->
                    EntityType.Builder.<com.dragonblock.common.entity.DBEnemy>of((type, level) -> new com.dragonblock.common.entity.DBEnemy(type, level,
                                    com.dragonblock.common.entity.EnemyKind.SOLDIER), MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(10)
                            .build("soldier")
            );

    public static final RegistryObject<EntityType<com.dragonblock.common.entity.DBEnemy>> ANDROID =
            ENTITY_TYPES.register("android", () ->
                    EntityType.Builder.<com.dragonblock.common.entity.DBEnemy>of((type, level) -> new com.dragonblock.common.entity.DBEnemy(type, level,
                                    com.dragonblock.common.entity.EnemyKind.ANDROID), MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(10)
                            .build("android")
            );

    public static final RegistryObject<EntityType<com.dragonblock.common.entity.DBEnemy>> EMPEROR =
            ENTITY_TYPES.register("emperor", () ->
                    EntityType.Builder.<com.dragonblock.common.entity.DBEnemy>of((type, level) -> new com.dragonblock.common.entity.DBEnemy(type, level,
                                    com.dragonblock.common.entity.EnemyKind.EMPEROR), MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(10)
                            .build("emperor")
            );

    public static final RegistryObject<EntityType<com.dragonblock.common.entity.DBEnemy>> SPROUT =
            ENTITY_TYPES.register("sprout", () ->
                    EntityType.Builder.<com.dragonblock.common.entity.DBEnemy>of((type, level) -> new com.dragonblock.common.entity.DBEnemy(type, level,
                                    com.dragonblock.common.entity.EnemyKind.SPROUT), MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(10)
                            .build("sprout")
            );

    public static final RegistryObject<EntityType<com.dragonblock.common.entity.DBEnemy>> DINO =
            ENTITY_TYPES.register("dino", () ->
                    EntityType.Builder.<com.dragonblock.common.entity.DBEnemy>of((type, level) -> new com.dragonblock.common.entity.DBEnemy(type, level,
                                    com.dragonblock.common.entity.EnemyKind.DINO), MobCategory.MONSTER)
                            .sized(1.6f, 2.4f)
                            .clientTrackingRange(10)
                            .build("dino")
            );

    public static final RegistryObject<EntityType<com.dragonblock.common.entity.DBEnemy>> RAIDER =
            ENTITY_TYPES.register("raider", () ->
                    EntityType.Builder.<com.dragonblock.common.entity.DBEnemy>of((type, level) -> new com.dragonblock.common.entity.DBEnemy(type, level,
                                    com.dragonblock.common.entity.EnemyKind.RAIDER), MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(10)
                            .build("raider")
            );

    public static final RegistryObject<EntityType<com.dragonblock.common.entity.DBEnemy>> MAJIN =
            ENTITY_TYPES.register("majin", () ->
                    EntityType.Builder.<com.dragonblock.common.entity.DBEnemy>of((type, level) -> new com.dragonblock.common.entity.DBEnemy(type, level,
                                    com.dragonblock.common.entity.EnemyKind.MAJIN), MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(10)
                            .build("majin")
            );

    public static final RegistryObject<EntityType<com.dragonblock.common.entity.DBEnemy>> MAJIN_SPAWN =
            ENTITY_TYPES.register("majin_spawn", () ->
                    EntityType.Builder.<com.dragonblock.common.entity.DBEnemy>of((type, level) -> new com.dragonblock.common.entity.DBEnemy(type, level,
                                    com.dragonblock.common.entity.EnemyKind.MAJIN_SPAWN), MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(10)
                            .build("majin_spawn")
            );
}
