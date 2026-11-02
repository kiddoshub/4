
package com.gaspoweredcake.client33.gui.screens.settings;

import com.gaspoweredcake.client33.gui.GuiTheme;
import com.gaspoweredcake.client33.gui.screens.settings.base.CollectionListSettingScreen;
import com.gaspoweredcake.client33.gui.widgets.WWidget;
import com.gaspoweredcake.client33.settings.BlockListSetting;
import com.gaspoweredcake.client33.utils.misc.Names;
import com.gaspoweredcake.client33.utils.render.DisplayItemUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.function.Predicate;

public class BlockListSettingScreen extends CollectionListSettingScreen<Block> {
    public BlockListSettingScreen(GuiTheme theme, BlockListSetting setting) {
        super(theme, "Select Blocks", setting, setting.get(), BuiltInRegistries.BLOCK);
    }

    @Override
    protected boolean includeValue(Block value) {
        if (BuiltInRegistries.BLOCK.getKey(value).getPath().endsWith("_wall_banner")) {
            return false;
        }

        Predicate<Block> filter = ((BlockListSetting) setting).filter;

        if (filter == null) return value != Blocks.AIR;
        return filter.test(value);
    }

    @Override
    protected WWidget getValueWidget(Block value) {
        return theme.itemWithLabel(DisplayItemUtils.toStack(value), Names.get(value));
    }

    @Override
    protected String[] getValueNames(Block value) {
        return new String[]{
            Names.get(value),
            BuiltInRegistries.BLOCK.getKey(value).toString()
        };
    }

    @Override
    protected Block getAdditionalValue(Block value) {
        String path = BuiltInRegistries.BLOCK.getKey(value).getPath();
        if (!path.endsWith("_banner")) return null;

        return BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(path.substring(0, path.length() - 6) + "wall_banner"));
    }
}
