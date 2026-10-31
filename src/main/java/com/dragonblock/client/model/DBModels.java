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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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
    public static final ModelLayerLocation APE_HEAD = layer("ape_head");

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
        event.registerLayerDefinition(APE_HEAD, DBModels::apeHead);
        event.registerLayerDefinition(FUR, () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.45f), 0), 64, 64));
    }

    // ------------------------------------------------------------------ hair

    /** Four tapering steps (fraction of length, fraction of width); the outer two follow the bend. */
    private static final float[][] TIERS = {{0.38f, 1.0f}, {0.28f, 0.8f}, {0.2f, 0.58f}, {0.14f, 0.34f}};

    /**
     * Hair is data-driven: assets/dragonblock/hair/<style>.json holds a volume block ("mass") and spikes.
     * The layout must match tools/preview_hair.py, which renders the same files in Blender for review.
     */
    private static LayerDefinition hair(HairStyle style) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.ZERO);
        CubeListBuilder cap = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.4f, -8.8f, -4.4f, 8.8f, 3.4f, 8.8f)
                .texOffs(0, 12).addBox(-4.4f, -6.2f, 0.6f, 8.8f, 5.4f, 3.8f)
                .texOffs(26, 12).addBox(-4.45f, -6.0f, -3.6f, 0.9f, 2.6f, 4.4f)
                .texOffs(26, 12).addBox(3.55f, -6.0f, -3.6f, 0.9f, 2.6f, 4.4f);

        JsonObject json = readHair(style);
        if (json.has("mass")) {
            JsonArray m = json.getAsJsonArray("mass");
            cap.texOffs(0, 40).addBox(m.get(0).getAsFloat(), m.get(1).getAsFloat(), m.get(2).getAsFloat(),
                    m.get(3).getAsFloat(), m.get(4).getAsFloat(), m.get(5).getAsFloat());
        }
        root.addOrReplaceChild("cap", cap, PartPose.ZERO);

        int index = 0;
        for (JsonElement e : json.getAsJsonArray("spikes")) {
            JsonObject sp = e.getAsJsonObject();
            spike(root, "spike" + index++, sp, false);
            if (sp.has("mirror") && sp.get("mirror").getAsBoolean()) spike(root, "spike" + index++, sp, true);
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static JsonObject readHair(HairStyle style) {
        return readJson("/assets/" + DragonBlockMod.MODID + "/hair/" + style.name().toLowerCase() + ".json");
    }

    private static JsonObject readJson(String path) {
        try (InputStream in = DBModels.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Missing model file " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /** One spike: a root part with the two inner steps, and a bent child with the two outer steps. */
    private static void spike(PartDefinition root, String name, JsonObject sp, boolean mirror) {
        JsonArray p = sp.getAsJsonArray("p");
        float x = p.get(0).getAsFloat(), y = p.get(1).getAsFloat(), z = p.get(2).getAsFloat();
        float side = sp.get("side").getAsFloat();
        if (mirror) {
            x = -x;
            side = -side;
        }
        float w = sp.get("w").getAsFloat();
        float d = sp.has("d") ? sp.get("d").getAsFloat() : w;
        float len = sp.get("len").getAsFloat();
        float bend = sp.has("bend") ? sp.get("bend").getAsFloat() : 0f;

        CubeListBuilder inner = CubeListBuilder.create().texOffs(0, 24);
        CubeListBuilder outer = CubeListBuilder.create().texOffs(28, 24);
        float start = 0f, innerLength = 0f;
        for (int i = 0; i < TIERS.length; i++) {
            if (i == 2) {
                innerLength = start;
                start = 0f;
            }
            float l = len * TIERS[i][0], tw = w * TIERS[i][1], td = d * TIERS[i][1];
            (i < 2 ? inner : outer).addBox(-tw / 2, -start - l - 0.15f, -td / 2, tw, l + 0.3f, td);
            start += l;
        }
        PartDefinition base = root.addOrReplaceChild(name, inner,
                PartPose.offsetAndRotation(x, y, z, -sp.get("back").getAsFloat() * Mth.DEG_TO_RAD, 0f, side * Mth.DEG_TO_RAD));
        base.addOrReplaceChild("tip", outer, PartPose.offsetAndRotation(0f, -innerLength, 0f, -bend * Mth.DEG_TO_RAD, 0f, 0f));
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

    /**
     * Giant-ape head over the player's head, read from assets/dragonblock/ape_head.json (preview it with
     * tools/preview_ape.py). Each group becomes a child part so the renderer can tint it separately.
     */
    private static LayerDefinition apeHead() {
        JsonObject json = readJson("/assets/" + DragonBlockMod.MODID + "/ape_head.json");
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        int v = 0;
        for (String group : new String[]{"fur", "skin", "dark", "fangs", "eyes"}) {
            CubeListBuilder cubes = CubeListBuilder.create().texOffs(0, v);
            for (JsonElement e : json.getAsJsonArray(group)) {
                JsonArray b = e.getAsJsonArray();
                cubes.addBox(b.get(0).getAsFloat(), b.get(1).getAsFloat(), b.get(2).getAsFloat(),
                        b.get(3).getAsFloat(), b.get(4).getAsFloat(), b.get(5).getAsFloat());
            }
            root.addOrReplaceChild(group, cubes, PartPose.ZERO);
            v += 6;
        }
        return LayerDefinition.create(mesh, 64, 32);
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
