
package com.gaspoweredcake.client33.systems.modules.player;

import com.gaspoweredcake.client33.settings.BoolSetting;
import com.gaspoweredcake.client33.settings.EntityTypeListSetting;
import com.gaspoweredcake.client33.settings.Setting;
import com.gaspoweredcake.client33.settings.SettingGroup;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Set;

public class NoMiningTrace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
        .name("blacklisted-entities")
        .description("Entities you will interact with as normal.")
        .defaultValue()
        .build()
    );

    private final Setting<Boolean> onlyWhenHoldingPickaxe = sgGeneral.add(new BoolSetting.Builder()
        .name("only-when-holding-a-pickaxe")
        .description("Whether or not to work only when holding a pickaxe.")
        .defaultValue(true)
        .build()
    );

    public NoMiningTrace() {
        super(Categories.Player, "no-mining-trace", "Allows you to mine blocks through entities.");
    }

    public boolean canWork(Entity entity) {
        if (!isActive()) return false;

        return (!onlyWhenHoldingPickaxe.get() || mc.player.getMainHandItem().is(ItemTags.PICKAXES) || mc.player.getOffhandItem().is(ItemTags.PICKAXES)) &&
            (entity == null || !entities.get().contains(entity.getType()));
    }
}
