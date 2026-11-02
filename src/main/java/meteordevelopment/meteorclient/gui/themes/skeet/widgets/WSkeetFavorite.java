package meteordevelopment.meteorclient.gui.themes.skeet.widgets;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.skeet.SkeetWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WFavorite;
public class WSkeetFavorite extends WFavorite implements SkeetWidget {
    public WSkeetFavorite(boolean checked){super(checked);}
    @Override protected void onRender(GuiRenderer renderer,double mx,double my,double delta){
        renderer.quad(x,y,width,height, checked ? theme().favoriteColor.get() : theme().textSecondaryColor.get());
    }
}
