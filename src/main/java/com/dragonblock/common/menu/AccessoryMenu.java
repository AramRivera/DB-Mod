package com.dragonblock.common.menu;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.item.AccessoryItem;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.registry.ModMenus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Accessory inventory: four slots (visor, torso, wrists, waist) above the player's inventory. On the
 * server the slots read and write the player's data directly, so what you wear is saved and synced to
 * everyone (other players draw it on you) the moment it changes.
 */
public class AccessoryMenu extends AbstractContainerMenu {
    public static final int SLOTS = AccessoryItem.Slot.values().length;
    /** Slot positions inside the screen, shared with AccessoryScreen. */
    public static final int SLOT_X = 152, SLOT_Y = 18, INV_X = 8, INV_Y = 104;

    private final SimpleContainer worn;

    /** Client side: contents arrive through the normal slot sync. */
    public AccessoryMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(SLOTS));
    }

    private AccessoryMenu(int id, Inventory inventory, SimpleContainer worn) {
        super(ModMenus.ACCESSORIES.get(), id);
        this.worn = worn;
        AccessoryItem.Slot[] kinds = AccessoryItem.Slot.values();
        for (int i = 0; i < SLOTS; i++) {
            AccessoryItem.Slot kind = kinds[i];
            addSlot(new Slot(worn, i, SLOT_X, SLOT_Y + i * 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof AccessoryItem a && a.getSlot() == kind;
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, INV_X + col * 18, INV_Y + 58));
    }

    /** Server side: a container backed by the player's worn accessories. */
    public static AccessoryMenu open(int id, Inventory inventory, ServerPlayer player) {
        PlayerDBCData data = PlayerDBCData.get(player);
        SimpleContainer worn = new SimpleContainer(SLOTS) {
            @Override
            public void setChanged() {
                super.setChanged();
                for (int i = 0; i < SLOTS; i++) {
                    ItemStack stack = getItem(i);
                    data.setAccessory(i, stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                }
                data.saveToPlayer(player);
                ModMessages.syncToTracking(player);
            }
        };
        for (int i = 0; i < SLOTS; i++) {
            String id2 = data.getAccessory(i);
            if (id2.isEmpty()) continue;
            ResourceLocation key = ResourceLocation.tryParse(id2);
            if (key != null && BuiltInRegistries.ITEM.containsKey(key)) {
                worn.setItem(i, new ItemStack(BuiltInRegistries.ITEM.get(key)));
            }
        }
        return new AccessoryMenu(id, inventory, worn);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < SLOTS) {
            // Worn -> inventory
            if (!moveItemStackTo(stack, SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof AccessoryItem a) {
            // Inventory -> its accessory slot
            int target = a.getSlot().ordinal();
            if (!moveItemStackTo(stack, target, target + 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive();
    }
}
