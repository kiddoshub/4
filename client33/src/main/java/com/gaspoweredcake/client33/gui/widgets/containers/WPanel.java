package com.gaspoweredcake.client33.gui.widgets.containers;

import com.gaspoweredcake.client33.gui.renderer.GuiRenderer;
import com.gaspoweredcake.client33.gui.themes.client33.Client33GuiTheme;
import com.gaspoweredcake.client33.utils.render.color.Color;

/** A quiet inset surface for related controls. Child cells supply their padding. */
public class WPanel extends WVerticalList {
    private static final Color BACKGROUND = new Color(20, 21, 28, 250);
    private static final Color BORDER = new Color(52, 53, 65, 255);
    @Override protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        Color background = theme instanceof Client33GuiTheme clientTheme ? clientTheme.panelBackground.get() : BACKGROUND;
        Color border = theme instanceof Client33GuiTheme clientTheme ? clientTheme.panelBorder.get() : BORDER;
        double line = theme.scale(1);
        renderer.quad(x, y, width, height, background);
        renderer.quad(x, y, width, line, border);
        renderer.quad(x, y, line, height, border);
        renderer.quad(x + width - line, y, line, height, border);
        renderer.quad(x, y + height - line, width, line, border);
    }
}
