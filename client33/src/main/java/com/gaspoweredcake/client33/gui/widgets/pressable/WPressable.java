
package com.gaspoweredcake.client33.gui.widgets.pressable;

import com.gaspoweredcake.client33.gui.widgets.WWidget;
import net.minecraft.client.input.MouseButtonEvent;

import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;

public abstract class WPressable extends WWidget {
    public Runnable action;

    protected boolean pressed;

    @Override
    public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
        if (mouseOver && (click.button() == MOUSE_BUTTON_LEFT || click.button() == MOUSE_BUTTON_RIGHT))
            pressed = true;
        return pressed;
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent click) {
        if (pressed) {
            onPressed(click.button());
            if (action != null) action.run();

            pressed = false;
        }

        return false;
    }

    protected void onPressed(int button) {
    }
}
