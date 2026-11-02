package com.gaspoweredcake.client33.gui.screens;

import com.gaspoweredcake.client33.gui.GuiThemes;
import com.gaspoweredcake.client33.gui.renderer.SkiaPanels;
import com.gaspoweredcake.client33.gui.utils.WorkspacePage;
import com.gaspoweredcake.client33.systems.config.Config;
import com.gaspoweredcake.client33.systems.modules.Category;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.systems.modules.Modules;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.Strata;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.text.TextStylePatch;
import java.util.Comparator;
import java.util.Locale;
import static com.gaspoweredcake.client33.Client33.mc;

/** Strata owns layout, focus, input and scrolling; module settings retain their editors. */
public final class StrataWorkspace implements Screen {
    private Category category;
    private boolean favorites,enabled;
    private int page;
    private String query="";
    private final SkiaPanels panels = new SkiaPanels();
    public static void open() {
        ThemeRef theme = ThemeRef.platform()
            .override(Button.STYLE_NORMAL, Paint.rounded(0xff303b4b, 5))
            .override(Button.STYLE_HOVERED, Paint.rounded(0xff51677b, 5))
            .override(Button.STYLE_PRESSED, Paint.rounded(0xff776448, 5))
            .override(Button.STYLE_DISABLED, Paint.rounded(0xff222b37, 5))
            .override(Button.STYLE_FOCUS, Paint.rounded(0x906caab8, 5))
            .override(TextBox.STYLE_NORMAL, Paint.rounded(0xff131c29, 5))
            .override(TextBox.STYLE_FOCUSED, Paint.rounded(0xff263748, 5));
        Strata.screens().open(new StrataWorkspace(), theme);
    }
    public static boolean isOpen() {
        return mc != null && isScreen(mc.gui.screen());
    }
    public static boolean isScreen(net.minecraft.client.gui.screens.Screen candidate) {
        return candidate instanceof org.inspirenxe.strata.minecraft.StrataScreen screen
            && screen.content() instanceof StrataWorkspace;
    }

    @Override
    public Component<?> build(ComponentFactory factory) {
        var root=factory.stackPanel(Orientation.HORIZONTAL).setSpacing(12).setPadding(12)
            .setBackground(Paint.rounded(0xc010131c,12)).setTextStyle(TextStylePatch.of().size(11).color(0xffeef3ff));
        var navigation=factory.stackPanel().setSpacing(6).setWidth(100);
        root.add(factory.scrollPanel().add(navigation).setWidth(100));
        var content=factory.stackPanel().setSpacing(8);
        root.add(content,1);
        content.add(factory.label("33 / MODULE LIBRARY"));
        var search=factory.textBox().setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP).setPlaceholder("Search modules and settings").setText(query);
        content.add(search);
        var summary=factory.label("");content.add(summary);
        StackPanel results=factory.stackPanel().setSpacing(8);
        content.add(factory.scrollPanel().add(results).setAnchor(Anchor.FILL),1);
        var footer=factory.stackPanel(Orientation.HORIZONTAL).setSpacing(8).setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        var previous=factory.button("Previous");var next=factory.button("Next");
        var position=factory.label("");
        footer.add(previous);footer.add(position,1);footer.add(next);content.add(footer);
        Runnable refresh=()->{
            results.clear();
            String text=query.strip().toLowerCase(Locale.ROOT);
            var modules=Modules.get().getAll().stream()
                .filter(m->!Config.get().hiddenModules.get().contains(m))
                .filter(m->category==null || m.category==category)
                .filter(m->!favorites || m.favorite).filter(m->!enabled || m.isActive())
                .filter(m->matches(m,text)).sorted(Comparator.comparing(m->m.title,String.CASE_INSENSITIVE_ORDER)).toList();
            WorkspacePage layout=WorkspacePage.strata(mc.getWindow().getGuiScaledWidth()-160,mc.getWindow().getGuiScaledHeight()-126,modules.size(),page);
            page=layout.page();
            summary.setText((category==null?(favorites?"Favorites":enabled?"Enabled":"All modules"):category.name)+" / "+modules.size()+" modules");
            position.setText("Page "+(page+1)+" / "+layout.pages());
            previous.setEnabled(page>0);next.setEnabled(page+1<layout.pages());
            StackPanel row=null;
            for(int i=layout.from();i<layout.to();i++) {
                if((i-layout.from())%layout.columns()==0) {row=factory.stackPanel(Orientation.HORIZONTAL).setSpacing(8).setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);results.add(row);}
                Module module=modules.get(i);
                var card=factory.stackPanel().setSpacing(4).setPadding(8).setBackground(Paint.NONE);
                card.onEvent(Component.PAINT,event -> panels.paint(event.painter(), event.bounds()));
                var toggle=factory.button((module.isActive()?"On / ":"Off / ")+module.title).setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
                toggle.onEvent(Button.PRESSED,event->{module.toggle();toggle.setText((module.isActive()?"On / ":"Off / ")+module.title);});
                card.add(toggle);
                card.add(factory.button("Configure").setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP).onEvent(Button.PRESSED,event->mc.gui.setScreen(GuiThemes.get().moduleScreen(module))));
                row.add(card,1);
            }
            if(modules.isEmpty())results.add(factory.label("No modules match. Clear search or choose another category."));
        };
        previous.onEvent(Button.PRESSED,event->{page--;refresh.run();});
        next.onEvent(Button.PRESSED,event->{page++;refresh.run();});
        navigation.add(factory.label("LIBRARY"));
        navigation.add(factory.button("All modules").onEvent(Button.PRESSED,event->{category=null;favorites=enabled=false;page=0;refresh.run();}));
        navigation.add(factory.button("Favorites").onEvent(Button.PRESSED,event->{category=null;favorites=true;enabled=false;page=0;refresh.run();}));
        navigation.add(factory.button("Enabled").onEvent(Button.PRESSED,event->{category=null;enabled=true;favorites=false;page=0;refresh.run();}));
        navigation.add(factory.label("CATEGORIES"));
        for(Category item:Modules.loopCategories())navigation.add(factory.button(item.name).onEvent(Button.PRESSED,event->{category=item;enabled=favorites=false;page=0;refresh.run();}));
        navigation.add(factory.button("Classic UI").onEvent(Button.PRESSED,event -> mc.gui.setScreen(GuiThemes.get().modulesScreen())));
        navigation.add(factory.button("Client settings").onEvent(Button.PRESSED,event ->
            com.gaspoweredcake.client33.gui.tabs.Tabs.get(com.gaspoweredcake.client33.gui.tabs.builtin.GuiTab.class).openScreen(GuiThemes.get())));
        search.onEvent(TextInput.EDITED,event->{query=search.text();page=0;refresh.run();});
        refresh.run();
        return factory.anchorPanel().setAnchor(Anchor.FILL).add(root.setAnchor(Anchor.FILL).setMargin(12));
    }
    private static boolean matches(Module module,String text) {
        if((module.title+" "+module.name+" "+module.description).toLowerCase(Locale.ROOT).contains(text))return true;
        for(var group:module.settings)for(var setting:group)
            if(setting.title.toLowerCase(Locale.ROOT).contains(text))return true;
        return false;
    }
}
