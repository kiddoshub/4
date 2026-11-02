
package com.gaspoweredcake.client33.utils.player;

import com.gaspoweredcake.client33.mixin.CreativeModeInventoryScreenAccessor;
import com.gaspoweredcake.client33.mixin.CreativeModeTabsAccessor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import static com.gaspoweredcake.client33.Client33.mc;

/** Player inventory indices are distinct from server menu slot IDs. Resolve the actual menu layout. */
public final class SlotUtils {
    public static final int HOTBAR_START = 0, HOTBAR_END = 8;
    public static final int MAIN_START = 9, MAIN_END = 35;
    /** Actual inventory indices: boots, leggings, chest, helmet. */
    public static final int ARMOR_START = 36, ARMOR_END = 39;
    public static final int OFFHAND = 40;

    private SlotUtils() {}

    public static int indexToId(int index) {
        if (mc.player == null || index < 0 || index >= mc.player.getInventory().getContainerSize()) return -1;
        AbstractContainerMenu menu = mc.player.containerMenu;
        if (menu instanceof CreativeModeInventoryScreen.ItemPickerMenu) {
            if (CreativeModeInventoryScreenAccessor.client33$getSelectedTab()
                != BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabsAccessor.client33$getInventory())) return -1;
            // Creative's inventory tab wraps the player's menu slots using their original IDs.
            menu = mc.player.inventoryMenu;
        }
        for (var slot : menu.slots) {
            if (slot.container == mc.player.getInventory() && slot.getContainerSlot() == index && slot.isActive()) return slot.index;
        }
        return -1;
    }

    public static int armorIndex(int equipmentIndex) {
        return equipmentIndex >= 0 && equipmentIndex < 4 ? ARMOR_START + equipmentIndex : -1;
    }
    public static boolean isHotbar(int index) { return index >= HOTBAR_START && index <= HOTBAR_END; }
    public static boolean isMain(int index) { return index >= MAIN_START && index <= MAIN_END; }
    public static boolean isArmor(int index) { return index >= ARMOR_START && index <= ARMOR_END; }
}
