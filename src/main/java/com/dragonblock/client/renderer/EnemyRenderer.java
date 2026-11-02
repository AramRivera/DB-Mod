package com.dragonblock.client.renderer;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.entity.DBEnemy;
import com.dragonblock.common.entity.EnemyKind;
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
import net.minecraft.util.Mth;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Hostile fighters: a player-shaped body with their skin (textures/entity/enemy) and 3D gear from
 * assets/dragonblock/enemy_extras.json (tools/gen_enemy_assets.py). Ki users raise an arm while aiming;
 * the emperor glows while he transforms and keeps a golden aura in his second form.
 */
public class EnemyRenderer extends HumanoidMobRenderer<DBEnemy, PlayerModel<DBEnemy>> {
    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "textures/entity/white.png");

    public EnemyRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        addLayer(new Gear(this));
    }

    @Override
    public ResourceLocation getTextureLocation(DBEnemy enemy) {
        String name = enemy.getKind() == EnemyKind.EMPEROR && enemy.getPhase() == 1 ? "emperor_gold" : enemy.getKind().id();
        return ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "textures/entity/enemy/" + name + ".png");
    }

    @Override
    protected void scale(DBEnemy enemy, PoseStack pose, float partialTick) {
        pose.scale(0.9375f, 0.9375f, 0.9375f);
    }

    /** Humanoid model whose right arm points at the target while the fighter aims a ki blast. */
    private static final class Model extends PlayerModel<DBEnemy> {
        Model(ModelPart root) {
            super(root, false);
        }

        @Override
        public void setupAnim(DBEnemy enemy, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch) {
            super.setupAnim(enemy, limbSwing, limbSwingAmount, age, headYaw, headPitch);
            if (enemy.isAiming()) {
                rightArm.xRot = -Mth.HALF_PI + headPitch * Mth.DEG_TO_RAD;
                rightArm.yRot = headYaw * Mth.DEG_TO_RAD - 0.1f;
                leftArm.xRot = -1.2f;
                leftArm.yRot = 0.5f;
            }
            if (enemy.getTransforming() > 0) {
                // Arms flexed down and out, head thrown back: the power-up stance
                float shake = Mth.sin(age * 2.7f) * 0.04f;
                rightArm.xRot = 0.15f + shake; rightArm.zRot = 0.8f;
                leftArm.xRot = 0.15f - shake; leftArm.zRot = -0.8f;
                head.xRot = -0.45f;
            }
            rightSleeve.copyFrom(rightArm);
            leftSleeve.copyFrom(leftArm);
            hat.copyFrom(head);
        }
    }

    private record Piece(String part, ModelPart model, int color, int goldColor) {}

    private static final class Gear extends RenderLayer<DBEnemy, PlayerModel<DBEnemy>> {
        private final Map<EnemyKind, List<Piece>> pieces = new EnumMap<>(EnemyKind.class);

        Gear(RenderLayerParent<DBEnemy, PlayerModel<DBEnemy>> parent) {
            super(parent);
            try (var in = EnemyRenderer.class.getResourceAsStream("/assets/" + DragonBlockMod.MODID + "/enemy_extras.json")) {
                JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (EnemyKind kind : EnemyKind.values()) {
                    if (!root.has(kind.id())) continue;
                    JsonObject e = root.getAsJsonObject(kind.id());
                    JsonObject colors = e.getAsJsonObject("colors");
                    JsonObject gold = e.has("gold_colors") ? e.getAsJsonObject("gold_colors") : colors;
                    List<Piece> list = new ArrayList<>();
                    for (String part : e.keySet()) {
                        if (part.endsWith("colors")) continue;
                        JsonObject groups = e.getAsJsonObject(part);
                        for (String group : groups.keySet()) {
                            MeshDefinition mesh = new MeshDefinition();
                            CubeListBuilder cubes = CubeListBuilder.create().texOffs(0, 0);
                            for (JsonElement el : groups.getAsJsonArray(group)) {
                                JsonArray b = el.getAsJsonArray();
                                cubes.addBox(b.get(0).getAsFloat(), b.get(1).getAsFloat(), b.get(2).getAsFloat(),
                                        b.get(3).getAsFloat(), b.get(4).getAsFloat(), b.get(5).getAsFloat());
                            }
                            mesh.getRoot().addOrReplaceChild("g", cubes, PartPose.ZERO);
                            int c = colors.has(group) ? colors.get(group).getAsInt() : 0xCCCCCC;
                            int gc = gold.has(group) ? gold.get(group).getAsInt() : c;
                            list.add(new Piece(part, LayerDefinition.create(mesh, 64, 64).bakeRoot(), 0xFF000000 | c, 0xFF000000 | gc));
                        }
                    }
                    pieces.put(kind, list);
                }
            } catch (Exception ex) {
                DragonBlockMod.LOGGER.error("Could not load enemy gear", ex);
            }
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, DBEnemy enemy, float limbSwing, float limbSwingAmount,
                           float partialTick, float age, float headYaw, float headPitch) {
            PlayerModel<DBEnemy> body = getParentModel();
            boolean gold = enemy.getPhase() == 1;
            for (Piece p : pieces.getOrDefault(enemy.getKind(), List.of())) {
                ModelPart anchor = switch (p.part()) {
                    case "head" -> body.head;
                    case "body" -> body.body;
                    case "rightArm" -> body.rightArm;
                    case "leftArm" -> body.leftArm;
                    default -> null;
                };
                if (anchor == null) continue;
                pose.pushPose();
                anchor.translateAndRotate(pose);
                p.model().render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE)), light, OverlayTexture.NO_OVERLAY,
                        gold ? p.goldColor() : p.color());
                pose.popPose();
            }
            // The emperor's power: crackling while he transforms, a golden aura afterwards
            int t = enemy.getTransforming();
            if (t > 0) {
                float k = 1f - t / (float) DBEnemy.TRANSFORM_TICKS;
                EnergyRender.aura(pose, buffers, age, 0xE8C040, 0.4f + 0.6f * k, 1.2f + 0.6f * k, 1f, 1.1f, true);
                if (((int) age) % 4 < 2) EnergyRender.lightning(pose, buffers, enemy.getId() + (int) age, 0xFFE070, 3, 1f);
            } else if (gold) {
                EnergyRender.aura(pose, buffers, age, 0xE8C040, 0.55f, 1.3f, 1f, 1f, false);
            }
        }
    }
}
