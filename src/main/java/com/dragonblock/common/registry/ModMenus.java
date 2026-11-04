package com.dragonblock.common.registry;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.menu.AccessoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, DragonBlockMod.MODID);

    public static final RegistryObject<MenuType<AccessoryMenu>> ACCESSORIES =
            MENUS.register("accessories", () -> IForgeMenuType.create((id, inventory, buf) -> new AccessoryMenu(id, inventory)));
}
