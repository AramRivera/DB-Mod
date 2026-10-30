package com.dragonblock.common.registry;

import com.dragonblock.DragonBlockMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, DragonBlockMod.MODID);

    public static final RegistryObject<SoundEvent> KI_CHARGE = registerSound("ki_charge");
    public static final RegistryObject<SoundEvent> KI_BLAST = registerSound("ki_blast");
    public static final RegistryObject<SoundEvent> KI_EXPLODE = registerSound("ki_explode");
    public static final RegistryObject<SoundEvent> TRANSFORM = registerSound("transform");

    private static RegistryObject<SoundEvent> registerSound(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(DragonBlockMod.MODID, name)
        ));
    }
}
