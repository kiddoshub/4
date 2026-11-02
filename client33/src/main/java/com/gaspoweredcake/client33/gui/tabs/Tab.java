
package com.gaspoweredcake.client33.gui.tabs;

import com.gaspoweredcake.client33.gui.GuiTheme;
import net.minecraft.client.gui.screens.Screen;

import static com.gaspoweredcake.client33.Client33.mc;

public abstract class Tab {
    public final String name;

    public Tab(String name) {
        this.name = name;
    }

    public void openScreen(GuiTheme theme) {
        TabScreen screen = this.createScreen(theme);
        screen.addDirect(theme.topBar()).top().centerX();
        mc.gui.setScreen(screen);
    }

    public abstract TabScreen createScreen(GuiTheme theme);

    public abstract boolean isScreen(Screen screen);
}
