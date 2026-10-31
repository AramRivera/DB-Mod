package com.dragonblock.client.model;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.entity.ShenlongEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Shenlong Model for Minecraft 1.21.1 Forge.
 * Modeled and textured via Blockbench with 100+ cubes, golden stag antlers,
 * glowing ruby eyes, open roaring jaw, fluid whiskers, 15-segment coiling body,
 * dark green dorsal spines, cream ribbed underbelly, and sharp front talons.
 */
public class ShenlongModel extends HierarchicalModel<ShenlongEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "shenlong"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart whiskerLeftBase;
    private final ModelPart whiskerRightBase;
    private final ModelPart armLeft;
    private final ModelPart armRight;
    private final ModelPart body;
    private final ModelPart[] bodySegments = new ModelPart[15];

    public ShenlongModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.whiskerLeftBase = this.head.getChild("whisker_left_base");
        this.whiskerRightBase = this.head.getChild("whisker_right_base");
        this.armLeft = root.getChild("arm_left");
        this.armRight = root.getChild("arm_right");
        this.body = root.getChild("body");
        for (int i = 0; i < 15; i++) {
            this.bodySegments[i] = this.body.getChild("body_seg_" + (i + 1));
        }
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(78, 43).addBox(-5.0F, -4.0F, -6.0F, 10.0F, 8.0F, 10.0F, new CubeDeformation(0.0F))
		.texOffs(33, 136).addBox(-3.8F, -1.5F, -17.0F, 7.6F, 4.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(0, 214).addBox(-1.8F, -2.8F, -16.5F, 3.6F, 1.3F, 10.5F, new CubeDeformation(0.0F))
		.texOffs(77, 312).addBox(-3.2F, -1.0F, -18.5F, 6.4F, 3.5F, 1.5F, new CubeDeformation(0.0F))
		.texOffs(36, 320).addBox(1.2F, -2.0F, -18.2F, 1.6F, 1.5F, 1.7F, new CubeDeformation(0.0F))
		.texOffs(45, 320).addBox(-2.8F, -2.0F, -18.2F, 1.6F, 1.5F, 1.7F, new CubeDeformation(0.0F))
		.texOffs(0, 312).addBox(1.8F, -2.8F, -10.0F, 3.4F, 1.6F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(19, 312).addBox(-5.2F, -2.8F, -10.0F, 3.4F, 1.6F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(96, 312).addBox(2.2F, -1.2F, -9.5F, 2.6F, 1.7F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(109, 312).addBox(-4.8F, -1.2F, -9.5F, 2.6F, 1.7F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(0, 320).addBox(2.2F, 2.5F, -15.5F, 1.2F, 3.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(9, 320).addBox(-3.4F, 2.5F, -15.5F, 1.2F, 3.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(54, 320).addBox(-2.2F, 2.5F, -16.8F, 4.4F, 1.3F, 1.3F, new CubeDeformation(0.0F))
		.texOffs(59, 293).addBox(5.0F, -1.0F, -4.0F, 2.5F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(74, 293).addBox(5.0F, -3.0F, 0.0F, 3.5F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(91, 293).addBox(-7.5F, -1.0F, -4.0F, 2.5F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(106, 293).addBox(-8.5F, -3.0F, 0.0F, 3.5F, 4.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(38, 312).addBox(-0.8F, -7.0F, -4.0F, 1.6F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(0, 303).addBox(-0.8F, -7.5F, 0.0F, 1.6F, 3.5F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -24.0F, -14.0F));

		PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(72, 136).addBox(-3.8F, 0.0F, -11.0F, 7.6F, 3.5F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(89, 256).addBox(-2.8F, 3.2F, -10.0F, 5.6F, 2.3F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(25, 281).addBox(-2.2F, -0.4F, -9.0F, 4.4F, 1.4F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(0, 269).addBox(-2.5F, 0.2F, 0.0F, 5.0F, 3.0F, 8.0F, new CubeDeformation(0.0F))
		.texOffs(18, 320).addBox(2.1F, -3.0F, -9.5F, 1.2F, 3.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(27, 320).addBox(-3.3F, -3.0F, -9.5F, 1.2F, 3.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(69, 320).addBox(-2.1F, -1.4F, -10.8F, 4.2F, 1.4F, 1.3F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 3.0F, -6.0F, 0.384F, 0.0F, 0.0F));

		PartDefinition horn_left = head.addOrReplaceChild("horn_left", CubeListBuilder.create().texOffs(27, 269).addBox(-0.8F, -8.0F, -1.2F, 2.0F, 8.0F, 2.4F, new CubeDeformation(0.0F))
		.texOffs(0, 293).addBox(-0.6F, -8.5F, -4.5F, 1.6F, 4.5F, 3.3F, new CubeDeformation(0.0F))
		.texOffs(13, 293).addBox(-0.6F, -13.0F, 0.5F, 1.4F, 5.5F, 2.5F, new CubeDeformation(0.0F))
		.texOffs(52, 281).addBox(-0.2F, -14.0F, -2.0F, 1.4F, 6.5F, 2.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.0F, -3.0F, 0.0F, -0.384F, 0.2094F, 0.3142F));

		PartDefinition horn_right = head.addOrReplaceChild("horn_right", CubeListBuilder.create().texOffs(38, 269).addBox(-1.2F, -8.0F, -1.2F, 2.0F, 8.0F, 2.4F, new CubeDeformation(0.0F))
		.texOffs(24, 293).addBox(-1.0F, -8.5F, -4.5F, 1.6F, 4.5F, 3.3F, new CubeDeformation(0.0F))
		.texOffs(37, 293).addBox(-0.8F, -13.0F, 0.5F, 1.4F, 5.5F, 2.5F, new CubeDeformation(0.0F))
		.texOffs(63, 281).addBox(-1.2F, -14.0F, -2.0F, 1.4F, 6.5F, 2.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0F, -3.0F, 0.0F, -0.384F, -0.2094F, -0.3142F));

		PartDefinition whisker_left_base = head.addOrReplaceChild("whisker_left_base", CubeListBuilder.create().texOffs(74, 281).addBox(-0.7F, -0.7F, -0.5F, 1.4F, 1.4F, 7.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(3.5F, 0.5F, -16.0F, 0.0F, 0.6981F, -0.2618F));

		PartDefinition whisker_left_mid = whisker_left_base.addOrReplaceChild("whisker_left_mid", CubeListBuilder.create().texOffs(49, 269).addBox(-0.6F, -0.6F, 0.0F, 1.2F, 1.2F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 7.0F, 0.2094F, 0.4363F, 0.3491F));

		PartDefinition whisker_left_tip = whisker_left_mid.addOrReplaceChild("whisker_left_tip", CubeListBuilder.create().texOffs(31, 242).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 9.0F, -0.2618F, 0.1745F, 0.4363F));

		PartDefinition whisker_right_base = head.addOrReplaceChild("whisker_right_base", CubeListBuilder.create().texOffs(95, 281).addBox(-0.7F, -0.7F, -0.5F, 1.4F, 1.4F, 7.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.5F, 0.5F, -16.0F, 0.0F, -0.6981F, 0.2618F));

		PartDefinition whisker_right_mid = whisker_right_base.addOrReplaceChild("whisker_right_mid", CubeListBuilder.create().texOffs(72, 269).addBox(-0.6F, -0.6F, 0.0F, 1.2F, 1.2F, 9.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 7.0F, 0.2094F, -0.4363F, -0.3491F));

		PartDefinition whisker_right_tip = whisker_right_mid.addOrReplaceChild("whisker_right_tip", CubeListBuilder.create().texOffs(56, 242).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 9.0F, -0.2618F, -0.1745F, -0.4363F));

		PartDefinition arm_left = partdefinition.addOrReplaceChild("arm_left", CubeListBuilder.create().texOffs(81, 242).addBox(-0.5F, -2.5F, -2.0F, 3.0F, 5.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(14.0F, -20.0F, -10.0F, -0.2618F, 0.7854F, -0.3491F));

		PartDefinition forearm_left = arm_left.addOrReplaceChild("forearm_left", CubeListBuilder.create().texOffs(35, 101).addBox(-1.5F, -2.5F, -11.0F, 3.0F, 5.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(95, 269).addBox(-2.5F, -2.0F, -17.0F, 5.5F, 5.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(13, 303).addBox(1.5F, -1.0F, -21.5F, 2.0F, 2.5F, 4.5F, new CubeDeformation(0.0F))
		.texOffs(28, 303).addBox(-1.0F, -1.2F, -22.0F, 2.0F, 2.4F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(43, 303).addBox(-3.5F, -1.0F, -21.5F, 2.0F, 2.5F, 4.5F, new CubeDeformation(0.0F))
		.texOffs(51, 312).addBox(-5.8F, 0.0F, -18.5F, 1.8F, 2.5F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.0F, 0.0F, 5.0F, 0.4363F, 0.2618F, 0.3491F));

		PartDefinition arm_right = partdefinition.addOrReplaceChild("arm_right", CubeListBuilder.create().texOffs(102, 242).addBox(-2.5F, -2.5F, -2.0F, 3.0F, 5.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-14.0F, -20.0F, -10.0F, -0.2618F, -0.7854F, 0.3491F));

		PartDefinition forearm_right = arm_right.addOrReplaceChild("forearm_right", CubeListBuilder.create().texOffs(64, 101).addBox(-1.5F, -2.5F, -11.0F, 3.0F, 5.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(0, 281).addBox(-3.0F, -2.0F, -17.0F, 5.5F, 5.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(58, 303).addBox(-3.5F, -1.0F, -21.5F, 2.0F, 2.5F, 4.5F, new CubeDeformation(0.0F))
		.texOffs(73, 303).addBox(-1.0F, -1.2F, -22.0F, 2.0F, 2.4F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(88, 303).addBox(1.5F, -1.0F, -21.5F, 2.0F, 2.5F, 4.5F, new CubeDeformation(0.0F))
		.texOffs(64, 312).addBox(4.0F, 0.0F, -18.5F, 1.8F, 2.5F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.0F, 0.0F, 5.0F, 0.4363F, -0.2618F, -0.3491F));

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(48, 293).addBox(15.5F, 42.5F, 8.0F, 1.0F, 4.5F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(103, 303).addBox(15.6F, 44.0F, 5.0F, 0.8F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -22.0F, -10.0F));

		PartDefinition body_seg_1 = body.addOrReplaceChild("body_seg_1", CubeListBuilder.create().texOffs(0, 63).addBox(-4.0F, -4.0F, -9.8195F, 8.0F, 8.0F, 9.8195F, new CubeDeformation(0.0F))
		.texOffs(0, 256).addBox(-3.0F, 2.4F, -9.7195F, 6.0F, 2.0F, 9.6195F, new CubeDeformation(0.0F))
		.texOffs(33, 256).addBox(-0.6F, -6.5F, -9.3195F, 1.2F, 2.7F, 8.8195F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.2187F, 3.1416F, 0.0F));

		PartDefinition body_seg_2 = body.addOrReplaceChild("body_seg_2", CubeListBuilder.create().texOffs(41, 22).addBox(-3.8714F, -3.8714F, -10.4995F, 7.7429F, 7.7429F, 10.4995F, new CubeDeformation(0.0F))
		.texOffs(31, 214).addBox(-2.9036F, 2.3229F, -10.3995F, 5.8071F, 1.9486F, 10.2995F, new CubeDeformation(0.0F))
		.texOffs(66, 214).addBox(-0.6F, -6.3714F, -9.9995F, 1.2F, 2.7F, 9.4995F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 2.0F, 9.0F, 0.3079F, -2.583F, 0.0F));

		PartDefinition body_seg_3 = body.addOrReplaceChild("body_seg_3", CubeListBuilder.create().texOffs(80, 22).addBox(-3.7429F, -3.7429F, -10.034F, 7.4857F, 7.4857F, 10.034F, new CubeDeformation(0.0F))
		.texOffs(56, 256).addBox(-2.8071F, 2.2457F, -9.934F, 5.6143F, 1.8971F, 9.834F, new CubeDeformation(0.0F))
		.texOffs(91, 214).addBox(-0.6F, -6.2429F, -9.534F, 1.2F, 2.7F, 9.034F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, 5.0F, 17.0F, 0.4379F, -1.9296F, 0.0F));

		PartDefinition body_seg_4 = body.addOrReplaceChild("body_seg_4", CubeListBuilder.create().texOffs(0, 43).addBox(-3.6143F, -3.6143F, -10.6499F, 7.2286F, 7.2286F, 10.6499F, new CubeDeformation(0.0F))
		.texOffs(0, 228).addBox(-2.7107F, 2.1686F, -10.5499F, 5.4214F, 1.8457F, 10.4499F, new CubeDeformation(0.0F))
		.texOffs(35, 228).addBox(-0.6F, -6.1143F, -10.1499F, 1.2F, 2.7F, 9.6499F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(13.0F, 9.0F, 20.0F, 0.4094F, -0.8622F, 0.0F));

		PartDefinition body_seg_5 = body.addOrReplaceChild("body_seg_5", CubeListBuilder.create().texOffs(39, 43).addBox(-3.4857F, -3.4857F, -11.825F, 6.9714F, 6.9714F, 11.825F, new CubeDeformation(0.0F))
		.texOffs(0, 169).addBox(-2.6143F, 2.0914F, -11.725F, 5.2286F, 1.7943F, 11.625F, new CubeDeformation(0.0F))
		.texOffs(37, 169).addBox(-0.6F, -5.9857F, -11.325F, 1.2F, 2.7F, 10.825F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(20.0F, 13.0F, 14.0F, 0.4617F, -0.0997F, 0.0F));

		PartDefinition body_seg_6 = body.addOrReplaceChild("body_seg_6", CubeListBuilder.create().texOffs(86, 0).addBox(-3.3571F, -3.3571F, -12.4743F, 6.7143F, 6.7143F, 12.4743F, new CubeDeformation(0.0F))
		.texOffs(0, 153).addBox(-2.5179F, 2.0143F, -12.3743F, 5.0357F, 1.7429F, 12.2743F, new CubeDeformation(0.0F))
		.texOffs(39, 153).addBox(-0.6F, -5.8571F, -11.9743F, 1.2F, 2.7F, 11.4743F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(21.0F, 18.0F, 4.0F, 0.3436F, 0.4636F, 0.0F));

		PartDefinition body_seg_7 = body.addOrReplaceChild("body_seg_7", CubeListBuilder.create().texOffs(0, 22).addBox(-3.2286F, -3.2286F, -12.0891F, 6.4571F, 6.4571F, 12.0891F, new CubeDeformation(0.0F))
		.texOffs(64, 169).addBox(-2.4214F, 1.9371F, -11.9891F, 4.8429F, 1.6914F, 11.8891F, new CubeDeformation(0.0F))
		.texOffs(68, 153).addBox(-0.6F, -5.7286F, -11.5891F, 1.2F, 2.7F, 11.0891F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(16.0F, 22.0F, -6.0F, 0.3556F, 1.1903F, 0.0F));

		PartDefinition body_seg_8 = body.addOrReplaceChild("body_seg_8", CubeListBuilder.create().texOffs(0, 0).addBox(-3.1F, -3.1F, -13.3279F, 6.2F, 6.2F, 13.3279F, new CubeDeformation(0.0F))
		.texOffs(0, 119).addBox(-2.325F, 1.86F, -13.2279F, 4.65F, 1.64F, 13.1279F, new CubeDeformation(0.0F))
		.texOffs(39, 119).addBox(-0.6F, -5.6F, -12.8279F, 1.2F, 2.7F, 12.3279F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(6.0F, 26.0F, -10.0F, 0.2379F, 1.8158F, 0.0F));

		PartDefinition body_seg_9 = body.addOrReplaceChild("body_seg_9", CubeListBuilder.create().texOffs(43, 0).addBox(-2.9714F, -2.9714F, -14.384F, 5.9429F, 5.9429F, 14.384F, new CubeDeformation(0.0F))
		.texOffs(37, 82).addBox(-2.2286F, 1.7829F, -14.284F, 4.4571F, 1.5886F, 14.184F, new CubeDeformation(0.0F))
		.texOffs(78, 82).addBox(-0.6F, -5.4714F, -13.884F, 1.2F, 2.7F, 13.384F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-6.0F, 29.0F, -7.0F, 0.2194F, 2.3036F, 0.0F));

		PartDefinition body_seg_10 = body.addOrReplaceChild("body_seg_10", CubeListBuilder.create().texOffs(37, 63).addBox(-2.8429F, -2.8429F, -11.7803F, 5.6857F, 5.6857F, 11.7803F, new CubeDeformation(0.0F))
		.texOffs(0, 184).addBox(-2.1321F, 1.7057F, -11.6803F, 4.2643F, 1.5371F, 11.5803F, new CubeDeformation(0.0F))
		.texOffs(35, 184).addBox(-0.6F, -5.3429F, -11.2803F, 1.2F, 2.7F, 10.7803F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-16.0F, 32.0F, 2.0F, 0.2717F, 2.7611F, 0.0F));

		PartDefinition body_seg_11 = body.addOrReplaceChild("body_seg_11", CubeListBuilder.create().texOffs(74, 63).addBox(-2.7143F, -2.7143F, -11.4628F, 5.4286F, 5.4286F, 11.4628F, new CubeDeformation(0.0F))
		.texOffs(62, 184).addBox(-2.0357F, 1.6286F, -11.3628F, 4.0714F, 1.4857F, 11.2628F, new CubeDeformation(0.0F))
		.texOffs(97, 184).addBox(-0.6F, -5.2143F, -10.9628F, 1.2F, 2.7F, 10.4628F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-20.0F, 35.0F, 12.0F, 0.2798F, -2.8501F, 0.0F));

		PartDefinition body_seg_12 = body.addOrReplaceChild("body_seg_12", CubeListBuilder.create().texOffs(0, 82).addBox(-2.5857F, -2.5857F, -11.825F, 5.1714F, 5.1714F, 11.825F, new CubeDeformation(0.0F))
		.texOffs(0, 199).addBox(-1.9393F, 1.5514F, -11.725F, 3.8786F, 1.4343F, 11.625F, new CubeDeformation(0.0F))
		.texOffs(33, 199).addBox(-0.6F, -5.0857F, -11.325F, 1.2F, 2.7F, 10.825F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-17.0F, 38.0F, 22.0F, 0.2705F, -2.1588F, 0.0F));

		PartDefinition body_seg_13 = body.addOrReplaceChild("body_seg_13", CubeListBuilder.create().texOffs(70, 119).addBox(-2.4571F, -2.4571F, -10.847F, 4.9143F, 4.9143F, 10.847F, new CubeDeformation(0.0F))
		.texOffs(60, 228).addBox(-1.8429F, 1.4743F, -10.747F, 3.6857F, 1.3829F, 10.647F, new CubeDeformation(0.0F))
		.texOffs(91, 228).addBox(-0.6F, -4.9571F, -10.347F, 1.2F, 2.7F, 9.847F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-8.0F, 41.0F, 28.0F, 0.1964F, -1.4711F, 0.0F));

		PartDefinition body_seg_14 = body.addOrReplaceChild("body_seg_14", CubeListBuilder.create().texOffs(0, 101).addBox(-2.3286F, -2.3286F, -11.6F, 4.6571F, 4.6571F, 11.6F, new CubeDeformation(0.0F))
		.texOffs(60, 199).addBox(-1.7464F, 1.3971F, -11.5F, 3.4929F, 1.3314F, 11.4F, new CubeDeformation(0.0F))
		.texOffs(93, 199).addBox(-0.6F, -4.8286F, -11.1F, 1.2F, 2.7F, 10.6F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.0F, 43.0F, 27.0F, 0.1828F, -0.9828F, 0.0F));

		PartDefinition body_seg_15 = body.addOrReplaceChild("body_seg_15", CubeListBuilder.create().texOffs(0, 136).addBox(-2.2F, -2.2F, -10.9441F, 4.4F, 4.4F, 10.9441F, new CubeDeformation(0.0F))
		.texOffs(0, 242).addBox(-1.65F, 1.32F, -10.8441F, 3.3F, 1.28F, 10.7441F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(11.0F, 45.0F, 21.0F, 0.0968F, -0.5071F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 512);
	}

    @Override
    public void setupAnim(ShenlongEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float time = ageInTicks * 0.05f;

        // Subtle undulating breath/motion for head and jaw
        this.head.yRot = Mth.sin(time * 0.8f) * 0.05f;
        this.head.xRot = Mth.cos(time * 0.6f) * 0.04f;
        this.jaw.xRot = 0.384f + Mth.sin(time * 1.2f) * 0.04f; // Roaring breath

        // Whisker floating flutter in divine wind
        this.whiskerLeftBase.zRot = -0.2618f + Mth.sin(time * 1.5f) * 0.08f;
        this.whiskerRightBase.zRot = 0.2618f - Mth.sin(time * 1.5f) * 0.08f;

        // Claw floating motion
        this.armLeft.xRot = -0.2618f + Mth.sin(time + 1.0f) * 0.06f;
        this.armRight.xRot = -0.2618f + Mth.cos(time + 1.0f) * 0.06f;

        // Serpentine body undulation through all 15 segments
        for (int i = 0; i < 15; i++) {
            float wave = Mth.sin(time * 1.2f + i * 0.35f) * 0.05f;
            this.bodySegments[i].zRot = wave;
        }
    }
}
