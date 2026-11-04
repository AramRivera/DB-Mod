package com.dragonblock.client.renderer;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.entity.NpcEntity;
import com.dragonblock.common.npc.NpcKind;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Characters: a player-shaped body with their own skin (textures/entity/npc) plus their 3D details from
 * assets/dragonblock/npc_extras.json (beards, hats, goggles, staff), both made by tools/gen_npc_assets.py.
 */
public class NpcRenderer extends HumanoidMobRenderer<NpcEntity, PlayerModel<NpcEntity>> {
    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "textures/entity/white.png");

    public NpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        addLayer(new Extras(this));
    }

    @Override
    public ResourceLocation getTextureLocation(NpcEntity npc) {
        return ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "textures/entity/npc/" + npc.getKind().id() + ".png");
    }

    @Override
    protected void scale(NpcEntity npc, PoseStack pose, float partialTick) {
        float s = 0.9375f * npc.getKind().getScale();
        pose.scale(s, s, s);
    }

    /** One tinted model per color group, attached to the body part it belongs to. */
    private record Piece(String part, ModelPart model, int color) {}

    private static final class Extras extends RenderLayer<NpcEntity, PlayerModel<NpcEntity>> {
        private final Map<NpcKind, java.util.List<Piece>> pieces = new EnumMap<>(NpcKind.class);

        Extras(RenderLayerParent<NpcEntity, PlayerModel<NpcEntity>> parent) {
            super(parent);
            try (var in = NpcRenderer.class.getResourceAsStream("/assets/" + DragonBlockMod.MODID + "/npc_extras.json")) {
                JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (NpcKind kind : NpcKind.values()) {
                    if (!root.has(kind.id())) continue;
                    JsonObject npc = root.getAsJsonObject(kind.id());
                    JsonObject colors = npc.getAsJsonObject("colors");
                    java.util.List<Piece> list = new java.util.ArrayList<>();
                    for (String part : npc.keySet()) {
                        if (part.equals("colors")) continue;
                        JsonObject groups = npc.getAsJsonObject(part);
                        for (String group : groups.keySet()) {
                            MeshDefinition mesh = new MeshDefinition();
                            CubeListBuilder cubes = CubeListBuilder.create().texOffs(0, 0);
                            for (JsonElement e : groups.getAsJsonArray(group)) {
                                JsonArray b = e.getAsJsonArray();
                                cubes.addBox(b.get(0).getAsFloat(), b.get(1).getAsFloat(), b.get(2).getAsFloat(),
                                        b.get(3).getAsFloat(), b.get(4).getAsFloat(), b.get(5).getAsFloat());
                            }
                            mesh.getRoot().addOrReplaceChild("g", cubes, PartPose.ZERO);
                            int color = colors != null && colors.has(group) ? colors.get(group).getAsInt() : 0xCCCCCC;
                            list.add(new Piece(part, LayerDefinition.create(mesh, 64, 64).bakeRoot(), 0xFF000000 | color));
                        }
                    }
                    pieces.put(kind, list);
                }
            } catch (Exception e) {
                DragonBlockMod.LOGGER.error("Could not load NPC extras", e);
            }
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, NpcEntity npc, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            PlayerModel<NpcEntity> body = getParentModel();
            Map<String, ModelPart> parts = new HashMap<>();
            parts.put("head", body.head);
            parts.put("body", body.body);
            parts.put("rightArm", body.rightArm);
            parts.put("leftArm", body.leftArm);
            for (Piece p : pieces.getOrDefault(npc.getKind(), java.util.List.of())) {
                ModelPart anchor = parts.get(p.part());
                if (anchor == null) continue;
                pose.pushPose();
                anchor.translateAndRotate(pose);
                p.model().render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE)), light, OverlayTexture.NO_OVERLAY, p.color());
                pose.popPose();
            }
        }
    }
}
