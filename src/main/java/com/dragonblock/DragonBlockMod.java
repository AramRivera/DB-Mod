package com.dragonblock;

import com.dragonblock.client.ClientConfig;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.registry.*;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
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
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        // Lifecycle listeners
        modEventBus.addListener(this::commonSetup);

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
