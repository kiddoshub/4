
package com.gaspoweredcake.client33.gui.themes.client33;

import com.gaspoweredcake.client33.gui.renderer.GuiRenderer;
import com.gaspoweredcake.client33.gui.utils.BaseWidget;
import com.gaspoweredcake.client33.gui.widgets.WWidget;
import com.gaspoweredcake.client33.utils.render.color.Color;

public interface Client33Widget extends BaseWidget {
    default Client33GuiTheme theme() {
        return (Client33GuiTheme) getTheme();
    }

    default void renderBackground(GuiRenderer renderer, WWidget widget, Color outlineColor, Color backgroundColor) {
        Client33GuiTheme theme = theme();
        double s = theme.scale(1);
        double corner = theme.scale(5);

        renderer.quad(widget.x, widget.y, widget.width, widget.height, backgroundColor);

        renderer.quad(widget.x, widget.y, widget.width, s, outlineColor);
        renderer.quad(widget.x, widget.y + widget.height - s, widget.width, s, outlineColor);
        renderer.quad(widget.x, widget.y + s, s, widget.height - s * 2, outlineColor);
        renderer.quad(widget.x + widget.width - s, widget.y + s, s, widget.height - s * 2, outlineColor);
        renderer.quad(widget.x, widget.y, corner, s, theme.accentColor.get());
        renderer.quad(widget.x, widget.y, s, corner, theme.accentColor.get());
    }

    default void renderBackground(GuiRenderer renderer, WWidget widget, boolean pressed, boolean mouseOver) {
        Client33GuiTheme theme = theme();
        renderBackground(renderer, widget, theme.outlineColor.get(pressed, mouseOver), theme.backgroundColor.get(pressed, mouseOver));
    }
}
