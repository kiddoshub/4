
package com.gaspoweredcake.client33.gui.themes.client33.widgets.pressable;

import com.gaspoweredcake.client33.gui.renderer.GuiRenderer;
import com.gaspoweredcake.client33.gui.renderer.packer.GuiTexture;
import com.gaspoweredcake.client33.gui.themes.client33.Client33GuiTheme;
import com.gaspoweredcake.client33.gui.themes.client33.Client33Widget;
import com.gaspoweredcake.client33.gui.widgets.pressable.WButton;
import com.gaspoweredcake.client33.utils.render.color.Color;
import net.minecraft.util.Mth;

public class WClient33Button extends WButton implements Client33Widget {
    private final Color surface = new Color();
    private final Color outline = new Color();
    private boolean colorInitialized;

    private static void approach(Color current, Color target, double amount) {
        current.r = (int) Math.round(Mth.lerp(amount, current.r, target.r));
        current.g = (int) Math.round(Mth.lerp(amount, current.g, target.g));
        current.b = (int) Math.round(Mth.lerp(amount, current.b, target.b));
        current.a = (int) Math.round(Mth.lerp(amount, current.a, target.a));
    }

    public WClient33Button(String text, GuiTexture texture) {
        super(text, texture);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        Client33GuiTheme theme = theme();
        double pad = pad();

        Color targetSurface = theme.backgroundColor.get(pressed, mouseOver);
        Color targetOutline = theme.outlineColor.get(pressed, mouseOver);
        double amount = colorInitialized ? Mth.clamp(delta * 14, 0, 1) : 1;
        approach(surface, targetSurface, amount);
        approach(outline, targetOutline, amount);
        colorInitialized = true;
        renderBackground(renderer, this, outline, surface);

        if (text != null) {
            renderer.text(text, x + width / 2 - textWidth / 2, y + pad, theme.textColor.get(), false);
        }
        else {
            double ts = theme.textHeight();
            renderer.quad(x + width / 2 - ts / 2, y + pad, ts, ts, texture, theme.textColor.get());
        }
    }
}
