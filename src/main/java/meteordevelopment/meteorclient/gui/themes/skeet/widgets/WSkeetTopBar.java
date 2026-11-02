package meteordevelopment.meteorclient.gui.themes.skeet.widgets;
import meteordevelopment.meteorclient.gui.themes.skeet.SkeetWidget;
import meteordevelopment.meteorclient.gui.widgets.WTopBar;
import meteordevelopment.meteorclient.utils.render.color.Color;
public class WSkeetTopBar extends WTopBar implements SkeetWidget {
    @Override protected Color getButtonColor(boolean pressed, boolean hovered){ return theme().backgroundColor.get(); }
    @Override protected Color getNameColor(){ return theme().textColor.get(); }
}
