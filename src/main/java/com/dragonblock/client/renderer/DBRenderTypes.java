package com.dragonblock.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * Additive glow render types that write color only. Vanilla's energySwirl/eyes write depth too, so
 * water (drawn after entities) vanished behind any aura like an x-ray. Extends RenderType only to
 * reach the protected state shards; it is never instantiated.
 */
public final class DBRenderTypes extends RenderType {
    private DBRenderTypes() {
        super("dragonblock_unused", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 0, false, false, () -> {}, () -> {});
    }

    private record Swirl(ResourceLocation texture, float u, float v) {}

    /** Scrolling additive energy (auras, beams, shockwaves). */
    public static RenderType energy(ResourceLocation texture, float u, float v) {
        return create("dragonblock_energy", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
                CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTexturingState(new OffsetTexturingStateShard(u, v))
                        .setTransparencyState(ADDITIVE_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));
    }

    private static final Function<ResourceLocation, RenderType> GLOW = Util.memoize(texture ->
            create("dragonblock_glow", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
                    CompositeState.builder()
                            .setShaderState(RENDERTYPE_EYES_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTransparencyState(ADDITIVE_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(false)));

    /** Plain translucent color that never writes depth: the darkening dome over the sky. */
    public static final RenderType DIM = create("dragonblock_dim", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 1536,
            false, true, CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    /** Full-bright additive glow (ki spheres, bursts). */
    public static RenderType glow(ResourceLocation texture) {
        return GLOW.apply(texture);
    }
}
