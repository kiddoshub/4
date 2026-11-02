package meteordevelopment.meteorclient.gui.themes.skeet.widgets.pressable;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.skeet.SkeetWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
public class WSkeetMinus extends WMinus implements SkeetWidget {
    @Override protected void onRender(GuiRenderer renderer,double mx,double my,double delta){
        double s=0.5;renderer.quad(x,y,width,height,theme().backgroundColor.get());
        renderer.quad(x,y,width,s,theme().outlineColor.get());renderer.quad(x,y+height-s,width,s,theme().outlineColor.get());
        renderer.quad(x,y,s,height,theme().outlineColor.get());renderer.quad(x+width-s,y,s,height,theme().outlineColor.get());
        renderer.quad(x+2,y+height/2-1,width-4,2,theme().minusColor.get());
    }
}
