
package meteordevelopment.meteorclient.gui.themes.skeet.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.skeet.SkeetGuiTheme;
import meteordevelopment.meteorclient.gui.themes.skeet.SkeetWidget;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.util.math.MathHelper;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;

public class WSkeetModule extends WPressable implements SkeetWidget {
    private final Module module;
    private double titleWidth;
    private double animationProgress;

    public WSkeetModule(Module module) {
        this.module = module;
        this.tooltip = module.description;
        if (module.isActive()) animationProgress = 1;
    }

    @Override
    public double pad() {
        return theme.scale(4);
    }

    @Override
    protected void onCalculateSize() {
        double pad = pad();
        if (titleWidth == 0) titleWidth = theme.textWidth(module.title);
        width = pad + titleWidth + pad;
        height = pad + theme.textHeight() + pad;
    }

    @Override
    protected void onPressed(int button) {
        if (button == GLFW_MOUSE_BUTTON_LEFT) module.toggle();
        else if (button == GLFW_MOUSE_BUTTON_RIGHT) mc.setScreen(theme.moduleScreen(module));
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        SkeetGuiTheme theme = theme();
        double pad = pad();

        animationProgress += delta * 6 * (module.isActive() ? 1 : -1);
        animationProgress = MathHelper.clamp(animationProgress, 0, 1);

        double s = 0.5;
        renderer.quad(x, y, width, height, theme.backgroundColor.get());
        if (animationProgress > 0) {
            renderer.quad(x, y, theme.scale(2), height, theme.accentColor.get().a((int)(255 * animationProgress)));
        }
        if (mouseOver && animationProgress < 1) {
            renderer.quad(x, y, theme.scale(2), height, theme.textSecondaryColor.get().a(120));
        }

        renderer.quad(x, y, width, s, theme.outlineColor.get());
        renderer.quad(x, y + height - s, width, s, theme.outlineColor.get());
        renderer.quad(x, y, s, height, theme.outlineColor.get());
        renderer.quad(x + width - s, y, s, height, theme.outlineColor.get());

        double x = this.x + pad + theme.scale(2);
        double w = width - pad * 2 - theme.scale(2);
        if (theme.moduleAlignment.get() == AlignmentX.Center) {
            x += w / 2 - titleWidth / 2;
        } else if (theme.moduleAlignment.get() == AlignmentX.Right) {
            x += w - titleWidth;
        }

        renderer.text(module.title, x, y + pad, module.isActive() ? theme.textColor.get() : theme.textSecondaryColor.get(), false);
    }
}
