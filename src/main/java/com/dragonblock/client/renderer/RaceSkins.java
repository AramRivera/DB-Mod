package com.dragonblock.client.renderer;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Race;
import com.dragonblock.common.data.Transformation;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * Race bodies instead of Minecraft skins. Each player's texture is composed from the race's tint layers
 * (textures/skin, made by tools/gen_race_skins.py) and the colors picked in the character creator, then
 * swapped into the player's tab-list entry so every renderer (world, first-person hand, inventory, tab
 * list) uses it. Textures are cached per look, so a server of Saiyans in the same colors shares one.
 */
public final class RaceSkins {
    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();
    private static final Map<String, NativeImage> LAYERS = new HashMap<>();
    private static final Set<PlayerInfo> PATCHED = Collections.newSetFromMap(new WeakHashMap<>());
    private static Field skinLookup;
    private static boolean broken = false;

    private RaceSkins() {}

    private static String template(Race race) {
        return switch (race) {
            case SAIYAN -> "saiyan";
            case NAMEKIAN -> "namekian";
            case ARCOSIAN -> "arcosian";
            case MAJIN -> "majin";
            default -> "human";
        };
    }

    public static ResourceLocation texture(PlayerDBCData d) {
        boolean hair = d.getHairStyle() != Transformation.HairStyle.NONE;
        String key = template(d.getRace()) + "_" + Integer.toHexString(d.getSkinColor()) + "_" + Integer.toHexString(d.getAccentColor())
                + "_" + Integer.toHexString(d.getHairColor()) + "_" + Integer.toHexString(d.getEyeColor()) + (hair ? "_h" : "");
        return TEXTURES.computeIfAbsent(key, k -> {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "race_skin/" + k);
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(compose(d, hair)));
            return id;
        });
    }

    private static NativeImage compose(PlayerDBCData d, boolean hair) {
        String t = template(d.getRace());
        NativeImage out = new NativeImage(64, 64, true);
        tint(out, layer(t + "_base"), d.getSkinColor(), true);
        if (hair) tint(out, layer(t + "_scalp"), d.getHairColor(), true);
        tint(out, layer(t + "_accent"), d.getAccentColor(), true);
        over(out, layer(t + "_detail"));
        tint(out, layer(t + "_brows"), d.getHairColor(), true);
        tint(out, layer("eyes"), d.getEyeColor(), false);
        return out;
    }

    /** Multiplies a grayscale layer by {@code rgb} and alpha-blends it over {@code out}; must match the tool's preview. */
    private static void tint(NativeImage out, NativeImage layer, int rgb, boolean sheen) {
        if (layer == null) return;
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int abgr = layer.getPixelRGBA(x, y);
                int a = abgr >>> 24;
                if (a == 0) continue;
                int v = abgr & 255;
                float add = sheen ? Math.max(0, v - 200) / 55f * 36f : 0f;
                blend(out, x, y, Math.min(255, (int) (r * v / 255f + add)), Math.min(255, (int) (g * v / 255f + add)),
                        Math.min(255, (int) (b * v / 255f + add)), a);
            }
        }
    }

    private static void over(NativeImage out, NativeImage layer) {
        if (layer == null) return;
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int abgr = layer.getPixelRGBA(x, y);
                int a = abgr >>> 24;
                if (a != 0) blend(out, x, y, abgr & 255, abgr >> 8 & 255, abgr >> 16 & 255, a);
            }
        }
    }

    private static void blend(NativeImage out, int x, int y, int r, int g, int b, int a) {
        int dst = out.getPixelRGBA(x, y);
        int da = dst >>> 24;
        if (a == 255 || da == 0) {
            out.setPixelRGBA(x, y, a << 24 | b << 16 | g << 8 | r);
            return;
        }
        float k = a / 255f;
        int nr = (int) (r * k + (dst & 255) * (1 - k));
        int ng = (int) (g * k + (dst >> 8 & 255) * (1 - k));
        int nb = (int) (b * k + (dst >> 16 & 255) * (1 - k));
        out.setPixelRGBA(x, y, Math.max(a, da) << 24 | nb << 16 | ng << 8 | nr);
    }

    private static NativeImage layer(String name) {
        return LAYERS.computeIfAbsent(name, n -> {
            var id = ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "textures/skin/" + n + ".png");
            var res = Minecraft.getInstance().getResourceManager().getResource(id);
            if (res.isEmpty()) return null;
            try (InputStream in = res.get().open()) {
                return NativeImage.read(in);
            } catch (Exception e) {
                DragonBlockMod.LOGGER.error("Could not read race skin layer {}", id, e);
                return null;
            }
        });
    }

    /**
     * Points every visible player's tab-list entry at their race body. The original skin lookup is kept
     * and used until that player's data has synced. Looked up by type (the only Supplier field) so it
     * works in dev and production names alike.
     */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getConnection() == null || broken) return;
        for (Player player : mc.level.players()) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
            if (info == null || PATCHED.contains(info)) continue;
            patch(info, player.getUUID());
        }
    }

    @SuppressWarnings("unchecked")
    private static void patch(PlayerInfo info, java.util.UUID uuid) {
        try {
            if (skinLookup == null) {
                for (Field f : PlayerInfo.class.getDeclaredFields()) {
                    if (f.getType() == Supplier.class) {
                        f.setAccessible(true);
                        skinLookup = f;
                    }
                }
                if (skinLookup == null) throw new IllegalStateException("PlayerInfo has no skin supplier");
            }
            Supplier<PlayerSkin> original = (Supplier<PlayerSkin>) skinLookup.get(info);
            skinLookup.set(info, (Supplier<PlayerSkin>) () -> {
                PlayerSkin vanilla = original.get();
                PlayerDBCData data = PlayerDBCData.getClient(uuid);
                if (data == null) return vanilla;
                return new PlayerSkin(texture(data), null, vanilla.capeTexture(), vanilla.elytraTexture(), PlayerSkin.Model.WIDE, true);
            });
            PATCHED.add(info);
        } catch (ReflectiveOperationException | RuntimeException e) {
            broken = true;
            DragonBlockMod.LOGGER.error("Could not install race bodies, players keep their own skins", e);
        }
    }
}
