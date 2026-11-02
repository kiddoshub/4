package meteordevelopment.meteorclient.gui.themes.skeet.widgets.pressable;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.skeet.SkeetWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WConfirmedButton;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
public class WSkeetConfirmedButton extends WConfirmedButton implements SkeetWidget {
    public WSkeetConfirmedButton(String text, String confirmText, GuiTexture texture){super(text,confirmText,texture);}
    @Override protected void onRender(GuiRenderer renderer,double mx,double my,double delta){
        double s=0.5;renderer.quad(x,y,width,height,theme().backgroundColor.get());
        if(mouseOver)renderer.quad(x,y,theme.scale(2),height,theme().accentColor.get());
        renderer.quad(x,y,width,s,theme().outlineColor.get());renderer.quad(x,y+height-s,width,s,theme().outlineColor.get());
        renderer.quad(x,y,s,height,theme().outlineColor.get());renderer.quad(x+width-s,y,s,height,theme().outlineColor.get());
        if(text!=null)renderer.text(text,x+theme.scale(4),y+theme.scale(4),theme().textColor.get(),false);
    }
}
