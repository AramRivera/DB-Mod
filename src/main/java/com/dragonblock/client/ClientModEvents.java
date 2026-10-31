package com.dragonblock.client;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.client.keybind.ModKeyBindings;
import com.dragonblock.client.overlay.DBCKiOverlay;
import com.dragonblock.client.anim.DBPlayerModel;
import com.dragonblock.client.model.DBModels;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import java.lang.reflect.Field;
import com.dragonblock.client.renderer.DBPlayerLayer;
import com.dragonblock.client.renderer.KiBlastRenderer;
import com.dragonblock.client.renderer.ShenlongRenderer;
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
        event.registerEntityRenderer(ModEntities.SHENLONG.get(), ShenlongRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        DBModels.register(event);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getPlayerSkin(skin) instanceof PlayerRenderer renderer) {
                boolean slim = skin == PlayerSkin.Model.SLIM;
                swapModel(renderer, new DBPlayerModel(
                        event.getContext().bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim));
                renderer.addLayer(new DBPlayerLayer(renderer, event.getEntityModels()));
            }
        }
    }

    /**
     * Replaces LivingEntityRenderer.model so our setupAnim poses every player. Looked up by type
     * instead of name so it works in dev and in obfuscated production builds alike.
     */
    private static void swapModel(PlayerRenderer renderer, DBPlayerModel model) {
        for (Field field : LivingEntityRenderer.class.getDeclaredFields()) {
            if (EntityModel.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    field.set(renderer, model);
                    return;
                } catch (ReflectiveOperationException e) {
                    DragonBlockMod.LOGGER.error("Could not install Dragon Block player model, poses disabled", e);
                }
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
