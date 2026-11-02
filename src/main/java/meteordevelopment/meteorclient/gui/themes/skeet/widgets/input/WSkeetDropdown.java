package meteordevelopment.meteorclient.gui.themes.skeet.widgets.input;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.skeet.SkeetWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;

public class WSkeetDropdown<T> extends WDropdown<T> implements SkeetWidget {
    public WSkeetDropdown(T[] values, T value) {
        super(values, value);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double s = 0.5;
        renderer.quad(x, y, width, height, theme().backgroundColor.get());
        if (mouseOver) renderer.quad(x, y, theme.scale(2), height, theme().accentColor.get());
        renderer.quad(x, y, width, s, theme().outlineColor.get());
        renderer.quad(x, y + height - s, width, s, theme().outlineColor.get());
        renderer.quad(x, y, s, height, theme().outlineColor.get());
        renderer.quad(x + width - s, y, s, height, theme().outlineColor.get());
        super.onRender(renderer, mouseX, mouseY, delta);
    }
}
