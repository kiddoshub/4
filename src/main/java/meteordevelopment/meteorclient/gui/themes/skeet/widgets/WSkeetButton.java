package meteordevelopment.meteorclient.gui.themes.skeet.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.skeet.SkeetWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;

public class WSkeetButton extends WButton implements SkeetWidget {
    public WSkeetButton(String text, GuiTexture texture){
        super(text, texture);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta){
        double s = 0.5;
        renderer.quad(x, y, width, height, theme().backgroundColor.get());
        if (mouseOver || pressed) {
            renderer.quad(x, y, theme.scale(2), height, theme().accentColor.get());
        }
        renderer.quad(x, y, width, s, theme().outlineColor.get());
        renderer.quad(x, y + height - s, width, s, theme().outlineColor.get());
        renderer.quad(x, y, s, height, theme().outlineColor.get());
        renderer.quad(x + width - s, y, s, height, theme().outlineColor.get());
        if (text != null) {
            renderer.text(text, x + theme.scale(6), y + theme.scale(4), theme().textColor.get(), false);
        }
    }
}
