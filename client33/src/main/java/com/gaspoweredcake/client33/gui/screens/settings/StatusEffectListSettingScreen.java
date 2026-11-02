
package com.gaspoweredcake.client33.gui.screens.settings;

import com.gaspoweredcake.client33.gui.GuiTheme;
import com.gaspoweredcake.client33.gui.screens.settings.base.CollectionListSettingScreen;
import com.gaspoweredcake.client33.gui.widgets.WWidget;
import com.gaspoweredcake.client33.settings.Setting;
import com.gaspoweredcake.client33.utils.misc.Names;
import com.gaspoweredcake.client33.utils.render.DisplayItemUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.List;
import java.util.Optional;

public class StatusEffectListSettingScreen extends CollectionListSettingScreen<MobEffect> {
    public StatusEffectListSettingScreen(GuiTheme theme, Setting<List<MobEffect>> setting) {
        super(theme, "Select Effects", setting, setting.get(), BuiltInRegistries.MOB_EFFECT);
    }

    @Override
    protected WWidget getValueWidget(MobEffect value) {
        return theme.itemWithLabel(getPotionStack(value), Names.get(value));
    }

    @Override
    protected String[] getValueNames(MobEffect value) {
        return new String[]{
            Names.get(value),
            BuiltInRegistries.MOB_EFFECT.getKey(value).toString()
        };
    }

    private ItemStack getPotionStack(MobEffect effect) {
        ItemStack potion = DisplayItemUtils.toStack(Items.POTION);

        potion.set(
            DataComponents.POTION_CONTENTS,
            new PotionContents(
                Optional.empty(),
                Optional.of(effect.getColor()),
                List.of(),
                Optional.empty()
            )
        );

        return potion;
    }
}
