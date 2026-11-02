
package com.gaspoweredcake.client33.utils.misc.input;

import com.gaspoweredcake.client33.gui.GuiKeyEvents;
import com.gaspoweredcake.client33.mixin.KeyMappingAccessor;
import com.gaspoweredcake.client33.utils.misc.CursorStyle;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;

import static com.gaspoweredcake.client33.Client33.mc;

public class Input {
    private static final InputState state = new InputState();

    private static CursorStyle lastCursorStyle = CursorStyle.Default;

    private Input() {
    }

    public static void setKeyState(int key, boolean pressed) {
        state.set(InputState.Device.Keyboard, key, pressed);
    }

    public static void setButtonState(int button, boolean pressed) {
        state.set(InputState.Device.Mouse, button, pressed);
    }

    public static void clearState() {
        state.clear();
    }

    public static int getKey(KeyMapping bind) {
        return ((KeyMappingAccessor) bind).client33$getKey().getValue();
    }

    public static void setKeyState(KeyMapping bind, boolean pressed) {
        var key = ((KeyMappingAccessor) bind).client33$getKey();
        if (key.getType() == InputConstants.Type.MOUSE) setButtonState(key.getValue(), pressed);
        else setKeyState(key.getValue(), pressed);
    }

    public static boolean isPressed(KeyMapping bind) {
        var key = ((KeyMappingAccessor) bind).client33$getKey();
        return key.getType() == InputConstants.Type.MOUSE ? isButtonPressed(key.getValue()) : isKeyPressed(key.getValue());
    }

    public static boolean isKeyPressed(int key) {
        if (!GuiKeyEvents.canUseKeys) return false;

        if (key == InputConstants.UNKNOWN.getValue()) return false;
        return state.pressed(InputState.Device.Keyboard, key);
    }

    public static boolean isButtonPressed(int button) {
        if (button == -1) return false;
        return state.pressed(InputState.Device.Mouse, button);
    }

    public static void setCursorStyle(CursorStyle style) {
        if (lastCursorStyle != style) {
            style.getCursor().select();
            lastCursorStyle = style;
        }
    }

    public static int getModifier(int key) {
        return switch (key) {
            case InputConstants.KEY_LSHIFT, InputConstants.KEY_RSHIFT -> InputConstants.MOD_SHIFT;
            case InputConstants.KEY_LCONTROL, InputConstants.KEY_RCONTROL -> InputConstants.MOD_CONTROL;
            case InputConstants.KEY_LALT, InputConstants.KEY_RALT -> InputConstants.MOD_ALT;
            case InputConstants.KEY_LGUI, InputConstants.KEY_RGUI -> InputConstants.MOD_SUPER;
            default -> 0;
        };
    }
}
