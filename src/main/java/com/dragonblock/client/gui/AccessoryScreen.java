package com.dragonblock.client.gui;

import com.dragonblock.common.item.AccessoryItem;
import com.dragonblock.common.menu.AccessoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Accessory inventory in the mod's style: the character on the left, the four worn slots on the right. */
public class AccessoryScreen extends AbstractContainerScreen<AccessoryMenu> {
    public AccessoryScreen(AccessoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 186;
        inventoryLabelY = AccessoryMenu.INV_Y - 11;
        titleLabelX = 22;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, UiTheme.PANEL);
        g.renderOutline(x, y, imageWidth, imageHeight, UiTheme.BORDER);
        UiTheme.header(g, x, y, imageWidth, 1f);

        // Character preview with what they wear
        int px = x + 8, py = y + 16, pw = 62, ph = 76;
        g.fill(px, py, px + pw, py + ph, UiTheme.PANEL_ALT);
        g.enableScissor(px, py, px + pw, py + ph);
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, px, py, px + pw, py + ph, 30, 0.0625f, mouseX, mouseY, minecraft.player);
        g.disableScissor();

        // Worn slots with their names
        AccessoryItem.Slot[] kinds = AccessoryItem.Slot.values();
        for (int i = 0; i < AccessoryMenu.SLOTS; i++) {
            int sx = x + AccessoryMenu.SLOT_X, sy = y + AccessoryMenu.SLOT_Y + i * 20;
            g.drawString(font, kinds[i].label(), sx - 6 - font.width(kinds[i].label()), sy + 4, UiTheme.TEXT_DIM, false);
            slotFrame(g, sx, sy, true);
        }
        for (Slot slot : menu.slots) {
            if (slot.index >= AccessoryMenu.SLOTS) slotFrame(g, x + slot.x, y + slot.y, false);
        }
    }

    private static void slotFrame(GuiGraphics g, int x, int y, boolean worn) {
        g.fill(x - 1, y - 1, x + 17, y + 17, worn ? 0xFF000000 | UiTheme.ORANGE : UiTheme.BORDER);
        g.fill(x, y, x + 16, y + 16, 0xFF0A0E1A);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title.getString().toUpperCase(), titleLabelX, 6, UiTheme.TEXT_HEADER, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, UiTheme.TEXT_DIM, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
