
package com.gaspoweredcake.client33.events.client33;

import com.gaspoweredcake.client33.events.Cancellable;
import com.gaspoweredcake.client33.utils.misc.input.KeyAction;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

public class MouseClickEvent extends Cancellable {
    private static final MouseClickEvent INSTANCE = new MouseClickEvent();

    public MouseButtonEvent click;
    public MouseButtonInfo input;
    public KeyAction action;

    public static MouseClickEvent get(MouseButtonEvent click, KeyAction action) {
        INSTANCE.setCancelled(false);
        INSTANCE.click = click;
        INSTANCE.input = click.buttonInfo();
        INSTANCE.action = action;
        return INSTANCE;
    }

    public int button() {
        return INSTANCE.input.button();
    }
}
