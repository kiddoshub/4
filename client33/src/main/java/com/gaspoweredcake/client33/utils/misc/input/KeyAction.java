
package com.gaspoweredcake.client33.utils.misc.input;

import com.mojang.blaze3d.platform.InputConstants;

public enum KeyAction {
    Press,
    Repeat,
    Release;

    public static KeyAction get(int action) {
        return switch (action) {
            case InputConstants.PRESS -> Press;
            case InputConstants.RELEASE -> Release;
            default -> Repeat;
        };
    }
}
