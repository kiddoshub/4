package meteordevelopment.meteorclient.gui.themes.skeet.widgets;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.themes.skeet.SkeetWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.accounts.Account;
public class WSkeetAccount extends WPressable implements SkeetWidget {
    public WSkeetAccount(WidgetScreen screen, Account<?> account){}
    @Override protected void onPressed(int button){}
    @Override protected void onRender(meteordevelopment.meteorclient.gui.renderer.GuiRenderer renderer,double mx,double my,double delta){}
}
