package com.dragonblock.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class ModKeyBindings {
    public static final String KEY_CATEGORY = "key.categories.dragonblock";

    public static final KeyMapping CHARGE_KI_KEY = new KeyMapping(
            "key.dragonblock.charge_ki",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            KEY_CATEGORY
    );

    public static final KeyMapping TRANSFORM_KEY = new KeyMapping(
            "key.dragonblock.transform",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            KEY_CATEGORY
    );

    public static final KeyMapping CHARACTER_SHEET_KEY = new KeyMapping(
            "key.dragonblock.character_sheet",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KEY_CATEGORY
    );

    public static final KeyMapping FLY_KEY = new KeyMapping(
            "key.dragonblock.fly",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            KEY_CATEGORY
    );

    public static final KeyMapping KI_BLAST_KEY = new KeyMapping(
            "key.dragonblock.ki_blast",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            KEY_CATEGORY
    );
}
