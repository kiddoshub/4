
package com.gaspoweredcake.client33.events.game;

import com.gaspoweredcake.client33.events.Cancellable;
import net.minecraft.client.gui.screens.Screen;

public class OpenScreenEvent extends Cancellable {
    private static final OpenScreenEvent INSTANCE = new OpenScreenEvent();

    public Screen screen;

    public static OpenScreenEvent get(Screen screen) {
        INSTANCE.setCancelled(false);
        INSTANCE.screen = screen;
        return INSTANCE;
    }
}
