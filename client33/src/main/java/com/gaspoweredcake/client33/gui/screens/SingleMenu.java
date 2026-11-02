package com.gaspoweredcake.client33.gui.screens;

import com.gaspoweredcake.client33.events.client33.ActiveModulesChangedEvent;
import com.gaspoweredcake.client33.events.client33.ModuleBindChangedEvent;
import com.gaspoweredcake.client33.gui.GuiTheme;
import com.gaspoweredcake.client33.gui.tabs.TabScreen;
import com.gaspoweredcake.client33.gui.tabs.Tabs;
import com.gaspoweredcake.client33.gui.widgets.WKeybind;
import com.gaspoweredcake.client33.gui.widgets.WWidget;
import com.gaspoweredcake.client33.gui.widgets.WVisualPreview;
import com.gaspoweredcake.client33.gui.widgets.containers.WContainer;
import com.gaspoweredcake.client33.gui.widgets.containers.WHorizontalList;
import com.gaspoweredcake.client33.gui.widgets.containers.WVerticalList;
import com.gaspoweredcake.client33.gui.widgets.containers.WView;
import com.gaspoweredcake.client33.gui.widgets.containers.WWindow;
import com.gaspoweredcake.client33.gui.widgets.input.WDropdown;
import com.gaspoweredcake.client33.gui.widgets.input.WTextBox;
import com.gaspoweredcake.client33.gui.widgets.pressable.WButton;
import com.gaspoweredcake.client33.gui.widgets.pressable.WCheckbox;
import com.gaspoweredcake.client33.gui.widgets.pressable.WFavorite;
import com.gaspoweredcake.client33.settings.Setting;
import com.gaspoweredcake.client33.systems.config.Config;
import com.gaspoweredcake.client33.systems.modules.Category;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.render.ESP;
import com.gaspoweredcake.client33.systems.modules.render.CombatVisuals;
import com.gaspoweredcake.client33.utils.misc.NbtUtils;
import meteordevelopment.orbit.EventHandler;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import static com.gaspoweredcake.client33.utils.Utils.getWindowHeight;
import static com.gaspoweredcake.client33.utils.Utils.getWindowWidth;

/** Compact, single module menu using 33's own widgets. */
public final class SingleMenu extends TabScreen {
    private WVerticalList rows, settingsContainer;
    private final Map<Module, WCheckbox> toggles = new IdentityHashMap<>();
    private Module selected;
    private Module bindingModule;
    private WKeybind keybind;
    private String query = "", category = "All";

    public SingleMenu(GuiTheme theme) { super(theme, Tabs.get().getFirst()); }

    @Override public void initWidgets() {
        cancelBinding();
        WWindow window = add(theme.window("33  /  MODULES")).top().padLeft(8).marginTop(38).widget();
        window.padding = 5;
        window.view.maxHeight = Math.max(theme.scale(100), getWindowHeight() - theme.scale(90));
        double width = Math.max(200, Math.min(420, getWindowWidth() / theme.scale(1) - 35));
        WTextBox search = window.add(theme.textBox(query, "Search modules and settings")).minWidth(width).expandX().widget();
        search.action = () -> { query = search.get(); selected = null; refresh(); };
        List<Category> categories = java.util.stream.StreamSupport.stream(Modules.loopCategories().spliterator(), false).toList();
        String[] names = new String[categories.size() + 3];
        names[0] = "All"; names[1] = "Favorites"; names[2] = "Enabled";
        for (int i = 0; i < categories.size(); i++) names[i + 3] = categories.get(i).name;
        if (java.util.Arrays.stream(names).noneMatch(category::equals)) category = "All";
        WDropdown<String> picker = window.add(theme.dropdown(names, category)).expandX().widget();
        picker.action = () -> { category = picker.get(); selected = null; refresh(); };
        WView view = window.add(theme.view()).minWidth(width).expandX().widget();
        view.maxHeight = Math.max(theme.scale(80), getWindowHeight() - theme.scale(180));
        rows = view.add(theme.verticalList()).expandX().widget();
        refresh();
    }

