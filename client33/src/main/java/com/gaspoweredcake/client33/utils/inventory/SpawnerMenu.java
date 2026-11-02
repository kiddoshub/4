package com.gaspoweredcake.client33.utils.inventory;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reads only the visible information item from a matching 54-slot menu. */
public final class SpawnerMenu {
    private SpawnerMenu() {}

    public static boolean matches(String title, String itemName, List<String> lore) {
        String name = itemName.strip().toLowerCase(Locale.ROOT);
        if (name.contains("spawner info")) return true;
        if (!name.endsWith(" spawner") || !name.equals(title.strip().toLowerCase(Locale.ROOT))) return false;
        boolean stacked = false, stored = false;
        for (String line : lore) {
            String value = line.strip().toLowerCase(Locale.ROOT);
            if (value.startsWith("stacked:")) stacked = true;
            if (value.startsWith("stored loot:")) stored = true;
        }
        return stacked && stored;
    }

    public static String read(AbstractContainerScreen<?> screen) {
        if (!(screen.getMenu() instanceof ChestMenu menu) || menu.getRowCount() != 6 || menu.slots.size() != 90) return null;
        ItemStack item = menu.getSlot(49).getItem();
        if (item.isEmpty()) return null;
        List<String> lines = new ArrayList<>();
        var lore = item.get(DataComponents.LORE);
        if (lore != null) for (var line : lore.lines()) {
            if (lines.size() >= 32) break;
            lines.add(line.getString());
        }
        String name = item.getHoverName().getString().strip();
        if (!matches(screen.getTitle().getString(), name, lines)) return null;
        StringBuilder text = new StringBuilder(name.substring(0, Math.min(name.length(), 160)));
        for (String line : lines) {
            String value = line.strip();
            if (value.isEmpty()) continue;
            text.append(" | ").append(value, 0, Math.min(value.length(), 160));
            if (text.length() >= 640) break;
        }
        return text.toString();
    }
}
