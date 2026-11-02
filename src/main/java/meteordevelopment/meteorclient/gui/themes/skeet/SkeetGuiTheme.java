
package meteordevelopment.meteorclient.gui.themes.skeet;

import meteordevelopment.meteorclient.gui.DefaultSettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.themes.skeet.widgets.*;
import meteordevelopment.meteorclient.gui.themes.skeet.widgets.input.WSkeetDropdown;
import meteordevelopment.meteorclient.gui.themes.skeet.widgets.input.WSkeetSlider;
import meteordevelopment.meteorclient.gui.themes.skeet.widgets.input.WSkeetTextBox;
import meteordevelopment.meteorclient.gui.themes.skeet.widgets.pressable.*;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.*;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.*;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class SkeetGuiTheme extends GuiTheme {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");
    private final SettingGroup sgTextColors = settings.createGroup("Text");

    public final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Scale of the GUI.")
        .defaultValue(1)
        .min(0.75)
        .sliderRange(0.75, 4)
        .onSliderRelease()
        .onChanged(aDouble -> {
            if (mc.currentScreen instanceof WidgetScreen) ((WidgetScreen) mc.currentScreen).invalidate();
        })
        .build()
    );

    public final Setting<AlignmentX> moduleAlignment = sgGeneral.add(new EnumSetting.Builder<AlignmentX>()
        .name("module-alignment")
        .description("How module titles are aligned.")
        .defaultValue(AlignmentX.Left)
        .build()
    );

    public final Setting<Boolean> categoryIcons = sgGeneral.add(new BoolSetting.Builder()
        .name("category-icons")
        .description("Adds item icons to module categories.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> hideHUD = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-HUD")
        .description("Hide HUD when in GUI.")
        .defaultValue(false)
        .onChanged(v -> {
            if (mc.currentScreen instanceof WidgetScreen) mc.options.hudHidden = v;
        })
        .build()
    );

    public final Setting<SettingColor> accentColor = color("accent", "Accent color of the GUI.", new SettingColor(255, 255, 255));
    public final Setting<SettingColor> checkboxColor = color("checkbox", "Color of checkbox.", new SettingColor(100, 150, 255));
    public final Setting<SettingColor> plusColor = color("plus", "Color of plus button.", new SettingColor(50, 255, 50));
    public final Setting<SettingColor> minusColor = color("minus", "Color of minus button.", new SettingColor(255, 50, 50));
    public final Setting<SettingColor> favoriteColor = color("favorite", "Color of checked favorite button.", new SettingColor(250, 215, 0));

    public final Setting<SettingColor> textColor = color(sgTextColors, "text", "Color of text.", new SettingColor(255, 255, 255));
    public final Setting<SettingColor> textSecondaryColor = color(sgTextColors, "text-secondary", "Color of secondary text.", new SettingColor(150, 150, 150));
    public final Setting<SettingColor> textHighlightColor = color(sgTextColors, "text-highlight", "Color of text highlighting.", new SettingColor(100, 150, 255, 100));
    public final Setting<SettingColor> titleTextColor = color(sgTextColors, "title-text", "Color of title text.", new SettingColor(255, 255, 255));

    public final Setting<SettingColor> backgroundColor = color("background", "Background color.", new SettingColor(16, 16, 16));
    public final Setting<SettingColor> moduleBackground = color("module-background", "Color of module background when active.", new SettingColor(30, 30, 30));
    public final Setting<SettingColor> outlineColor = color("outline", "Outline color.", new SettingColor(45, 45, 45));
    public final Setting<SettingColor> borderColor = color("border", "Border color.", new SettingColor(51, 51, 51));

    public SkeetGuiTheme() {
        super("Skeet");
        settingsFactory = new DefaultSettingsWidgetFactory(this);
    }

    private Setting<SettingColor> color(SettingGroup group, String name, String description, SettingColor color) {
        return group.add(new ColorSetting.Builder()
            .name(name + "-color")
            .description(description)
            .defaultValue(color)
            .build());
    }
    private Setting<SettingColor> color(String name, String description, SettingColor color) {
        return color(sgColors, name, description, color);
    }

    @Override
    public WWindow window(WWidget icon, String title) {
        return w(new WSkeetWindow(icon, title));
    }

    @Override
    public WLabel label(String text, boolean title, double maxWidth) {
        return w(new WSkeetLabel(text, title, maxWidth));
    }

    @Override
    public WHorizontalSeparator horizontalSeparator(String text) {
        return w(new WSkeetHorizontalSeparator(text));
    }

    @Override
    public WVerticalSeparator verticalSeparator() {
        return w(new WSkeetVerticalSeparator());
    }

    @Override
    protected WButton button(String text, GuiTexture texture) {
        return w(new WSkeetButton(text, texture));
    }

    @Override
    protected WConfirmedButton confirmedButton(String text, String confirmText, GuiTexture texture) {
        return w(new WSkeetConfirmedButton(text, confirmText, texture));
    }

    @Override
    public WMinus minus() {
        return w(new WSkeetMinus());
    }

    @Override
    public WConfirmedMinus confirmedMinus() {
        return w(new WSkeetConfirmedMinus());
    }

    @Override
    public WPlus plus() {
        return w(new WSkeetPlus());
    }

    @Override
    public WCheckbox checkbox(boolean checked) {
        return w(new WSkeetCheckbox(checked));
    }

    @Override
    public WSlider slider(double value, double min, double max) {
        return w(new WSkeetSlider(value, min, max));
    }

    @Override
    public WTextBox textBox(String text, String placeholder, CharFilter filter, Class<? extends WTextBox.Renderer> renderer) {
        return w(new WSkeetTextBox(text, placeholder, filter, renderer));
    }

    @Override
    public <T> WDropdown<T> dropdown(T[] values, T value) {
        return w(new WSkeetDropdown<>(values, value));
    }

    @Override
    public WTriangle triangle() {
        return w(new WSkeetTriangle());
    }

    @Override
    public WTooltip tooltip(String text) {
        return w(new WSkeetTooltip(text));
    }

    @Override
    public WView view() {
        return w(new WSkeetView());
    }

    @Override
    public WSection section(String title, boolean expanded, WWidget headerWidget) {
        return w(new WSkeetSection(title, expanded, headerWidget));
    }

    @Override
    public WAccount account(WidgetScreen screen, Account<?> account) {
        return w(new WSkeetAccount(screen, account));
    }

    @Override
    public WWidget module(Module module) {
        return w(new WSkeetModule(module));
    }

    @Override
    public WQuad quad(Color color) {
        return w(new WSkeetQuad(color));
    }

    @Override
    public WTopBar topBar() {
        return w(new WSkeetTopBar());
    }

    @Override
    public WFavorite favorite(boolean checked) {
        return w(new WSkeetFavorite(checked));
    }

    @Override
    public Color textColor() {
        return textColor.get();
    }

    @Override
    public Color textSecondaryColor() {
        return textSecondaryColor.get();
    }

    @Override
    public Color starscriptTextColor() {
        return textColor.get();
    }

    @Override
    public Color starscriptBraceColor() {
        return textSecondaryColor.get();
    }

    @Override
    public Color starscriptParenthesisColor() {
        return textColor.get();
    }

    @Override
    public Color starscriptDotColor() {
        return textColor.get();
    }

    @Override
    public Color starscriptCommaColor() {
        return textColor.get();
    }

    @Override
    public Color starscriptOperatorColor() {
        return textColor.get();
    }

    @Override
    public Color starscriptStringColor() {
        return new SettingColor(106, 135, 89);
    }

    @Override
    public Color starscriptNumberColor() {
        return new SettingColor(104, 141, 187);
    }

    @Override
    public Color starscriptKeywordColor() {
        return new SettingColor(204, 120, 50);
    }

    @Override
    public Color starscriptAccessedObjectColor() {
        return new SettingColor(152, 118, 170);
    }
}
