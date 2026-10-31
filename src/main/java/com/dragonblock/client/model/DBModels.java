package com.dragonblock.client.model;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.data.Transformation.HairStyle;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Code-defined models attached to the player's model parts. All coordinates are in model pixels,
 * relative to the part they follow (head pivot at the neck, body pivot at the neck).
 */
public final class DBModels {
    public static final Map<HairStyle, ModelLayerLocation> HAIR = new EnumMap<>(HairStyle.class);
    public static final ModelLayerLocation TAIL = layer("tail");
    public static final ModelLayerLocation TAIL_WRAPPED = layer("tail_wrapped");
    public static final ModelLayerLocation THICK_TAIL = layer("thick_tail");
    public static final ModelLayerLocation ANTENNAE = layer("antennae");
    public static final ModelLayerLocation HORNS = layer("horns");
    public static final ModelLayerLocation TENDRIL = layer("tendril");
    public static final ModelLayerLocation FUR = layer("fur");

    /** Segments in a tail chain, named seg0..segN-1, each a child of the previous. */
    public static final int TAIL_SEGMENTS = 7;
    public static final int TENDRIL_SEGMENTS = 4;

    static {
        for (HairStyle style : HairStyle.values()) {
            if (style != HairStyle.NONE) HAIR.put(style, layer("hair_" + style.name().toLowerCase()));
        }
    }

