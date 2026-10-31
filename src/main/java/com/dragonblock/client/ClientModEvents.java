package com.dragonblock.client;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.client.keybind.ModKeyBindings;
import com.dragonblock.client.overlay.DBCKiOverlay;
import com.dragonblock.client.renderer.KiBlastRenderer;
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
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyBindings.CHARGE_KI_KEY);
        event.register(ModKeyBindings.TRANSFORM_KEY);
        event.register(ModKeyBindings.CHARACTER_SHEET_KEY);
        event.register(ModKeyBindings.FLY_KEY);
        event.register(ModKeyBindings.KI_BLAST_KEY);
    }

    @SubscribeEvent
    public static void onAddGuiLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(
                ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "ki_overlay"),
                DBCKiOverlay.INSTANCE
        );
    }
}
