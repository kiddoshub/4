package meteordevelopment.meteorclient.gui.themes.skeet.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.skeet.SkeetWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;

public class WSkeetCheckbox extends WCheckbox implements SkeetWidget {
    public WSkeetCheckbox(boolean checked){
        super(checked);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta){
        double s = 0.5;
        renderer.quad(x, y, width, height, theme().backgroundColor.get());
        renderer.quad(x, y, width, s, theme().outlineColor.get());
        renderer.quad(x, y + height - s, width, s, theme().outlineColor.get());
        renderer.quad(x, y, s, height, theme().outlineColor.get());
        renderer.quad(x + width - s, y, s, height, theme().outlineColor.get());
        if (checked) {
            renderer.quad(x + 2, y + 2, width - 4, height - 4, theme().accentColor.get());
        }
    }
}