    private void refresh() {
        if (rows == null) return;
        cancelBinding();
        rows.clear(); toggles.clear(); settingsContainer = null;
        String filter = query.strip().toLowerCase(Locale.ROOT);
        List<Module> modules = Modules.get().getAll().stream()
            .filter(module -> !Config.get().hiddenModules.get().contains(module))
            .filter(module -> category.equals("All") || category.equals("Favorites") && module.favorite
                || category.equals("Enabled") && module.isActive() || module.category.name.equals(category))
            .filter(module -> matches(module, filter))
            .sorted(Comparator.comparing(module -> module.title, String.CASE_INSENSITIVE_ORDER)).toList();
        if (modules.isEmpty()) rows.add(theme.label("No matching modules."));
        for (Module module : modules) {
            WHorizontalList row = rows.add(theme.horizontalList()).expandX().widget();
            WCheckbox enabled = row.add(theme.checkbox(module.isActive())).widget();
            toggles.put(module, enabled);
            enabled.action = () -> { if (module.isActive() != enabled.checked) module.toggle(); };
            WButton detail = row.add(theme.button((selected == module ? "−  " : "+  ") + module.title)).expandX().widget();
            detail.tooltip = module.description;
            detail.action = () -> { selected = selected == module ? null : module; refresh(); };
            WFavorite favorite = row.add(theme.favorite(module.favorite)).widget();
            favorite.tooltip = "Favorite";
            favorite.action = () -> { module.favorite = favorite.checked; if (category.equals("Favorites")) taskAfterRender = this::refresh; };
            if (selected == module) addDetails(module);
        }
    }

    private void addDetails(Module module) {
        WVerticalList detail = rows.add(theme.verticalList()).expandX().padHorizontal(12).widget();
        detail.add(theme.label(module.description, 380)).expandX();
        if (module instanceof ESP || module instanceof CombatVisuals) detail.add(new WVisualPreview(module instanceof CombatVisuals));
        settingsContainer = detail.add(theme.verticalList()).expandX().widget();
        settingsContainer.add(theme.settings(module.settings)).expandX();
        WWidget widget = module.getWidget(theme);
        if (widget != null) {
            if (widget instanceof WContainer) detail.add(widget).expandX();
            else detail.add(widget);
        }
        WHorizontalList binding = detail.add(theme.horizontalList()).expandX().widget();
        binding.add(theme.label("Bind"));
        keybind = binding.add(theme.keybind(module.keybind)).expandX().widget();
        keybind.actionOnSet = () -> { bindingModule = module; Modules.get().setModuleToBind(module); };
        keybind.action = () -> bindingModule = null;
        WButton clear = binding.add(theme.button("Clear")).widget();
        clear.action = keybind::resetBind;
        WCheckbox feedback = binding.add(theme.checkbox(module.chatFeedback)).widget();
        feedback.tooltip = "Chat feedback";
        feedback.action = () -> module.chatFeedback = feedback.checked;
        WHorizontalList release = detail.add(theme.horizontalList()).expandX().widget();
        release.add(theme.label("Toggle on bind release"));
        WCheckbox onRelease = release.add(theme.checkbox(module.toggleOnBindRelease)).widget();
        onRelease.action = () -> module.toggleOnBindRelease = onRelease.checked;
    }

    private boolean matches(Module module, String text) {
        if (text.isEmpty() || (module.title + " " + module.name + " " + module.description).toLowerCase(Locale.ROOT).contains(text)) return true;
        for (var group : module.settings) for (Setting<?> setting : group)
            if (setting.title.toLowerCase(Locale.ROOT).contains(text)) return true;
        return false;
    }

    @Override public void tick() {
        super.tick();
        if (selected != null && settingsContainer != null) selected.settings.tick(settingsContainer, theme);
    }

    @EventHandler private void onActiveModulesChanged(ActiveModulesChangedEvent event) {
        if (category.equals("Enabled")) taskAfterRender = this::refresh;
        else toggles.forEach((module, checkbox) -> checkbox.checked = module.isActive());
    }

    @EventHandler private void onModuleBindChanged(ModuleBindChangedEvent event) {
        if (keybind != null && selected == event.module) keybind.reset();
    }

    private void cancelBinding() {
        if (keybind != null && bindingModule != null) keybind.reset();
        bindingModule = null;
        keybind = null;
    }

    @Override protected void onClosed() { cancelBinding(); }

    @Override public void reload() { super.reload(); addDirect(theme.topBar()).top().centerX(); }
    @Override public void resize(int width, int height) { super.resize(width, height); reload(); }
    @Override public boolean shouldCloseOnEsc() { return !Modules.get().isBinding(); }
    @Override public boolean toClipboard() { return NbtUtils.toClipboard(Modules.get()); }
    @Override public boolean fromClipboard() { boolean imported = NbtUtils.fromClipboard(Modules.get()); refresh(); return imported; }
}
