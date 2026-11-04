package com.dragonblock.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Flat dark button matching the mod's UI instead of the vanilla stone texture. */
public class FlatButton extends Button {
    private final int accent;

    public FlatButton(int x, int y, int w, int h, Component label, int accent, OnPress onPress) {
        super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
        this.accent = accent;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean hover = isHoveredOrFocused() && active;
        int bg = !active ? UiTheme.BUTTON_DISABLED : hover ? UiTheme.BUTTON_HOVER : UiTheme.BUTTON;
        g.fill(getX(), getY(), getX() + width, getY() + height, bg);
        g.renderOutline(getX(), getY(), width, height, hover ? 0xFF000000 | accent : UiTheme.BORDER);
        int textColor = active ? (hover ? 0xFFFFFF : UiTheme.TEXT) : UiTheme.TEXT_DIM;
        var font = Minecraft.getInstance().font;
        g.drawString(font, getMessage(), getX() + (width - font.width(getMessage())) / 2,
                getY() + (height - 8) / 2, textColor, false);
    }
}
