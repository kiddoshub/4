
package com.gaspoweredcake.client33.gui.screens;

import com.mojang.blaze3d.platform.MacosUtil;
import com.gaspoweredcake.client33.events.client33.ActiveModulesChangedEvent;
import com.gaspoweredcake.client33.gui.GuiTheme;
import com.gaspoweredcake.client33.gui.utils.WorkspacePage;
import com.gaspoweredcake.client33.gui.tabs.TabScreen;
import com.gaspoweredcake.client33.gui.tabs.Tabs;
import com.gaspoweredcake.client33.gui.widgets.WLabel;
import com.gaspoweredcake.client33.gui.widgets.WVisualPreview;
import com.gaspoweredcake.client33.gui.widgets.input.WDropdown;
import com.gaspoweredcake.client33.gui.widgets.pressable.WCheckbox;
import com.gaspoweredcake.client33.systems.modules.render.ESP;
import com.gaspoweredcake.client33.systems.modules.render.CombatVisuals;
import com.gaspoweredcake.client33.gui.widgets.containers.*;
import com.gaspoweredcake.client33.gui.widgets.input.WTextBox;
import com.gaspoweredcake.client33.gui.widgets.pressable.WButton;
import com.gaspoweredcake.client33.gui.widgets.pressable.WFavorite;
import com.gaspoweredcake.client33.settings.Setting;
import com.gaspoweredcake.client33.settings.SettingGroup;
import com.gaspoweredcake.client33.systems.config.Config;
import com.gaspoweredcake.client33.systems.modules.Category;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.utils.misc.NbtUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.input.KeyEvent;
import org.jspecify.annotations.NonNull;

import java.util.Comparator;
import java.util.Locale;

import static com.gaspoweredcake.client33.Client33.mc;
import static com.gaspoweredcake.client33.utils.Utils.getWindowHeight;
import static com.gaspoweredcake.client33.utils.Utils.getWindowWidth;
import static com.mojang.blaze3d.platform.InputConstants.*;

/** A single workspace for discovering, configuring and switching modules. */
public class ModulesScreen extends TabScreen {
    private WTextBox search;
    private WView results;
    private WVerticalList navigation;
    private WLabel summary;
    private Category category;
    private Filter filter = Filter.All;
    private int page;
    private WLabel pageLabel;
    private WButton previousPage, nextPage;
    private String query = "";

