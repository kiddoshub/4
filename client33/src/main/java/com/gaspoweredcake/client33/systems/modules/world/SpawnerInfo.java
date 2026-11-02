package com.gaspoweredcake.client33.systems.modules.world;

import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.utils.inventory.SpawnerMenu;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Reports information actually displayed by a manually opened spawner menu. */
public class SpawnerInfo extends Module {
    private Object menuSeen;
    private String lastObservation;

    public SpawnerInfo() {
        super(Categories.World, "spawner-info", "Reads the Spawner Info item in an opened 54-slot menu.");
    }

    @Override public void onActivate() { clear(); }
    @Override public void onDeactivate() { clear(); }
    private void clear() { menuSeen = null; lastObservation = null; }

    @EventHandler private void onTick(TickEvent.Post event) {
        if (mc.player == null || !(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)
            || screen.getMenu() != mc.player.containerMenu) { clear(); return; }
        if (menuSeen != screen.getMenu()) { clear(); menuSeen = screen.getMenu(); }
        String observation = SpawnerMenu.read(screen);
        if (observation == null) return;
        if (!observation.equals(lastObservation)) {
            lastObservation = observation;
            info("Spawner Info: %s", observation);
        }
    }
}
