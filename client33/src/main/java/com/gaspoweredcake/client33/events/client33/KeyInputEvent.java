
package com.gaspoweredcake.client33.events.client33;

import com.gaspoweredcake.client33.events.Cancellable;
import com.gaspoweredcake.client33.utils.misc.input.KeyAction;

public class KeyInputEvent extends Cancellable {
    private static final KeyInputEvent INSTANCE = new KeyInputEvent();

    public net.minecraft.client.input.KeyEvent input;
    public KeyAction action;

    public static KeyInputEvent get(net.minecraft.client.input.KeyEvent input, KeyAction action) {
        INSTANCE.setCancelled(false);
        INSTANCE.input = input;
        INSTANCE.action = action;
        return INSTANCE;
    }

    public int key() {
        return INSTANCE.input.key();
    }

    public int modifiers() {
        return INSTANCE.input.modifiers();
    }
}
