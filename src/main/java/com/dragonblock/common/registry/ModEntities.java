package com.dragonblock.common.registry;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.entity.KiBlastEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DragonBlockMod.MODID);

    public static final RegistryObject<EntityType<KiBlastEntity>> KI_BLAST =
            ENTITY_TYPES.register("ki_blast", () ->
                    EntityType.Builder.<KiBlastEntity>of(KiBlastEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(4)
                            .updateInterval(2)
                            .build("ki_blast")
            );
}
