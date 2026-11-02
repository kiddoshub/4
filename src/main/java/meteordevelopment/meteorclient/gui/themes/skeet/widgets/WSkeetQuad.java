package meteordevelopment.meteorclient.gui.themes.skeet.widgets;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.skeet.SkeetWidget;
import meteordevelopment.meteorclient.gui.widgets.WQuad;
import meteordevelopment.meteorclient.utils.render.color.Color;
public class WSkeetQuad extends WQuad implements SkeetWidget {
    public WSkeetQuad(Color color){super(color);}
    @Override protected void onRender(GuiRenderer renderer,double mx,double my,double delta){renderer.quad(x,y,width,height,color);}
}
