package com.dragonblock.client;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.client.keybind.ModKeyBindings;
import com.dragonblock.client.overlay.DBCKiOverlay;
import com.dragonblock.client.model.DBModels;
import com.dragonblock.client.renderer.DBPlayerLayer;
import com.dragonblock.client.renderer.KiBlastRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import com.dragonblock.common.registry.ModEntities;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DragonBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.KI_BLAST.get(), KiBlastRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        DBModels.register(event);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getPlayerSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new DBPlayerLayer(renderer, event.getEntityModels()));
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (var key : ModKeyBindings.ALL) event.register(key);
    }

    @SubscribeEvent
    public static void onAddGuiLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(
                ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "ki_overlay"),
                DBCKiOverlay.INSTANCE
        );
    }
}
