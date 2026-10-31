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

    public static final KeyMapping REVERT_KEY = key("revert", GLFW.GLFW_KEY_H);
    public static final KeyMapping KI_WAVE_KEY = key("ki_wave", GLFW.GLFW_KEY_Z);
    public static final KeyMapping DASH_KEY = key("dash", GLFW.GLFW_KEY_R);
    public static final KeyMapping GUARD_KEY = key("guard", GLFW.GLFW_KEY_B);

    public static final KeyMapping[] ALL = {
            CHARGE_KI_KEY, TRANSFORM_KEY, CHARACTER_SHEET_KEY, FLY_KEY, KI_BLAST_KEY,
            REVERT_KEY, KI_WAVE_KEY, DASH_KEY, GUARD_KEY
    };

    private static KeyMapping key(String name, int glfwKey) {
        return new KeyMapping("key.dragonblock." + name, KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, glfwKey, KEY_CATEGORY);
    }
}
