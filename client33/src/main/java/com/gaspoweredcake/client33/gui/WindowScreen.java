
package com.gaspoweredcake.client33.gui;

import com.gaspoweredcake.client33.gui.utils.Cell;
import com.gaspoweredcake.client33.gui.widgets.WWidget;
import com.gaspoweredcake.client33.gui.widgets.containers.WWindow;

public abstract class WindowScreen extends WidgetScreen {
    protected final WWindow window;
    protected final Cell<WWindow> windowCell;

    public WindowScreen(GuiTheme theme, WWidget icon, String title) {
        super(theme, title);

        windowCell = super.add(theme.window(icon, title)).center();
        window = windowCell.widget();
        window.view.scrollOnlyWhenMouseOver = false;
    }

    public WindowScreen(GuiTheme theme, String title) {
        this(theme, null, title);
    }

    @Override
    public <W extends WWidget> Cell<W> add(W widget) {
        return window.add(widget);
    }

    @Override
    public void clear() {
        window.clear();
    }
}
