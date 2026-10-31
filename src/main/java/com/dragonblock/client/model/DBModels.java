package com.dragonblock.client.model;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.data.Transformation.HairStyle;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.EntityRenderersEvent;

import java.util.EnumMap;
import java.util.Map;

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
     * Hair = a cap hugging the skull plus chunky stepped spikes. Spikes are laid out by hand per style
     * (mirrored left/right) so each reads as one hairstyle. Angles: {@code back} > 0 sweeps a spike toward
     * the back of the head (PI/2 = straight back, PI = straight down), {@code side} > 0 tilts it outward.
     */
    private static LayerDefinition hair(HairStyle style) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("cap", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.4f, -8.8f, -4.4f, 8.8f, 3.4f, 8.8f)
                .texOffs(0, 12).addBox(-4.4f, -6.2f, 0.6f, 8.8f, 5.4f, 3.8f)
                .texOffs(26, 12).addBox(-4.45f, -6.0f, -3.6f, 0.9f, 2.6f, 4.4f)
                .texOffs(26, 12).addBox(3.55f, -6.0f, -3.6f, 0.9f, 2.6f, 4.4f), PartPose.ZERO);

        Spikes h = new Spikes(root);
        switch (style) {
            case FLARE -> { // upswept golden-tier style
                h.add(0f, -8.4f, -1.6f, 0.35f, 0f, 3.8f, 10f);
                h.add(0f, -8.4f, 1.6f, 0.75f, 0f, 3.8f, 10.5f);
                h.mirror(2.5f, -8.2f, -1.0f, 0.3f, 0.35f, 3.4f, 9f);
                h.mirror(2.5f, -8.0f, 2.0f, 0.85f, 0.3f, 3.4f, 9.5f);
                h.mirror(3.9f, -6.6f, 0.2f, 0.25f, 1.0f, 3.0f, 7f);
                h.add(0f, -4.2f, 3.8f, 1.95f, 0f, 3.4f, 6.5f);
                h.mirror(2.6f, -4.4f, 3.6f, 1.85f, 0.35f, 3.0f, 6f);
                bangs(h, 3, 4.5f);
            }
            case RIDGE -> { // taller, sharper, more vertical
                h.add(0f, -8.4f, -1.2f, 0.12f, 0f, 3.4f, 13f);
                h.add(0f, -8.4f, 1.8f, 0.45f, 0f, 3.4f, 12.5f);
                h.mirror(2.4f, -8.2f, -0.8f, 0.1f, 0.25f, 3.0f, 11.5f);
                h.mirror(2.5f, -8.0f, 2.2f, 0.55f, 0.22f, 3.0f, 11.5f);
                h.mirror(3.9f, -6.6f, 0.0f, 0.15f, 0.8f, 2.8f, 8.5f);
                h.add(0f, -4.0f, 3.8f, 1.8f, 0f, 3.2f, 7f);
                h.mirror(2.6f, -4.2f, 3.6f, 1.7f, 0.3f, 2.8f, 6.5f);
                h.add(-1.2f, -7.8f, -4.0f, Mth.PI - 0.35f, -0.15f, 2.4f, 6.5f); // single loose bang
            }
            case MANE -> { // waist-length mane
                h.add(0f, -8.4f, -1.0f, 0.5f, 0f, 3.6f, 7f);
                h.mirror(2.4f, -8.2f, -0.6f, 0.45f, 0.4f, 3.2f, 6.5f);
                h.mirror(1.2f, -5.2f, 3.9f, Mth.PI - 0.22f, 0.06f, 4.2f, 30f);
                h.mirror(3.2f, -5.0f, 3.6f, Mth.PI - 0.25f, 0.16f, 3.8f, 26f);
                h.add(0f, -7.4f, 3.4f, Mth.PI - 0.3f, 0f, 4.2f, 28f);
                h.mirror(4.2f, -4.2f, 1.2f, Mth.PI - 0.18f, 0.22f, 3.2f, 18f);
                bangs(h, 2, 5f);
            }
            case WILD -> { // untamed dark style with a long back
                for (int i = 0; i < 5; i++) {
                    float a = -0.9f + i * 0.45f;
                    h.add(Mth.sin(a) * 3f, -8.3f, -1.5f + Mth.cos(a) * 1.5f, 0.45f + Math.abs(a) * 0.3f, a * 0.9f, 3.4f, 8f + (i % 2) * 1.5f);
                }
                h.mirror(3.9f, -6.4f, 0.8f, 0.6f, 1.15f, 3.2f, 8f);
                h.add(0f, -5.0f, 3.9f, 2.55f, 0f, 3.8f, 15f);
                h.mirror(2.2f, -5.0f, 3.8f, 2.45f, 0.25f, 3.6f, 13.5f);
                h.mirror(3.8f, -4.0f, 3.0f, 2.35f, 0.45f, 3.0f, 11f);
                bangs(h, 3, 4f);
            }
            case SLEEK -> { // slicked back, flatter spikes
                h.add(0f, -8.6f, -2.2f, 1.25f, 0f, 3.6f, 8f);
                h.mirror(2.3f, -8.4f, -2.0f, 1.2f, 0.2f, 3.2f, 7.5f);
                h.add(0f, -8.0f, 1.0f, 1.45f, 0f, 3.6f, 7.5f);
                h.mirror(2.4f, -7.8f, 1.2f, 1.4f, 0.25f, 3.2f, 7f);
                h.mirror(3.9f, -6.4f, 0.4f, 1.0f, 0.55f, 2.8f, 5.5f);
                bangs(h, 2, 3.5f);
            }
            case SPIRE -> { // tall central crest
                h.add(0f, -8.6f, 0f, 0.05f, 0f, 4.2f, 17f);
                h.mirror(2.2f, -8.4f, -0.4f, 0.15f, 0.25f, 3.4f, 12.5f);
                h.mirror(2.4f, -8.2f, 2.2f, 0.55f, 0.3f, 3.2f, 10f);
                h.mirror(3.9f, -6.6f, 0.2f, 0.2f, 0.9f, 2.8f, 7.5f);
                h.add(0f, -4.2f, 3.8f, 1.9f, 0f, 3.4f, 7f);
                bangs(h, 1, 5f);
            }
            default -> {}
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Strands falling over the forehead. */
    private static void bangs(Spikes h, int count, float length) {
        float[][] layouts = {{0f}, {-1.6f, 1.4f}, {-2.0f, 0.1f, 1.9f}};
        for (float x : layouts[count - 1]) {
            h.add(x, -7.8f, -4.0f, Mth.PI - 0.4f, x * -0.06f, 2.6f, length - Math.abs(x) * 0.3f);
        }
    }

    /** Builds three-step tapered spikes (root, middle, tip) pointing along local -Y. */
    private static final class Spikes {
        private final PartDefinition root;
        private int index;

        Spikes(PartDefinition root) { this.root = root; }

        void mirror(float x, float y, float z, float back, float side, float width, float length) {
            add(x, y, z, back, side, width, length);
            add(-x, y, z, back, -side, width, length);
        }

        void add(float x, float y, float z, float back, float side, float width, float length) {
            float l1 = length * 0.45f, l2 = length * 0.33f, l3 = length - l1 - l2;
            float w2 = width * 0.66f, w3 = width * 0.34f;
            root.addOrReplaceChild("spike" + index++, CubeListBuilder.create()
                            .texOffs(0, 24).addBox(-width / 2, -l1, -width / 2, width, l1, width)
                            .texOffs(18, 24).addBox(-w2 / 2, -l1 - l2 - 0.2f, -w2 / 2, w2, l2 + 0.4f, w2)
                            .texOffs(32, 24).addBox(-w3 / 2, -length, -w3 / 2, w3, l3 + 0.3f, w3),
                    PartPose.offsetAndRotation(x, y, z, -back, 0f, side));
        }
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
                    PartPose.offsetAndRotation(side * 4.2f, -6.5f, 0f, 0f, side * 1.57f, 0f)); // +Z box turned to point outward
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
                    PartPose.offsetAndRotation(0f, i == 0 ? 0f : -3.2f, 0f, -0.45f, 0f, 0f)); // curls toward the back
            t *= 0.78f;
        }
        return LayerDefinition.create(mesh, 32, 32);
    }
}
