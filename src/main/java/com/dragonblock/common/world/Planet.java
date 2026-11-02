package com.dragonblock.common.world;

import com.dragonblock.DragonBlockMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/** The planets a ship can fly to, and where on each one it sets down. */
public enum Planet {
    EARTH("Tierra", "Tu planeta natal: mares azules, montañas y gravedad normal.", Level.OVERWORLD),
    NAMEK("Namek", "Verde y tranquilo, de aguas esmeralda y tres soles: allí nunca anochece.", ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, "namek"))),
    CELESTIAL("Planeta Celeste", "Diminuto y denso, flotando en el cielo violeta. Gravedad aplastante: se entrena el doble.", Worlds.CELESTIAL);

    private final String displayName, description;
    private final ResourceKey<Level> dimension;

    Planet(String displayName, String description, ResourceKey<Level> dimension) {
        this.displayName = displayName;
        this.description = description;
        this.dimension = dimension;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public ResourceKey<Level> getDimension() { return dimension; }

    public static Planet of(ResourceKey<Level> dimension) {
        for (Planet p : values()) if (p.dimension == dimension) return p;
        return null;
    }

    /** Ground to land on: the Earth's spawn, the celestial landing pad, or the nearest dry land to Namek's center. */
    public BlockPos landing(ServerLevel level) {
        return switch (this) {
            case EARTH -> surface(level, level.getSharedSpawnPos().getX() + 6, level.getSharedSpawnPos().getZ() - 6);
            case CELESTIAL -> {
                Worlds.ensureBuilt(level);
                yield BlockPos.containing(Worlds.CELESTIAL_ARRIVAL).offset(0, 0, 3);
            }
            case NAMEK -> dryLand(level);
        };
    }

    private static BlockPos surface(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    /** Spirals out from the center in 24-block steps until it finds ground that isn't under water. */
    private static BlockPos dryLand(ServerLevel level) {
        for (int ring = 0; ring <= 12; ring++) {
            for (int i = -ring; i <= ring; i++) {
                for (int[] c : new int[][]{{i, -ring}, {i, ring}, {-ring, i}, {ring, i}}) {
                    BlockPos p = surface(level, c[0] * 24, c[1] * 24);
                    if (level.getFluidState(p.below()).isEmpty() && level.getFluidState(p).isEmpty()) return p;
                }
            }
        }
        return surface(level, 0, 0);
    }
}
