package meteordevelopment.meteorclient.gui.themes.skeet.widgets;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.skeet.SkeetWidget;
import meteordevelopment.meteorclient.gui.widgets.WTooltip;
public class WSkeetTooltip extends WTooltip implements SkeetWidget {
    public WSkeetTooltip(String text){super(text);}
    @Override protected void onRender(GuiRenderer renderer,double mx,double my,double delta){
        double s=0.5;renderer.quad(x,y,width,height,theme().backgroundColor.get());
        renderer.quad(x,y,width,s,theme().outlineColor.get());renderer.quad(x,y+height-s,width,s,theme().outlineColor.get());
        renderer.quad(x,y,s,height,theme().outlineColor.get());renderer.quad(x+width-s,y,s,height,theme().outlineColor.get());
    }
}
