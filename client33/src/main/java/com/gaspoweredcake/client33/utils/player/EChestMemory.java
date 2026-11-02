
package com.gaspoweredcake.client33.utils.player;

import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.events.game.GameLeftEvent;
import com.gaspoweredcake.client33.events.game.OpenScreenEvent;
import com.gaspoweredcake.client33.events.world.BlockActivateEvent;
import com.gaspoweredcake.client33.utils.PreInit;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EnderChestBlock;

import static com.gaspoweredcake.client33.Client33.mc;

public class EChestMemory {
    public static final NonNullList<ItemStack> ITEMS = NonNullList.withSize(27, ItemStack.EMPTY);
    private static int echestOpenedState;
    private static boolean isKnown = false;

    private EChestMemory() {
    }

    @PreInit
    public static void init() {
        Client33.EVENT_BUS.subscribe(EChestMemory.class);
    }

    @EventHandler
    private static void onBlockActivate(BlockActivateEvent event) {
        if (event.blockState.getBlock() instanceof EnderChestBlock && echestOpenedState == 0) echestOpenedState = 1;
    }

    @EventHandler
    private static void onOpenScreenEvent(OpenScreenEvent event) {
        if (echestOpenedState == 1 && event.screen instanceof ContainerScreen) {
            echestOpenedState = 2;
            return;
        }
        if (echestOpenedState == 0) return;

        if (!(mc.gui.screen() instanceof ContainerScreen)) return;
        ChestMenu container = ((ContainerScreen) mc.gui.screen()).getMenu();
        if (container == null) return;
        Container inv = container.getContainer();

        for (int i = 0; i < 27; i++) {
            ITEMS.set(i, inv.getItem(i));
        }
        isKnown = true;

        echestOpenedState = 0;
    }

    @EventHandler
    private static void onLeaveEvent(GameLeftEvent event) {
        ITEMS.clear();
        isKnown = false;
    }

    public static boolean isKnown() {
        return isKnown;
    }
}
