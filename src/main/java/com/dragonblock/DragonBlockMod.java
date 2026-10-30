package com.dragonblock;

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
        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

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
