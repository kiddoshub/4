package meteordevelopment.meteorclient.gui.themes.skeet.widgets;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.skeet.SkeetWidget;
import meteordevelopment.meteorclient.gui.widgets.WVerticalSeparator;
public class WSkeetVerticalSeparator extends WVerticalSeparator implements SkeetWidget {
    @Override protected void onRender(GuiRenderer renderer,double mx,double my,double delta){ renderer.quad(x+width/2,y,1,height,theme().outlineColor.get()); }
}
