package com.dragonblock.client.renderer;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.entity.SpaceshipEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** The capsule ship from assets/dragonblock/ship.json (tools/gen_ship_json.py), wobbling a little in flight. */
public class ShipRenderer extends EntityRenderer<SpaceshipEntity> {
    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "textures/entity/white.png");

    private record Group(ModelPart part, int color) {}

    private final List<Group> groups = new ArrayList<>();

    public ShipRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 1.6f;
        try (var in = ShipRenderer.class.getResourceAsStream("/assets/" + DragonBlockMod.MODID + "/ship.json")) {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject colors = root.getAsJsonObject("colors");
            for (String name : root.keySet()) {
                if (name.equals("colors")) continue;
                MeshDefinition mesh = new MeshDefinition();
                CubeListBuilder cubes = CubeListBuilder.create().texOffs(0, 0);
                for (JsonElement e : root.getAsJsonArray(name)) {
                    JsonArray b = e.getAsJsonArray();
                    cubes.addBox(b.get(0).getAsFloat(), b.get(1).getAsFloat(), b.get(2).getAsFloat(),
                            b.get(3).getAsFloat(), b.get(4).getAsFloat(), b.get(5).getAsFloat());
                }
                mesh.getRoot().addOrReplaceChild("g", cubes, PartPose.ZERO);
                groups.add(new Group(LayerDefinition.create(mesh, 64, 64).bakeRoot(), 0xFF000000 | colors.get(name).getAsInt()));
            }
        } catch (Exception e) {
            DragonBlockMod.LOGGER.error("Could not load the ship model", e);
        }
    }

    @Override
    public void render(SpaceshipEntity ship, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180f - Mth.rotLerp(partial, ship.yRotO, ship.getYRot())));
        if (ship.getState() != SpaceshipEntity.State.IDLE) {
            float t = ship.tickCount + partial;
            pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(t * 0.7f) * 1.5f));
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.cos(t * 0.6f) * 1.5f));
        }
        pose.scale(-1f, -1f, 1f); // model pixels are y-down
        for (Group g : groups) {
            // Culled faces, so from inside the hull you see out through it
            g.part().render(pose, buffers.getBuffer(RenderType.entityCutout(WHITE)), light, OverlayTexture.NO_OVERLAY, g.color());
        }
        pose.popPose();
        super.render(ship, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SpaceshipEntity ship) {
        return WHITE;
    }
}
