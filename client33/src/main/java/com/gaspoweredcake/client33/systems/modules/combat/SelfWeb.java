
package com.gaspoweredcake.client33.systems.modules.combat;

import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.settings.*;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.utils.entity.SortPriority;
import com.gaspoweredcake.client33.utils.entity.TargetUtils;
import com.gaspoweredcake.client33.utils.player.FindItemResult;
import com.gaspoweredcake.client33.utils.player.InvUtils;
import com.gaspoweredcake.client33.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.Items;

public class SelfWeb extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The mode to use for selfweb.")
        .defaultValue(Mode.Normal)
        .build()
    );

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("range")
        .description("How far away the player has to be from you to place webs. Requires Mode to Smart.")
        .defaultValue(3)
        .min(1)
        .sliderRange(1, 7)
        .visible(() -> mode.get() == Mode.Smart)
        .build()
    );

    private final Setting<Boolean> doubles = sgGeneral.add(new BoolSetting.Builder()
        .name("double-place")
        .description("Places webs in your upper hitbox as well.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> turnOff = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-toggle")
        .description("Toggles off after placing the webs.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Forces you to rotate downwards when placing webs.")
        .defaultValue(true)
        .build()
    );

    public SelfWeb() {
        super(Categories.Combat, "self-web", "Automatically places webs on you.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        switch (mode.get()) {
            case Normal -> placeWeb();
            case Smart -> {
                if (TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestDistance) != null) placeWeb();
            }
        }
    }

    private void placeWeb() {
        FindItemResult web = InvUtils.findInHotbar(Items.COBWEB);

        BlockUtils.place(mc.player.blockPosition(), web, rotate.get(), 0, false);

        if (doubles.get()) {
            BlockUtils.place(mc.player.blockPosition().offset(0, 1, 0), web, rotate.get(), 0, false);
        }

        if (turnOff.get()) toggle();
    }

    public enum Mode {
        Normal,
        Smart
    }
}
