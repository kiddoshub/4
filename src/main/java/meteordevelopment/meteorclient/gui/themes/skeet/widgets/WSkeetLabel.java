
package meteordevelopment.meteorclient.gui.themes.skeet.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.skeet.SkeetGuiTheme;
import meteordevelopment.meteorclient.gui.themes.skeet.SkeetWidget;
import meteordevelopment.meteorclient.gui.widgets.WLabel;

public class WSkeetLabel extends WLabel implements SkeetWidget {
    public WSkeetLabel(String text, boolean title, double maxWidth) {
        super(text, title, maxWidth);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        SkeetGuiTheme theme = theme();
        renderer.text(getText(), x, y, title ? theme.titleTextColor.get() : (color != null ? color : theme.textColor.get()), shadow);
    }
}
