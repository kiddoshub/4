package meteordevelopment.meteorclient.gui.themes.skeet.widgets.input;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.skeet.SkeetWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.utils.CharFilter;

public class WSkeetTextBox extends WTextBox implements SkeetWidget {
    public WSkeetTextBox(String text, String placeholder, CharFilter filter, Class<? extends Renderer> renderer) {
        super(text, placeholder, filter, renderer);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double s = 0.5;
        renderer.quad(x, y, width, height, theme().backgroundColor.get());
        renderer.quad(x, y, width, s, theme().outlineColor.get());
        renderer.quad(x, y + height - s, width, s, theme().outlineColor.get());
        renderer.quad(x, y, s, height, theme().outlineColor.get());
        renderer.quad(x + width - s, y, s, height, theme().outlineColor.get());
        super.onRender(renderer, mouseX, mouseY, delta);
    }
}
