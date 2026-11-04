package com.dragonblock.client.gui;

import com.dragonblock.DragonBlockMod;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.network.packet.C2SActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A dragon-ball button on the right edge of the vanilla inventory that opens your accessory slots. It
 * follows the inventory when the recipe book slides it sideways.
 */
@Mod.EventBusSubscriber(modid = DragonBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class InventoryAccessoryButton extends Button {
    private final AbstractContainerScreen<?> screen;

    private InventoryAccessoryButton(AbstractContainerScreen<?> screen) {
        super(0, 0, 20, 20, Component.literal("Accesorios"), b -> {
            Minecraft.getInstance().setScreen(null);
            ModMessages.sendToServer(new C2SActionPacket(C2SActionPacket.Action.OPEN_ACCESSORIES, 0));
        }, DEFAULT_NARRATION);
        this.screen = screen;
        setTooltip(Tooltip.create(Component.literal("Accesorios [K]")));
    }

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen || event.getScreen() instanceof CreativeModeInventoryScreen) {
            event.addListener(new InventoryAccessoryButton((AbstractContainerScreen<?>) event.getScreen()));
        }
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        setX(screen.getGuiLeft() + screen.getXSize() + 2);
        setY(screen.getGuiTop() + 4);
        boolean hover = isHoveredOrFocused();
        g.fill(getX(), getY(), getX() + width, getY() + height, hover ? UiTheme.BUTTON_HOVER : UiTheme.PANEL);
        g.renderOutline(getX(), getY(), width, height, 0xFF000000 | (hover ? UiTheme.GOLD : UiTheme.ORANGE));
        g.blit(UiTheme.EMBLEM, getX() + 2, getY() + 2, 0, 0, 16, 16, 16, 16);
    }
}
