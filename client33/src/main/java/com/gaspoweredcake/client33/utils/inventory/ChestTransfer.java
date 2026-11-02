package com.gaspoweredcake.client33.utils.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Checks the actual destination slots before issuing a quick move. */
public final class ChestTransfer {
    private ChestTransfer() {}

    public static boolean hasSpace(List<Slot> slots, int firstPlayerSlot, Container inventory, ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (int i = firstPlayerSlot; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (slot.container != inventory || !slot.isActive() || !slot.mayPlace(stack)) continue;
            ItemStack target = slot.getItem();
            if (target.isEmpty() && slot.getMaxStackSize(stack) > 0) return true;
            if (ItemStack.isSameItemSameComponents(stack, target)
                && target.getCount() < Math.min(target.getMaxStackSize(), slot.getMaxStackSize(stack))) return true;
        }
        return false;
    }
}
