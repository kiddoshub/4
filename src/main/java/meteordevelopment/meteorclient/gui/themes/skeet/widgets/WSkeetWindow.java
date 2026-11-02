
package meteordevelopment.meteorclient.gui.themes.skeet.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.skeet.SkeetWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;

public class WSkeetWindow extends WWindow implements SkeetWidget {
    public WSkeetWindow(WWidget icon, String title) {
        super(icon, title);
    }

    @Override
    protected WHeader header(WWidget icon) {
        return new WSkeetHeader(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (expanded || animProgress > 0) {
            double s = 0.5;
            renderer.quad(x, y + header.height, width, height - header.height, theme().backgroundColor.get());
            renderer.quad(x, y + header.height, width, s, theme().outlineColor.get());
            renderer.quad(x, y + header.height, s, height - header.height, theme().outlineColor.get());
            renderer.quad(x + width - s, y + header.height, s, height - header.height, theme().outlineColor.get());
            renderer.quad(x, y + height - s, width, s, theme().outlineColor.get());
        }
    }

    private class WSkeetHeader extends WHeader {
        public WSkeetHeader(WWidget icon) {
            super(icon);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            double s = 0.5;
            renderer.quad(this, theme().backgroundColor.get());
            renderer.quad(x, y, width, s, theme().outlineColor.get());
            renderer.quad(x, y + height - s, width, s, theme().outlineColor.get());
            renderer.quad(x, y, s, height, theme().outlineColor.get());
            renderer.quad(x + width - s, y, s, height, theme().outlineColor.get());
            // Rainbow bar like skeet
            renderer.quad(x, y, width, theme.scale(1), theme().accentColor.get());
        }
    }
}
