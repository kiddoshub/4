
package com.gaspoweredcake.client33.gui.themes.client33.widgets;

import com.gaspoweredcake.client33.gui.renderer.GuiRenderer;
import com.gaspoweredcake.client33.gui.themes.client33.Client33Widget;
import com.gaspoweredcake.client33.gui.widgets.WWidget;
import com.gaspoweredcake.client33.gui.widgets.containers.WWindow;
import com.gaspoweredcake.client33.utils.render.color.Color;

public class WClient33Window extends WWindow implements Client33Widget {

    public WClient33Window(WWidget icon, String title) {
        super(icon, title);
    }

    @Override
    protected WHeader header(WWidget icon) {
        return new WClient33Header(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (expanded || animProgress > 0) {
            renderer.quad(x, y + header.height, width, height - header.height, theme().backgroundColor.get());

            double border = theme.scale(1);
            double bodyHeight = height - header.height;
            Color outline = theme().outlineColor.get();
            renderer.quad(x, y + header.height, border, bodyHeight, outline);
            renderer.quad(x + width - border, y + header.height, border, bodyHeight, outline);
            renderer.quad(x, y + height - border, width, border, outline);
            renderer.quad(x, y + height - border, theme.scale(12), border, theme().accentColor.get());
        }
    }

    private class WClient33Header extends WHeader {
        public WClient33Header(WWidget icon) {
            super(icon);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            double line = theme.scale(1);
            renderer.quad(x, y, width, height, theme().backgroundColor.get());
            renderer.quad(x, y, width, line, theme().outlineColor.get());
            renderer.quad(x, y, line * 2, height, theme().accentColor.get());
            renderer.quad(x, y + height - line, width, line, theme().outlineColor.get());
        }
    }
}
