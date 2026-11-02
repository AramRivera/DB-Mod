package com.dragonblock;

import com.dragonblock.client.ClientConfig;
import net.minecraftforge.fml.config.ModConfig;

import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.registry.ModCreativeTabs;
import com.dragonblock.common.registry.ModEntities;
import com.dragonblock.common.registry.ModItems;
import com.dragonblock.common.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(DragonBlockMod.MODID)
public class DragonBlockMod {
    public static final String MODID = "dragonblock";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DragonBlockMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Register deferred registers
        com.dragonblock.common.registry.ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        com.dragonblock.common.registry.ModMenus.MENUS.register(modEventBus);

        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        // Lifecycle listeners
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent e) -> {
            e.put(ModEntities.NPC.get(), com.dragonblock.common.entity.NpcEntity.attributes().build());
            e.put(ModEntities.BANDIT.get(), com.dragonblock.common.entity.DBEnemy.attributes(com.dragonblock.common.entity.EnemyKind.BANDIT).build());
            e.put(ModEntities.SOLDIER.get(), com.dragonblock.common.entity.DBEnemy.attributes(com.dragonblock.common.entity.EnemyKind.SOLDIER).build());
            e.put(ModEntities.ANDROID.get(), com.dragonblock.common.entity.DBEnemy.attributes(com.dragonblock.common.entity.EnemyKind.ANDROID).build());
            e.put(ModEntities.EMPEROR.get(), com.dragonblock.common.entity.DBEnemy.attributes(com.dragonblock.common.entity.EnemyKind.EMPEROR).build());
            e.put(ModEntities.SPROUT.get(), com.dragonblock.common.entity.DBEnemy.attributes(com.dragonblock.common.entity.EnemyKind.SPROUT).build());
            e.put(ModEntities.DINO.get(), com.dragonblock.common.entity.DBEnemy.attributes(com.dragonblock.common.entity.EnemyKind.DINO).build());
            e.put(ModEntities.RAIDER.get(), com.dragonblock.common.entity.DBEnemy.attributes(com.dragonblock.common.entity.EnemyKind.RAIDER).build());
            e.put(ModEntities.MAJIN.get(), com.dragonblock.common.entity.DBEnemy.attributes(com.dragonblock.common.entity.EnemyKind.MAJIN).build());
            e.put(ModEntities.MAJIN_SPAWN.get(), com.dragonblock.common.entity.DBEnemy.attributes(com.dragonblock.common.entity.EnemyKind.MAJIN_SPAWN).build());
        });
        // Fighters spawn in any light: Namek never gets dark
        modEventBus.addListener((net.minecraftforge.event.entity.SpawnPlacementRegisterEvent e) -> {
            for (var type : java.util.List.of(ModEntities.BANDIT.get(), ModEntities.SOLDIER.get(), ModEntities.ANDROID.get(),
                    ModEntities.SPROUT.get(), ModEntities.DINO.get(), ModEntities.RAIDER.get())) {
                e.register(type, net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules, net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE);
            }
        });

        // Register ourselves to MinecraftForge event bus
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("Dragon Block Mod 1.21.1 inicializado con éxito!");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModMessages.register();
            LOGGER.info("Dragon Block: Red y paquetes de red registrados.");
        });
    }
}
