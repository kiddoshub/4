package meteordevelopment.meteorclient.gui.themes.skeet.widgets;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.skeet.SkeetWidget;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;
public class WSkeetHorizontalSeparator extends WHorizontalSeparator implements SkeetWidget {
    public WSkeetHorizontalSeparator(String text){super(text);}
    @Override protected void onRender(GuiRenderer renderer,double mx,double my,double delta){
        if(text != null) super.onRender(renderer,mx,my,delta);
        else renderer.quad(x,y+height/2,width,1,theme().outlineColor.get());
    }
}
