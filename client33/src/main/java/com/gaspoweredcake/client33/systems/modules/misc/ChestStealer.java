package com.gaspoweredcake.client33.systems.modules.misc;

import com.gaspoweredcake.client33.events.packets.InventoryEvent;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.settings.IntSetting;
import com.gaspoweredcake.client33.settings.Setting;
import com.gaspoweredcake.client33.settings.SettingGroup;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.utils.inventory.ChestTransfer;
import com.gaspoweredcake.client33.utils.inventory.ChestStealSession;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Moves stacks only from an initialized, open chest-style menu on the client thread. */
public class ChestStealer extends Module {
    private final SettingGroup general = settings.getDefaultGroup();
    private final Setting<Integer> stacksPerTick = general.add(new IntSetting.Builder()
        .name("stacks-per-tick").description("Maximum stacks to transfer in each batch.")
        .defaultValue(3).range(1, 9).sliderRange(1, 9).build());
    private final Setting<Integer> delay = general.add(new IntSetting.Builder()
        .name("delay").description("Ticks to wait between transfer batches.")
        .defaultValue(0).range(0, 20).sliderRange(0, 20).build());

    private final ChestStealSession session = new ChestStealSession();

    public ChestStealer() {
        super(Categories.Misc, "chest-stealer", "Quickly transfers items from open chest-style containers.");
    }

    @Override
    public void onActivate() {
        clear();
        // A screen already open when enabled has already received its initial contents.
        if (mc.player != null && mc.gui.screen() instanceof AbstractContainerScreen<?> screen
            && screen.getMenu() == mc.player.containerMenu && isChestMenu(screen.getMenu())) session.markReady(screen.getMenu());
    }

    @Override
    public void onDeactivate() { clear(); }

    @EventHandler
    private void onInventory(InventoryEvent event) {
        if (mc.player == null || !(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) return;
        AbstractContainerMenu menu = mc.player.containerMenu;
        if (menu == screen.getMenu() && isChestMenu(menu) && event.packet.containerId() == menu.containerId) session.markReady(menu);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.gameMode == null || !(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            clear();
            return;
        }
        AbstractContainerMenu menu = mc.player.containerMenu;
        if (screen.getMenu() != menu || !isChestMenu(menu)) { clear(); return; }
        session.beginTick(menu);
        if (!session.canRun(menu, menu.getCarried().isEmpty(), Modules.get().get(InventoryTweaks.class).isAutoMoving())) return;

        int chestSlots = menu.slots.size() - 36;
        int moved = 0;
        for (int i = 0; i < chestSlots && moved < stacksPerTick.get(); i++) {
            // Check again before every click; another action may have changed the screen or cursor.
            if (mc.gui.screen() != screen || mc.player.containerMenu != menu || !menu.getCarried().isEmpty()) break;
            Slot slot = menu.getSlot(i);
            ItemStack stack = slot.getItem();
            if (!session.canAttempt(i) || slot.container == mc.player.getInventory() || !slot.isActive() || !slot.mayPickup(mc.player) || stack.isEmpty() || !ChestTransfer.hasSpace(menu.slots, chestSlots, mc.player.getInventory(), stack)) continue;
            // Allow corrections from the server before retrying the same slot.
            session.attempted(i);
            mc.gameMode.handleContainerInput(menu.containerId, i, 0, ContainerInput.QUICK_MOVE, mc.player);
            moved++;
        }
        if (moved > 0) session.finishBatch(delay.get());
    }

    private boolean isChestMenu(AbstractContainerMenu menu) {
        return menu instanceof ChestMenu && (menu.getType() == MenuType.GENERIC_9x3 || menu.getType() == MenuType.GENERIC_9x6)
            && (menu.slots.size() == 63 || menu.slots.size() == 90);
    }

    private void clear() { session.reset(); }
}
