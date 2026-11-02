
package com.gaspoweredcake.client33.gui.themes.client33.widgets.pressable;

import com.gaspoweredcake.client33.gui.themes.client33.Client33Widget;
import com.gaspoweredcake.client33.gui.widgets.pressable.WFavorite;
import com.gaspoweredcake.client33.utils.render.color.Color;

public class WClient33Favorite extends WFavorite implements Client33Widget {
    public WClient33Favorite(boolean checked) {
        super(checked);
    }

    @Override
    protected Color getColor() {
        return theme().favoriteColor.get();
    }
}
