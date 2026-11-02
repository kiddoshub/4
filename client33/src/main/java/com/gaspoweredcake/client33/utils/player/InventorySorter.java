package com.gaspoweredcake.client33.utils.player;

import com.gaspoweredcake.client33.utils.render.PeekScreen;
import com.gaspoweredcake.client33.mixin.AbstractContainerScreenAccessor;
import com.gaspoweredcake.client33.mixin.CreativeModeInventoryScreenAccessor;
import com.gaspoweredcake.client33.mixin.CreativeModeTabsAccessor;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.gaspoweredcake.client33.Client33.mc;

/** Plans against copied component-bearing stacks, aborting if the screen or affected slots change. */
public final class InventorySorter {
    private record Action(int from, int to, Slot fromView, Slot toView, ItemStack expectedFrom, ItemStack expectedTo) {}
    private static final class PlannedSlot {
        final int id;
        ItemStack stack;
        PlannedSlot(int id, Slot slot) { this.id = id; stack = slot.getItem().copy(); }
    }
    private final AbstractContainerScreen<?> screen;
    private final AbstractContainerMenu menu;
    private final List<Action> actions = new ArrayList<>();
    private int next, ticks;
    private boolean invalid;

    public InventorySorter(AbstractContainerScreen<?> screen, Slot origin) {
        this.screen = screen;
        menu = screen.getMenu();
        if (mc.player == null || menu != mc.player.containerMenu || screen instanceof PeekScreen
            || !compatibleScreen() || !menu.getCarried().isEmpty() || !origin.isActive()) { invalid = true; return; }
        boolean playerInventory = origin.container == mc.player.getInventory();
        if ((playerInventory && !SlotUtils.isMain(origin.getContainerSlot()))
            || (!playerInventory && !(screen instanceof ContainerScreen || screen instanceof ShulkerBoxScreen))) { invalid = true; return; }
        List<PlannedSlot> slots = new ArrayList<>();
        for (int id = 0; id < menu.slots.size(); id++) {
            Slot slot = menu.slots.get(id);
            if (!slot.isActive() || !slot.mayPickup(mc.player)) continue;
            boolean samePart = playerInventory ? slot.container == mc.player.getInventory() && SlotUtils.isMain(slot.getContainerSlot())
                : slot.container == origin.container && !(slot.container instanceof Inventory);
            if (samePart) slots.add(new PlannedSlot(id, slot));
        }
        if (slots.size() > 108) { invalid = true; return; }
        slots.sort(Comparator.comparingInt(slot -> slot.id));
        compact(slots);
        order(slots);
    }

    /** Returns true when complete or safely cancelled. */
    public boolean tick(int delay) {
        if (invalid || next >= actions.size()) return true;
        if (mc.player == null || mc.gui.screen() != screen || mc.player.containerMenu != menu || !compatibleScreen() || !menu.getCarried().isEmpty()) return true;
        if (ticks++ < Math.max(0, delay)) return false;
        ticks = 0;
        Action action = actions.get(next);
        Slot from = menu.getSlot(action.from), to = menu.getSlot(action.to);
        if (from != action.fromView || to != action.toView || !from.isActive() || !to.isActive() || !from.mayPickup(mc.player) || !to.mayPickup(mc.player)
            || !to.mayPlace(from.getItem()) || (!to.getItem().isEmpty() && !from.mayPlace(to.getItem()))
            || !matches(from.getItem(), action.expectedFrom) || !matches(to.getItem(), action.expectedTo)) return true;
        if (screen instanceof CreativeModeInventoryScreen) {
            var clicks = (AbstractContainerScreenAccessor) screen;
            clicks.client33$clickSlot(from, action.from, 0, ContainerInput.PICKUP);
            clicks.client33$clickSlot(to, action.to, 0, ContainerInput.PICKUP);
            if (!menu.getCarried().isEmpty()) clicks.client33$clickSlot(from, action.from, 0, ContainerInput.PICKUP);
        } else InvUtils.move().fromId(action.from).toId(action.to);
        next++;
        return next >= actions.size();
    }

    private boolean compatibleScreen() {
        return !(screen instanceof CreativeModeInventoryScreen) || CreativeModeInventoryScreenAccessor.client33$getSelectedTab()
            == BuiltInRegistries.CREATIVE_MODE_TAB.getValue(CreativeModeTabsAccessor.client33$getInventory());
    }
    private static boolean matches(ItemStack actual, ItemStack expected) {
        return actual.getCount() == expected.getCount() && ItemStack.isSameItemSameComponents(actual, expected);
    }
    private void record(PlannedSlot from, PlannedSlot to) {
        actions.add(new Action(from.id, to.id, menu.getSlot(from.id), menu.getSlot(to.id), from.stack.copy(), to.stack.copy()));
    }
    private void compact(List<PlannedSlot> slots) {
        for (int i = 0; i < slots.size(); i++) {
            PlannedSlot to = slots.get(i);
            if (to.stack.isEmpty() || !to.stack.isStackable()) continue;
            int capacity = Math.min(to.stack.getMaxStackSize(), menu.getSlot(to.id).getMaxStackSize(to.stack));
            for (int j = i + 1; j < slots.size() && to.stack.getCount() < capacity; j++) {
                PlannedSlot from = slots.get(j);
                if (from.stack.isEmpty() || !ItemStack.isSameItemSameComponents(from.stack, to.stack)) continue;
                record(from, to);
                int moved = Math.min(from.stack.getCount(), capacity - to.stack.getCount());
                to.stack = to.stack.copyWithCount(to.stack.getCount() + moved);
                from.stack = from.stack.copyWithCount(from.stack.getCount() - moved);
            }
        }
    }
    private void order(List<PlannedSlot> slots) {
        for (int i = 0; i < slots.size(); i++) {
            PlannedSlot destination = slots.get(i), best = destination;
            for (int j = i + 1; j < slots.size(); j++) if (compare(slots.get(j).stack, best.stack) < 0) best = slots.get(j);
            if (best == destination || best.stack.isEmpty()) continue;
            // Container storage and main inventory accept ordinary stacks; preserve custom slot constraints too.
            if (best.stack.getCount() > menu.getSlot(destination.id).getMaxStackSize(best.stack)
                || (!destination.stack.isEmpty() && destination.stack.getCount() > menu.getSlot(best.id).getMaxStackSize(destination.stack))) { invalid = true; return; }
            record(best, destination);
            ItemStack previous = destination.stack; destination.stack = best.stack; best.stack = previous;
        }
    }
    private static int compare(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) return Boolean.compare(first.isEmpty(), second.isEmpty());
        int item = BuiltInRegistries.ITEM.getKey(first.getItem()).compareTo(BuiltInRegistries.ITEM.getKey(second.getItem()));
        if (item != 0) return item;
        int count = Integer.compare(second.getCount(), first.getCount());
        if (count != 0) return count;
        return Integer.compare(first.getDamageValue(), second.getDamageValue());
    }
}
