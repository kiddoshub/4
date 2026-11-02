
package meteordevelopment.meteorclient.gui.themes.skeet;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.BaseWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.utils.render.color.Color;

public interface SkeetWidget extends BaseWidget {
    default SkeetGuiTheme theme() {
        return (SkeetGuiTheme) getTheme();
    }

    default void renderBackground(GuiRenderer renderer, WWidget widget, Color outlineColor, Color backgroundColor) {
        double s = 0.5;
        renderer.quad(widget.x, widget.y, widget.width, widget.height, backgroundColor);
        renderer.quad(widget.x, widget.y, widget.width, s, outlineColor);
        renderer.quad(widget.x, widget.y + widget.height - s, widget.width, s, outlineColor);
        renderer.quad(widget.x, widget.y, s, widget.height, outlineColor);
        renderer.quad(widget.x + widget.width - s, widget.y, s, widget.height, outlineColor);
    }

    default void renderBackground(GuiRenderer renderer, WWidget widget, boolean pressed, boolean mouseOver) {
        SkeetGuiTheme theme = theme();
        renderBackground(renderer, widget, theme.outlineColor.get(), theme.backgroundColor.get());
    }
}