    public ModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());
    }

    @Override
    public void initWidgets() {
        WWindow window = add(theme.window("33  /  WORKSPACE")).centerX().top().marginTop(36).widget();
        window.padding = 10;
        window.view.spacing = 8;

        WHorizontalList body = window.add(theme.horizontalList()).expandX().widget();
        body.spacing = 12;
        WView sidebar = body.add(theme.view()).top().widget();
        sidebar.maxHeight = Math.max(theme.scale(90), getWindowHeight() - theme.scale(140));
        navigation = sidebar.add(theme.verticalList()).minWidth(125).expandX().widget();
        navigation.spacing = 4;
        refreshNavigation();

        WVerticalList content = body.add(theme.verticalList()).top().expandX().widget();
        content.spacing = 8;
        WHorizontalList toolbar = content.add(theme.horizontalList()).expandX().widget();
        search = toolbar.add(theme.textBox(query, "Search modules and settings  /  Ctrl+F")).minWidth(200).expandX().widget();
        search.action = () -> { query = search.get(); page=0; refreshResults(); };
        toolbar.add(theme.button("Clear")).widget().action = () -> {
            search.set(""); query = ""; page=0; search.setFocused(true); refreshResults();
        };
        toolbar.add(theme.button("Strata workspace")).widget().action = StrataWorkspace::open;
        summary = content.add(theme.label("")).padTop(4).widget();
        results = content.add(theme.view()).minWidth(Math.max(240, Math.min(1100, getWindowWidth() / theme.scale(1) - 240))).expandX().widget();
        results.maxHeight = Math.max(theme.scale(70), sidebar.maxHeight - theme.scale(108));
        results.spacing = 10;
        WHorizontalList pager=content.add(theme.horizontalList()).expandX().widget();
        previousPage=pager.add(theme.button("Previous")).widget();
        pageLabel=pager.add(theme.label("")).expandCellX().widget();
        nextPage=pager.add(theme.button("Next")).widget();
        previousPage.action=()->{page--;refreshResults();};
        nextPage.action=()->{page++;refreshResults();};
        refreshResults();

        if (theme.modulesHelpText()) {
            window.add(theme.label("Click to toggle  /  Configure to edit  /  Ctrl+F to search")
                .color(theme.textSecondaryColor()));
        }
    }

    @Override
    protected void init() {
        boolean alreadyBuilt = results != null;
        super.init();
        if (alreadyBuilt) refreshResults();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        reload();
    }

    @Override
    public void reload() {
        super.reload();
        // Tab.createScreen adds this outside initWidgets; a rebuilt root must restore it.
        addDirect(theme.topBar()).top().centerX();
    }

    private void refreshNavigation() {
        navigation.clear();
        navigation.add(theme.label("33  /  MENU")).padBottom(7);
        navigation.add(theme.label("WORKSPACE").color(theme.textSecondaryColor())).padTop(4);
        for (Filter value : Filter.values()) {
            WButton button = navigation.add(theme.button((category == null && filter == value ? ">  " : "    ") + value)).expandX().widget();
            button.action = () -> select(null, value);
        }
        navigation.add(theme.label("CATEGORIES").color(theme.textSecondaryColor())).padTop(10);
        for (Category value : Modules.loopCategories()) {
            navigation.add(theme.button((category == value ? ">  " : "    ") + value.name)).expandX().widget().action = () -> select(value, Filter.All);
        }

    }

    private void select(Category category, Filter filter) {
        this.category = category;
        this.filter = filter;
        page=0;
        refreshNavigation();
        refreshResults();
    }

    private void refreshResults() {
        if (results == null) return;
        results.clear();
        pageLabel.set("");
        previousPage.action=()->{}; nextPage.action=()->{};
        String text = query.strip().toLowerCase(Locale.ROOT);
        if (filter == Filter.Home && category == null && text.isEmpty()) {
            summary.set("OVERVIEW  /  Your workspace");
            buildHome();
            return;
        }
        if (filter == Filter.Studio && category == null && text.isEmpty()) {
            summary.set("VISUAL STUDIO  /  Build your signature");
            buildStudio();
            return;
        }
        var modules = Modules.get().getAll().stream()
            .filter(module -> !Config.get().hiddenModules.get().contains(module))
            .filter(module -> category == null || module.category == category)
            .filter(module -> filter != Filter.Favorites || module.favorite)
            .filter(module -> filter != Filter.Enabled || module.isActive())
            .filter(module -> matches(module, text))
            .sorted(Comparator.comparingInt((Module module) -> searchRank(module, text))
                .thenComparing(module -> module.title, String.CASE_INSENSITIVE_ORDER))
            .toList();
        summary.set((category == null ? filter.toString() : category.name) + "  /  " + modules.size() + " modules");

        if (modules.isEmpty()) {
            results.add(theme.label("No modules found.")).pad(8);
            results.add(theme.label("Try another category or clear your search.").color(theme.textSecondaryColor()));
            return;
        }

        WTable grid = results.add(theme.table()).expandX().widget();
        grid.horizontalSpacing = 10;
        grid.verticalSpacing = 10;
        WorkspacePage layout=WorkspacePage.layout(Math.max(240,getWindowWidth()/theme.scale(1)-240),results.maxHeight/theme.scale(1),modules.size(),page);
        page=layout.page();
        pageLabel.set("Page "+(page+1)+" / "+layout.pages());
        previousPage.action=()->{page=Math.max(0,page-1);refreshResults();};
        nextPage.action=()->{page=Math.min(layout.pages()-1,page+1);refreshResults();};
        int columns=layout.columns();
        int index = 0;
        for (Module module : modules.subList(layout.from(),layout.to())) {
            WPanel panel = grid.add(new WPanel()).expandX().top().widget();
            panel.tooltip=module.description;
            panel.add(theme.module(module)).expandX().padHorizontal(7).padTop(3);

            WHorizontalList row = panel.add(theme.horizontalList()).expandX().pad(8).widget();
            WFavorite favorite = row.add(theme.favorite(module.favorite)).widget();
            favorite.tooltip = "Add to favorites";
            favorite.action = () -> {
                module.favorite = favorite.checked;
                if (filter == Filter.Favorites) taskAfterRender = this::refreshResults;
            };
            row.add(theme.button("Configure")).expandX().widget().action = () -> mc.gui.setScreen(theme.moduleScreen(module));
            if (++index % columns == 0) grid.row();
        }
    }

    private void buildHome() {
        var visible = Modules.get().getAll().stream().filter(module -> !Config.get().hiddenModules.get().contains(module)).toList();
        long enabled = visible.stream().filter(Module::isActive).count();
        long favorites = visible.stream().filter(module -> module.favorite).count();
        WPanel welcome = results.add(new WPanel()).expandX().widget();
        welcome.add(theme.label("YOUR CLIENT, AT A GLANCE")).padHorizontal(16).padTop(15);
        welcome.add(theme.label("Find your tools, tune your visuals and get back in game.", 550).color(theme.textSecondaryColor())).padHorizontal(16).padVertical(8);
        WHorizontalList actions = welcome.add(theme.horizontalList()).expandX().padHorizontal(16).padBottom(14).widget();
        actions.add(theme.button("Browse modules  >")).expandX().widget().action = () -> select(null, Filter.All);
        actions.add(theme.button("Visual studio  >")).expandX().widget().action = () -> select(null, Filter.Studio);

        WTable stats = results.add(theme.table()).expandX().widget();
        stats.horizontalSpacing = 10;
        WPanel library = stats.add(new WPanel()).expandX().widget();
        library.add(theme.label("LIBRARY").color(theme.textSecondaryColor())).padHorizontal(12).padTop(12);
        library.add(theme.label(visible.size() + " modules")).padHorizontal(12).padVertical(8);
        library.add(theme.button("Explore")).expandX().padHorizontal(12).padBottom(12).widget().action = () -> select(null, Filter.All);
        if (getWindowWidth() / theme.scale(1) < 700) stats.row();
        WPanel active = stats.add(new WPanel()).expandX().widget();
        active.add(theme.label("ACTIVE").color(theme.textSecondaryColor())).padHorizontal(12).padTop(12);
        active.add(theme.label(enabled + " enabled")).padHorizontal(12).padVertical(8);
        active.add(theme.button("Manage")).expandX().padHorizontal(12).padBottom(12).widget().action = () -> select(null, Filter.Enabled);
        if (getWindowWidth() / theme.scale(1) < 700) stats.row();
        WPanel starred = stats.add(new WPanel()).expandX().widget();
        starred.add(theme.label("SAVED").color(theme.textSecondaryColor())).padHorizontal(12).padTop(12);
        starred.add(theme.label(favorites + " favorites")).padHorizontal(12).padVertical(8);
        starred.add(theme.button("Open")).expandX().padHorizontal(12).padBottom(12).widget().action = () -> select(null, Filter.Favorites);

        WPanel shortcuts = results.add(new WPanel()).expandX().widget();
        shortcuts.add(theme.label("QUICK CONTROLS")).padHorizontal(14).padTop(12);
        shortcuts.add(theme.label("Click to toggle / right-click to configure", 450).color(theme.textSecondaryColor())).padHorizontal(14).padBottom(6);
        for (String name : new String[]{"esp", "chams", "combat-visuals", "kill-aura"}) {
            Module module = Modules.get().get(name);
            if (module != null && !Config.get().hiddenModules.get().contains(module)) shortcuts.add(theme.module(module)).expandX().padHorizontal(10);
        }
        shortcuts.add(theme.label(" ")).padBottom(4);
    }

    private void buildStudio() {
        ESP esp = Modules.get().get(ESP.class);
        CombatVisuals combat = Modules.get().get(CombatVisuals.class);
        WTable grid = results.add(theme.table()).expandX().widget();
        grid.horizontalSpacing = 12;
        grid.verticalSpacing = 12;
        boolean wide = getWindowWidth() / theme.scale(1) >= 950;
        WPanel entity = studioCard(grid, esp, "01 / ENTITY SIGNATURE", false);
        entity.add(theme.label("ESP MODE").color(theme.textSecondaryColor())).padHorizontal(12).padBottom(4);
        WDropdown<ESP.Mode> mode = entity.add(theme.dropdown(esp.mode.get())).expandX().padHorizontal(12).widget();
        mode.action = () -> {
            esp.mode.set(mode.get());
            taskAfterRender = this::refreshResults;
        };
        boolean signature = esp.mode.get() == ESP.Mode.Hologram || esp.mode.get() == ESP.Mode.Constellation;
        boolean screen = esp.mode.get() == ESP.Mode._2D || esp.mode.get() == ESP.Mode.Telemetry;
        if (signature || esp.isSurfaceMode() || esp.mode.get() == ESP.Mode.Telemetry) toggle(entity, "Animated scans", esp.animate);
        if (esp.isSurfaceMode()) toggle(entity,"Through walls",esp.throughWalls);
        if (signature) {
            toggle(entity, "Ground markers", esp.groundMarker);
            toggle(entity, "Through walls", esp.throughWalls);
            toggle(entity, "Target tether", esp.tether);
        }
        if (screen) {
            entity.add(theme.label("NAMES").color(theme.textSecondaryColor())).padHorizontal(12).padTop(4);
            WDropdown<ESP.NameLabel> names = entity.add(theme.dropdown(esp.nameLabel.get())).expandX().padHorizontal(12).widget();
            names.action = () -> esp.nameLabel.set(names.get());
            toggle(entity, "Health bars", esp.healthBar);
            toggle(entity, "Armor bars", esp.armorBar);
            toggle(entity, "Health labels", esp.healthLabel);
            toggle(entity, "Distance labels", esp.distanceLabel);
        }
        toggle(entity, "Ignore friends", esp.ignoreFriends);
        toggle(entity, "Ignore invisible", esp.ignoreInvisible);
        Module tracers = Modules.get().get("tracers");
        if (tracers != null) entity.add(theme.button("Tracer & offscreen settings")).expandX().padHorizontal(12).padTop(8).widget().action = () -> mc.gui.setScreen(theme.moduleScreen(tracers));
        entity.add(theme.button("All ESP settings")).expandX().pad(12).widget().action = () -> mc.gui.setScreen(theme.moduleScreen(esp));
        if (!wide) grid.row();
        WPanel effects = studioCard(grid, combat, "02 / COMBAT CHOREOGRAPHY", true);
        toggle(effects, "Third-person camera", combat.thirdPerson);
        toggle(effects, "Attack bursts", combat.attackBursts);
        toggle(effects, "Movement ribbon", combat.trails);
        toggle(effects, "Target crown", combat.crown);
        effects.add(theme.button("All combat visuals")).expandX().pad(12).widget().action = () -> mc.gui.setScreen(theme.moduleScreen(combat));
        grid.row();
        WPanel looks = grid.add(new WPanel()).expandX().top().widget();
        looks.add(theme.label("03 / LOOK PRESETS")).pad(12);
        for (String name : new String[]{"Crimson hologram", "Arctic constellation", "Minimal telemetry"}) {
            looks.add(theme.button(name)).expandX().padHorizontal(12).padBottom(6).widget().action = () -> {
                boolean minimal = name.startsWith("Minimal"), arctic = name.startsWith("Arctic");
                esp.mode.set(minimal ? ESP.Mode.Telemetry : arctic ? ESP.Mode.Constellation : ESP.Mode.Hologram);
                esp.animate.set(!minimal);
                combat.targetOrbit.set(!minimal); combat.scanBand.set(!minimal); combat.crown.set(!minimal);
                combat.attackBursts.set(!minimal); combat.sparks.set(!minimal); combat.trails.set(!minimal);
                combat.primary.set(new com.gaspoweredcake.client33.utils.render.color.SettingColor(arctic ? 92 : 255, arctic ? 220 : 65, arctic ? 255 : 95, 220));
                esp.signatureAccent.set(new com.gaspoweredcake.client33.utils.render.color.SettingColor(combat.primary.get()));
                taskAfterRender = this::refreshResults;
            };
        }
        looks.add(theme.label("Changes appearance only. Enable above.", 280).color(theme.textSecondaryColor())).pad(12);
        if (!wide) grid.row();
        WPanel scene = grid.add(new WPanel()).expandX().top().widget();
        scene.add(theme.label("04 / SCENE & CAMERA")).pad(12);
        for (String name : new String[]{"ambience", "fullbright", "camera-tweaks", "free-look", "hand-view"}) {
            Module module = Modules.get().get(name);
            if (module != null) scene.add(theme.button(module.title + "  >")).expandX().padHorizontal(12).padBottom(6).widget().action = () -> mc.gui.setScreen(theme.moduleScreen(module));
        }
        scene.add(theme.label("Fine-tune your world, camera and hands.", 280).color(theme.textSecondaryColor())).pad(12);
        grid.row();
        WPanel recon = grid.add(new WPanel()).expandX().top().widget();
        recon.add(theme.label("05 / WORLD INTELLIGENCE")).pad(12);
        for (String name : new String[]{"storage-esp", "faction-intel", "movement-heatmap", "structure-esp"}) {
            Module module = Modules.get().get(name);
            recon.add(theme.button(module.title + "  >")).expandX().padHorizontal(12).padBottom(6).widget().action = () -> mc.gui.setScreen(theme.moduleScreen(module));
        }
        recon.add(theme.label("Loaded data, source labels and expiring observations.", 280).color(theme.textSecondaryColor())).pad(12);
        if (!wide) grid.row();
        WPanel combatTools = grid.add(new WPanel()).expandX().top().widget();
        combatTools.add(theme.label("06 / PVP CONTROL")).pad(12);
        for (String name : new String[]{"kill-aura", "shield-predict", "crystal-aura", "auto-totem"}) {
            Module module = Modules.get().get(name);
            combatTools.add(theme.button(module.title + "  >")).expandX().padHorizontal(12).padBottom(6).widget().action = () -> mc.gui.setScreen(theme.moduleScreen(module));
        }
        combatTools.add(theme.label("Server timing, shield estimates and explosion risk controls.", 280).color(theme.textSecondaryColor())).pad(12);
    }

    private WPanel studioCard(WTable grid, Module module, String title, boolean combat) {
        WPanel card = grid.add(new WPanel()).expandX().top().widget();
        card.add(theme.label(title)).pad(12);
        card.add(new WVisualPreview(combat)).expandX().padHorizontal(8);
        card.add(theme.module(module)).expandX().pad(8);
        return card;
    }

    private void toggle(WPanel card, String title, Setting<Boolean> setting) {
        WHorizontalList row = card.add(theme.horizontalList()).expandX().padHorizontal(12).padVertical(4).widget();
        row.add(theme.label(title)).expandCellX();
        WCheckbox checkbox = row.add(theme.checkbox(setting.get())).widget();
        checkbox.action = () -> setting.set(checkbox.checked);
        checkbox.tooltip = setting.description;
    }

    private int searchRank(Module module, String query) {
        String title = module.title.toLowerCase(Locale.ROOT);
        if (title.equals(query) || module.name.equalsIgnoreCase(query)) return 0;
        if (title.startsWith(query)) return 1;
        if (title.contains(query)) return 2;
        return 3;
    }

    private boolean matches(Module module, String query) {
        if (query.isEmpty()) return true;
        if ((module.title + " " + module.name + " " + module.description).toLowerCase(Locale.ROOT).contains(query)) return true;
        for (SettingGroup group : module.settings) {
            for (Setting<?> setting : group) {
                if (setting.title.toLowerCase(Locale.ROOT).contains(query)) return true;
            }
        }
        return false;
    }

    @EventHandler
    private void onActiveModulesChanged(ActiveModulesChangedEvent event) {
        if (filter == Filter.Enabled || filter == Filter.Home) taskAfterRender = this::refreshResults;
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent value) {
        if (locked) return false;
        int modifier = MacosUtil.IS_MACOS ? MOD_SUPER : MOD_CONTROL;
        if ((value.modifiers() & modifier) != 0 && value.key() == KEY_F) {
            search.setFocused(true);
            search.setCursorMax();
            return true;
        }
        return super.keyPressed(value);
    }

    @Override
    public boolean toClipboard() {
        return NbtUtils.toClipboard(Modules.get());
    }

    @Override
    public boolean fromClipboard() {
        boolean imported = NbtUtils.fromClipboard(Modules.get());
        refreshResults();
        return imported;
    }

    private enum Filter { Home, Studio, All, Favorites, Enabled }
}
