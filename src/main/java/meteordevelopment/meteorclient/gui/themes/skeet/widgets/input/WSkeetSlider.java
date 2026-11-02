package meteordevelopment.meteorclient.gui.themes.skeet.widgets.input;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.skeet.SkeetWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;

public class WSkeetSlider extends WSlider implements SkeetWidget {
    public WSkeetSlider(double value, double min, double max) {
        super(value, min, max);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double s = 0.5;
        double value = (this.value - min) / (max - min);
        renderer.quad(x, y + height / 2 - theme.scale(2), width, theme.scale(4), theme().backgroundColor.get());
        if (value > 0) renderer.quad(x, y + height / 2 - theme.scale(2), width * value, theme.scale(4), theme().accentColor.get());
        renderer.quad(x + width * value - theme.scale(4), y, theme.scale(8), height, theme().accentColor.get());
        renderer.quad(x + width * value - theme.scale(4), y, theme.scale(8), s, theme().outlineColor.get());
        renderer.quad(x + width * value - theme.scale(4), y + height - s, theme.scale(8), s, theme().outlineColor.get());
        renderer.quad(x + width * value - theme.scale(4), y, s, height, theme().outlineColor.get());
        renderer.quad(x + width * value - theme.scale(4) + theme.scale(8) - s, y, s, height, theme().outlineColor.get());
    }
}
