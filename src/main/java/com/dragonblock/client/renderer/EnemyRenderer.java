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
 * the emperor glows while he transforms and keeps a golden aura in his second form. The sprout swells
 * and flashes white on its fuse, the demon waddles, and the invader's ape hunches and beats its chest.
 */
public class EnemyRenderer extends HumanoidMobRenderer<DBEnemy, PlayerModel<DBEnemy>> {
    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "textures/entity/white.png");

    public EnemyRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        addLayer(new Gear(this));
    }

    @Override
    public ResourceLocation getTextureLocation(DBEnemy enemy) {
        String name = switch (enemy.getKind()) {
            case EMPEROR -> enemy.getPhase() == 1 ? "emperor_gold" : "emperor";
            case RAIDER -> enemy.getPhase() == 1 ? "raider_ape" : "raider";
            case MAJIN_SPAWN -> "majin";
            default -> enemy.getKind().id();
        };
        return ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "textures/entity/enemy/" + name + ".png");
    }

    @Override
    protected void scale(DBEnemy enemy, PoseStack pose, float partialTick) {
        float s = 0.9375f;
        if (enemy.getKind() == EnemyKind.SPROUT && enemy.getTransforming() > 0) {
            float k = 1f - (enemy.getTransforming() - partialTick) / DBEnemy.FUSE_TICKS;
            s *= 1f + k * 0.35f + Mth.sin(enemy.tickCount * 1.6f) * 0.04f * k; // swelling and trembling
        }
        if (enemy.getKind() == EnemyKind.MAJIN || enemy.getKind() == EnemyKind.MAJIN_SPAWN) pose.scale(s * 1.12f, s, s * 1.12f); // round
        else pose.scale(s, s, s);
    }

    /** The sprout's fuse blinks white faster and faster, like it's about to burst. */
    @Override
    protected float getWhiteOverlayProgress(DBEnemy enemy, float partialTick) {
        if (enemy.getKind() != EnemyKind.SPROUT || enemy.getTransforming() <= 0) return 0f;
        float k = 1f - enemy.getTransforming() / (float) DBEnemy.FUSE_TICKS;
        return (int) (k * 10f) % 2 == 0 ? 0f : Mth.clamp(k, 0.3f, 1f);
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
            EnemyKind kind = enemy.getKind();
            head.z = 0f; // the ape pose moves it; vanilla only resets head.y
            if (kind == EnemyKind.MAJIN || kind == EnemyKind.MAJIN_SPAWN) {
                // A bouncy waddle: body rolls with the steps, arms swing wide
                float roll = Mth.sin(limbSwing * 0.66f) * 0.12f * Math.min(1f, limbSwingAmount);
                body.zRot = roll;
                head.zRot = roll * 0.6f;
                rightArm.zRot += 0.35f;
                leftArm.zRot -= 0.35f;
            }
            if (kind == EnemyKind.RAIDER && enemy.getPhase() == 1) {
                // The ape: hunched, long arms hanging forward; beats its chest now and then
                body.xRot = 0.35f;
                head.y = 2f;
                head.z = -2.5f; // set, not added: these aren't reset between frames
                rightArm.xRot = rightArm.xRot * 0.6f - 0.35f;
                leftArm.xRot = leftArm.xRot * 0.6f - 0.35f;
                float beat = (age % 160f) / 30f;
                if (beat < 1f && !enemy.isAiming()) {
                    float hit = Mth.abs(Mth.sin(beat * Mth.PI * 4f));
                    rightArm.xRot = -1.3f - hit * 0.3f;
                    leftArm.xRot = -1.3f - (1 - hit) * 0.3f;
                    rightArm.yRot = -0.6f;
                    leftArm.yRot = 0.6f;
                    head.xRot = -0.5f;
                }
            }
            if (kind == EnemyKind.SPROUT && enemy.getTransforming() > 0) {
                // Clinging on: both arms forward around you
                rightArm.xRot = -1.5f;
                leftArm.xRot = -1.5f;
                rightArm.yRot = 0.4f;
                leftArm.yRot = -0.4f;
            }
            if (enemy.getTransforming() > 0 && kind != EnemyKind.SPROUT) {
                // Arms flexed down and out, head thrown back: the power-up stance
                float shake = Mth.sin(age * 2.7f) * 0.04f;
                rightArm.xRot = 0.15f + shake; rightArm.zRot = 0.8f;
                leftArm.xRot = 0.15f - shake; leftArm.zRot = -0.8f;
                head.xRot = -0.45f;
            }
            rightSleeve.copyFrom(rightArm);
            leftSleeve.copyFrom(leftArm);
            jacket.copyFrom(body);
            hat.copyFrom(head);
        }
    }

    private record Piece(String part, String group, ModelPart model, int color, int goldColor) {}

    private static final class Gear extends RenderLayer<DBEnemy, PlayerModel<DBEnemy>> {
        private final Map<EnemyKind, List<Piece>> pieces = new EnumMap<>(EnemyKind.class);

        Gear(RenderLayerParent<DBEnemy, PlayerModel<DBEnemy>> parent) {
            super(parent);
            try (var in = EnemyRenderer.class.getResourceAsStream("/assets/" + DragonBlockMod.MODID + "/enemy_extras.json")) {
                JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (EnemyKind kind : EnemyKind.values()) {
                    String id = kind == EnemyKind.MAJIN_SPAWN ? EnemyKind.MAJIN.id() : kind.id();
                    if (!root.has(id)) continue;
                    JsonObject e = root.getAsJsonObject(id);
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
                            list.add(new Piece(part, group, LayerDefinition.create(mesh, 64, 64).bakeRoot(), 0xFF000000 | c, 0xFF000000 | gc));
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
                if (p.group().endsWith(gold ? "_p0" : "_p1")) continue; // gear of the other form
                ModelPart anchor = switch (p.part()) {
                    case "head" -> body.head;
                    case "body" -> body.body;
                    case "rightArm" -> body.rightArm;
                    case "leftArm" -> body.leftArm;
                    case "rightLeg" -> body.rightLeg;
                    case "leftLeg" -> body.leftLeg;
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
            int t = enemy.getKind() == EnemyKind.SPROUT ? 0 : enemy.getTransforming();
            if (t > 0) {
                float k = 1f - t / (float) DBEnemy.TRANSFORM_TICKS;
                EnergyRender.aura(pose, buffers, age, 0xE8C040, 0.4f + 0.6f * k, 1.2f + 0.6f * k, 1f, 1.1f, true);
                if (((int) age) % 4 < 2) EnergyRender.lightning(pose, buffers, enemy.getId() + (int) age, 0xFFE070, 3, 1f);
            } else if (gold && enemy.getKind() == EnemyKind.EMPEROR) {
                EnergyRender.aura(pose, buffers, age, 0xE8C040, 0.55f, 1.3f, 1f, 1f, false);
            }
        }
    }
}