    private DBModels() {}

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, name), "main");
    }

    public static void register(EntityRenderersEvent.RegisterLayerDefinitions event) {
        HAIR.forEach((style, loc) -> event.registerLayerDefinition(loc, () -> hair(style)));
        event.registerLayerDefinition(TAIL, () -> tail(1.6f, 3.2f, 0.92f));
        event.registerLayerDefinition(THICK_TAIL, () -> tail(2.6f, 3.6f, 0.86f));
        event.registerLayerDefinition(TAIL_WRAPPED, DBModels::wrappedTail);
        event.registerLayerDefinition(ANTENNAE, DBModels::antennae);
        event.registerLayerDefinition(HORNS, DBModels::horns);
        event.registerLayerDefinition(TENDRIL, DBModels::tendril);
        event.registerLayerDefinition(FUR, () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.45f), 0), 64, 64));
    }

    // ------------------------------------------------------------------ hair

    /**
     * Hair is a cap over the skull plus tapered spikes. Each spike is two boxes (wide root, thin tip)
     * pointing along local -Y, then rotated into place. A seeded RNG keeps every style stable but organic.
     */
    private static LayerDefinition hair(HairStyle style) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.3f, -8.6f, -4.3f, 8.6f, 3.2f, 8.6f)
                .texOffs(0, 12).addBox(-4.3f, -6.0f, 0.5f, 8.6f, 5.0f, 3.8f), PartPose.ZERO);

        Random rnd = new Random(style.ordinal() * 7919L);
        int[] index = {0};
        switch (style) {
            case FLARE -> {
                crown(root, rnd, index, 11, 7f, 10f, -0.55f, 0.9f, 2.4f);
                back(root, rnd, index, 6, 5f, 8f, 1.9f);
                fringe(root, rnd, index, 3, 4f, 5.5f);
            }
            case RIDGE -> {
                crown(root, rnd, index, 13, 8f, 12f, -0.35f, 0.7f, 2.0f);
                back(root, rnd, index, 6, 6f, 9f, 2.0f);
                fringe(root, rnd, index, 1, 6f, 6f);
            }
            case MANE -> {
                crown(root, rnd, index, 8, 6f, 8f, -0.4f, 0.8f, 2.6f);
                back(root, rnd, index, 14, 20f, 30f, 2.8f);
                fringe(root, rnd, index, 2, 4f, 5f);
            }
            case WILD -> {
                crown(root, rnd, index, 12, 5f, 9f, -0.9f, 1.4f, 2.4f);
                back(root, rnd, index, 10, 10f, 16f, 2.4f);
                fringe(root, rnd, index, 4, 3f, 5f);
            }
            case SLEEK -> {
                crown(root, rnd, index, 10, 5f, 7f, -1.25f, 0.5f, 2.2f);
                back(root, rnd, index, 5, 4f, 6f, 1.7f);
                fringe(root, rnd, index, 2, 3f, 4f);
            }
            case SPIRE -> {
                spike(root, index, 0f, -8f, 0.5f, -0.15f, 0f, 3.0f, 16f);
                crown(root, rnd, index, 10, 7f, 11f, -0.3f, 0.6f, 2.2f);
                back(root, rnd, index, 6, 7f, 11f, 2.0f);
                fringe(root, rnd, index, 1, 5f, 5f);
            }
            default -> {}
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Spikes rising from the top of the skull, fanned around the head. */
    private static void crown(PartDefinition root, Random rnd, int[] index, int count, float minLen, float maxLen,
                              float tiltBack, float spread, float width) {
        for (int i = 0; i < count; i++) {
            float a = (float) (Math.PI * 2 * i / count + rnd.nextFloat() * 0.4f);
            float x = (float) Math.sin(a) * 2.6f;
            float z = (float) Math.cos(a) * 2.6f;
            float xRot = tiltBack + (float) Math.cos(a) * -spread * 0.6f + (rnd.nextFloat() - 0.5f) * 0.3f;
            float zRot = (float) Math.sin(a) * -spread + (rnd.nextFloat() - 0.5f) * 0.3f;
            spike(root, index, x, -8.2f, z, xRot, zRot, width, minLen + rnd.nextFloat() * (maxLen - minLen));
        }
    }

    /** Spikes from the back of the head; {@code angle} ~1.6 points backwards, ~3 straight down the spine. */
    private static void back(PartDefinition root, Random rnd, int[] index, int count, float minLen, float maxLen, float angle) {
        for (int i = 0; i < count; i++) {
            float x = -3.4f + 6.8f * i / Math.max(1, count - 1);
            float y = -7.0f + rnd.nextFloat() * 4f;
            float zRot = x * -0.06f + (rnd.nextFloat() - 0.5f) * 0.3f;
            spike(root, index, x, y, 3.8f, angle + (rnd.nextFloat() - 0.5f) * 0.3f, zRot, 2.6f,
                    minLen + rnd.nextFloat() * (maxLen - minLen));
        }
    }

    /** Strands over the forehead, angled down and slightly out. */
    private static void fringe(PartDefinition root, Random rnd, int[] index, int count, float minLen, float maxLen) {
        for (int i = 0; i < count; i++) {
            float x = count == 1 ? 1.2f : -2.4f + 4.8f * i / (count - 1);
            spike(root, index, x, -7.6f, -3.9f, -2.5f + (rnd.nextFloat() - 0.5f) * 0.3f, x * 0.08f, 1.6f,
                    minLen + rnd.nextFloat() * (maxLen - minLen));
        }
    }

    private static void spike(PartDefinition root, int[] index, float x, float y, float z, float xRot, float zRot,
                              float width, float length) {
        float root1 = length * 0.55f;
        float tipW = width * 0.45f;
        root.addOrReplaceChild("spike" + index[0]++, CubeListBuilder.create()
                        .texOffs(0, 24).addBox(-width / 2, -root1, -width / 2, width, root1, width)
                        .texOffs(16, 24).addBox(-tipW / 2, -length, -tipW / 2, tipW, length - root1 + 0.2f, tipW),
                PartPose.offsetAndRotation(x, y, z, xRot, 0f, zRot));
    }

    // ------------------------------------------------------------------ body features

    private static LayerDefinition tail(float thickness, float segLength, float taper) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parent = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(),
                PartPose.offset(0f, 11f, 2f));
        float t = thickness;
        for (int i = 0; i < TAIL_SEGMENTS; i++) {
            parent = parent.addOrReplaceChild("seg" + i, CubeListBuilder.create().texOffs(0, 0)
                            .addBox(-t / 2, -t / 2, 0f, t, t, segLength + 0.3f),
                    PartPose.offset(0f, 0f, i == 0 ? 0f : segLength));
            t *= taper;
        }
        return LayerDefinition.create(mesh, 32, 32);
    }

    /** Tail wound around the waist like a belt. */
    private static LayerDefinition wrappedTail() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("belt", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4.6f, 9.6f, -2.6f, 9.2f, 1.8f, 1.2f)
                .addBox(-4.6f, 9.6f, 1.4f, 9.2f, 1.8f, 1.2f)
                .addBox(-4.6f, 9.6f, -2.6f, 1.2f, 1.8f, 5.2f)
                .addBox(3.4f, 9.6f, -2.6f, 1.2f, 1.8f, 5.2f)
                .addBox(2.6f, 10.6f, -2.9f, 1.6f, 1.6f, 0.8f), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    private static LayerDefinition antennae() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int side = -1; side <= 1; side += 2) {
            PartDefinition base = root.addOrReplaceChild("stalk" + (side < 0 ? "L" : "R"), CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-0.5f, -4f, -0.5f, 1f, 4f, 1f),
                    PartPose.offsetAndRotation(side * 1.6f, -8f, -3f, -0.35f, 0f, side * 0.25f));
            base.addOrReplaceChild("upper", CubeListBuilder.create()
                            .texOffs(4, 0).addBox(-0.45f, -3.5f, -0.45f, 0.9f, 3.5f, 0.9f)
                            .texOffs(8, 0).addBox(-0.8f, -4.6f, -0.8f, 1.6f, 1.4f, 1.6f),
                    PartPose.offsetAndRotation(0f, -4f, 0f, 0.9f, 0f, 0f));
        }
        return LayerDefinition.create(mesh, 32, 32);
    }

    private static LayerDefinition horns() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int side = -1; side <= 1; side += 2) {
            PartDefinition base = root.addOrReplaceChild("horn" + (side < 0 ? "L" : "R"), CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-1.1f, -1.1f, -1.1f, 2.2f, 2.2f, 3f),
                    PartPose.offsetAndRotation(side * 4.2f, -6.5f, 0f, 0f, side * -1.57f, 0f));
            PartDefinition mid = base.addOrReplaceChild("mid", CubeListBuilder.create()
                            .texOffs(0, 6).addBox(-0.8f, -3.2f, -0.8f, 1.6f, 3.4f, 1.6f),
                    PartPose.offsetAndRotation(0f, 0f, 2.6f, 0.35f, 0f, 0f));
            mid.addOrReplaceChild("tip", CubeListBuilder.create()
                            .texOffs(8, 6).addBox(-0.5f, -2.6f, -0.5f, 1f, 2.8f, 1f),
                    PartPose.offsetAndRotation(0f, -3.1f, 0f, -0.45f, 0f, 0f));
        }
        return LayerDefinition.create(mesh, 32, 32);
    }

    private static LayerDefinition tendril() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parent = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0f, -8f, 1.5f, -0.3f, 0f, 0f));
        float t = 2.8f;
        for (int i = 0; i < TENDRIL_SEGMENTS; i++) {
            parent = parent.addOrReplaceChild("seg" + i, CubeListBuilder.create().texOffs(0, 0)
                            .addBox(-t / 2, -3.2f, -t / 2, t, 3.4f, t),
                    PartPose.offsetAndRotation(0f, i == 0 ? 0f : -3.2f, 0f, 0.45f, 0f, 0f));
            t *= 0.78f;
        }
        return LayerDefinition.create(mesh, 32, 32);
    }
}
