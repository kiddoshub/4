
package com.gaspoweredcake.client33.utils.misc.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.gaspoweredcake.client33.Client33;
import net.minecraft.client.KeyMapping;

public class KeyBinds {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Client33.identifier("client33"));

    public static KeyMapping OPEN_GUI = new KeyMapping("key.client33.open-gui", InputConstants.Type.KEYBOARD, InputConstants.KEY_RSHIFT, CATEGORY);
    public static KeyMapping OPEN_COMMANDS = new KeyMapping("key.client33.open-commands", InputConstants.Type.KEYBOARD, InputConstants.KEY_PERIOD, CATEGORY);

    private KeyBinds() {
    }

    public static KeyMapping[] apply(KeyMapping[] binds) {
        // Add key binding
        KeyMapping[] newBinds = new KeyMapping[binds.length + 2];

        System.arraycopy(binds, 0, newBinds, 0, binds.length);
        newBinds[binds.length] = OPEN_GUI;
        newBinds[binds.length + 1] = OPEN_COMMANDS;

        return newBinds;
    }
}
