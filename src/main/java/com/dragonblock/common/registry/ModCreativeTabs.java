package com.dragonblock.common.registry;

import com.dragonblock.DragonBlockMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DragonBlockMod.MODID);

    public static final RegistryObject<CreativeModeTab> DRAGON_BLOCK_TAB =
            CREATIVE_MODE_TABS.register("dragon_block_tab", () -> CreativeModeTab.builder()
                    .title(Component.literal("Dragon Block"))
                    .icon(() -> ModItems.DRAGON_BALL_4.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.SENZU_BEAN.get());
                        output.accept(ModItems.SCOUTER.get());
                        output.accept(ModItems.DRAGON_RADAR.get());
                        output.accept(ModItems.TRAINING_WEIGHTS.get());
                        output.accept(ModItems.WRIST_WEIGHTS.get());
                        output.accept(ModItems.COMBAT_BELT.get());
                        output.accept(ModItems.KI_BLAST_ITEM.get());

                        output.accept(ModItems.DRAGON_BALL_1.get());
                        output.accept(ModItems.DRAGON_BALL_2.get());
                        output.accept(ModItems.DRAGON_BALL_3.get());
                        output.accept(ModItems.DRAGON_BALL_4.get());
                        output.accept(ModItems.DRAGON_BALL_5.get());
                        output.accept(ModItems.DRAGON_BALL_6.get());
                        output.accept(ModItems.DRAGON_BALL_7.get());
                    })
                    .build());
}
