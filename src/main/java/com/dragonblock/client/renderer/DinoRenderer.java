package com.dragonblock.client.renderer;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.entity.DBEnemy;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The dinosaur from assets/dragonblock/dino.json (tools/gen_dino_json.py): jointed parts tinted by color
 * group. Legs stride with a bird-like knee lift, the tail sways in a lagging wave, the head follows its
 * target, and the jaw snaps open on a bite (and now and then in a roar).
 */
public class DinoRenderer extends MobRenderer<DBEnemy, DinoRenderer.Model> {
    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "textures/entity/white.png");

    public DinoRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(), 1.1f);
    }

    @Override
    public ResourceLocation getTextureLocation(DBEnemy enemy) {
        return WHITE;
    }

    /** One joint: its pivot from the parent's, a resting pitch, its color groups and the joints hanging off it. */
    private static final class Joint {
        final float x, y, z, rest;
        final List<ModelPart> groups = new ArrayList<>();
        final List<Integer> colors = new ArrayList<>();
        final List<Joint> children = new ArrayList<>();
        float xRot, yRot, zRot;

        Joint(JsonArray pivot, float rest) {
            this.x = pivot.get(0).getAsFloat();
            this.y = pivot.get(1).getAsFloat();
            this.z = pivot.get(2).getAsFloat();
            this.rest = rest;
        }

        void render(PoseStack pose, VertexConsumer vc, int light, int overlay, int alpha) {
            pose.pushPose();
            pose.translate(x / 16f, y / 16f, z / 16f);
            if (zRot != 0) pose.mulPose(Axis.ZP.rotation(zRot));
            if (yRot != 0) pose.mulPose(Axis.YP.rotation(yRot));
            if (rest + xRot != 0) pose.mulPose(Axis.XP.rotation(rest + xRot));
            for (int i = 0; i < groups.size(); i++) groups.get(i).render(pose, vc, light, overlay, alpha | colors.get(i));
            for (Joint c : children) c.render(pose, vc, light, overlay, alpha);
            pose.popPose();
        }
    }

    public static final class Model extends EntityModel<DBEnemy> {
        private final Map<String, Joint> joints = new HashMap<>();
        private Joint root;

        Model() {
            try (var in = DinoRenderer.class.getResourceAsStream("/assets/" + DragonBlockMod.MODID + "/dino.json")) {
                JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                JsonObject colors = json.getAsJsonObject("colors"), parts = json.getAsJsonObject("parts");
                for (String name : parts.keySet()) {
                    JsonObject p = parts.getAsJsonObject(name);
                    Joint j = new Joint(p.getAsJsonArray("pivot"), p.has("rest") ? p.get("rest").getAsFloat() : 0f);
                    JsonObject boxes = p.getAsJsonObject("boxes");
                    for (String group : boxes.keySet()) {
                        MeshDefinition mesh = new MeshDefinition();
                        CubeListBuilder cubes = CubeListBuilder.create().texOffs(0, 0);
                        for (JsonElement e : boxes.getAsJsonArray(group)) {
                            JsonArray b = e.getAsJsonArray();
                            cubes.addBox(b.get(0).getAsFloat(), b.get(1).getAsFloat(), b.get(2).getAsFloat(),
                                    b.get(3).getAsFloat(), b.get(4).getAsFloat(), b.get(5).getAsFloat());
                        }
                        mesh.getRoot().addOrReplaceChild("g", cubes, PartPose.ZERO);
                        j.groups.add(LayerDefinition.create(mesh, 64, 64).bakeRoot());
                        j.colors.add(colors.has(group) ? colors.get(group).getAsInt() : 0xCCCCCC);
                    }
                    joints.put(name, j);
                }
                for (String name : parts.keySet()) {
                    JsonElement parent = parts.getAsJsonObject(name).get("parent");
                    if (parent == null || parent.isJsonNull()) root = joints.get(name);
                    else joints.get(parent.getAsString()).children.add(joints.get(name));
                }
            } catch (Exception e) {
                DragonBlockMod.LOGGER.error("Could not load the dinosaur model", e);
            }
        }

        private void rot(String name, float x, float y, float z) {
            Joint j = joints.get(name);
            if (j == null) return;
            j.xRot = x;
            j.yRot = y;
            j.zRot = z;
        }

        @Override
        public void setupAnim(DBEnemy dino, float limbSwing, float amount, float age, float headYaw, float headPitch) {
            float stride = limbSwing * 0.45f;
            amount = Math.min(1f, amount);
            for (int side = 0; side < 2; side++) {
                float phase = stride + side * Mth.PI;
                String s = side == 0 ? "left" : "right";
                rot(s + "Leg", Mth.cos(phase) * 0.65f * amount, 0, 0);
                rot(s + "Shin", Math.max(0f, -Mth.sin(phase)) * 0.7f * amount, 0, 0);
                rot(s + "Foot", -Math.max(0f, -Mth.sin(phase)) * 0.35f * amount, 0, 0);
                rot(s + "Arm", -0.3f + Mth.sin(age * 0.12f + side) * 0.08f, 0, (side == 0 ? -1 : 1) * 0.15f);
            }
            // Body sways with the steps; the tail answers a beat later at each segment
            rot("body", Mth.sin(stride * 2) * 0.03f * amount, Mth.sin(stride) * 0.06f * amount, 0);
            float wave = age * 0.07f;
            for (int i = 0; i < 3; i++) {
                float lag = i * 0.7f;
                rot(i == 0 ? "tail" : "tail" + (i + 1), -0.04f + Mth.sin(wave - lag) * 0.03f,
                        (Mth.sin(wave - lag) * 0.1f + Mth.sin(stride - lag) * 0.18f * amount) * (1 + i * 0.3f), 0);
            }
            // Head: tracks the target, breathes, lunges on a bite; roars every ten seconds or so
            float bite = Mth.sin(attackTime * Mth.PI);
            float roarT = (age % 220f) / 30f;
            float roar = roarT < 1f ? Mth.sin(roarT * Mth.PI) : 0f;
            rot("head", Mth.clamp(headPitch, -30f, 30f) * Mth.DEG_TO_RAD * 0.6f + Mth.sin(age * 0.09f) * 0.025f + bite * 0.35f - roar * 0.35f,
                    Mth.clamp(headYaw, -50f, 50f) * Mth.DEG_TO_RAD * 0.7f, 0);
            rot("jaw", 0.06f + Mth.sin(age * 0.09f) * 0.03f + bite * 0.85f + roar * 0.75f, 0, 0);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            if (root == null) return;
            pose.pushPose();
            pose.translate(0f, 1.5f, 0f); // living renderers put the model's y = 24px at the feet; ours stand at y = 0
            root.render(pose, vc, light, overlay, color & 0xFF000000);
            pose.popPose();
        }
    }
}
