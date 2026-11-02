
package meteordevelopment.meteorclient.gui.widgets.pressable;

import net.minecraft.client.gui.Click;

public class WConfirmedMinus extends WMinus {
    protected boolean pressedOnce = false;

    @Override
    public boolean onMouseClicked(Click click, boolean doubled) {
        boolean pressed = super.onMouseClicked(click, doubled);
        if (!pressed) {
            pressedOnce = false;
        }
        return pressed;
    }

    @Override
    public boolean onMouseReleased(Click click) {
        if (pressed && pressedOnce) super.onMouseReleased(click);
        pressedOnce = pressed;
        return pressed = false;
    }
}
