package com.dragonblock.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Gear worn in the accessory inventory (key K). Each item fits one slot; while worn it is drawn on the
 * player for everyone and applies its effect. Effects live where the stat is computed (TP, ki regen,
 * speed) or in the scouter HUD.
 */
public class AccessoryItem extends Item {
    public enum Slot {
        VISOR("Visor"), TORSO("Torso"), WRISTS("Muñecas"), BELT("Cintura");

        private final String label;

        Slot(String label) { this.label = label; }

        public String label() { return label; }
    }

    private final Slot slot;
    private final String effect;

    public AccessoryItem(Slot slot, String effect) {
        this(slot, effect, new Item.Properties().stacksTo(1));
    }

    protected AccessoryItem(Slot slot, String effect, Item.Properties properties) {
        super(properties);
        this.slot = slot;
        this.effect = effect;
    }

    public Slot getSlot() {
        return slot;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Accesorio · " + slot.label()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal(effect).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Equípalo en tus accesorios [K].").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
