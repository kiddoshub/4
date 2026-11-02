
package com.gaspoweredcake.client33.systems.modules.player;

import com.gaspoweredcake.client33.events.entity.player.BlockBreakingCooldownEvent;
import com.gaspoweredcake.client33.events.client33.MouseClickEvent;
import com.gaspoweredcake.client33.settings.BoolSetting;
import com.gaspoweredcake.client33.settings.IntSetting;
import com.gaspoweredcake.client33.settings.Setting;
import com.gaspoweredcake.client33.settings.SettingGroup;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;

public class BreakDelay extends Module {
    SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> cooldown = sgGeneral.add(new IntSetting.Builder()
        .name("cooldown")
        .description("Block break cooldown in ticks.")
        .defaultValue(0)
        .min(0)
        .sliderMax(5)
        .build()
    );

    private final Setting<Boolean> noInstaBreak = sgGeneral.add(new BoolSetting.Builder()
        .name("no-insta-break")
        .description("Prevents you from misbreaking blocks if you can instantly break them.")
        .defaultValue(false)
        .build()
    );

    private boolean breakBlockCooldown = false;

    public BreakDelay() {
        super(Categories.Player, "break-delay", "Changes the delay between breaking blocks.");
    }

    @EventHandler
    private void onBlockBreakingCooldown(BlockBreakingCooldownEvent event) {
        if (breakBlockCooldown) {
            event.cooldown = 5;
            breakBlockCooldown = false;
        } else {
            event.cooldown = cooldown.get();
        }
    }

    @EventHandler
    private void onClick(MouseClickEvent event) {
        if (event.action == KeyAction.Press && noInstaBreak.get()) {
            breakBlockCooldown = true;
        }
    }

    public boolean preventInstaBreak() {
        return isActive() && noInstaBreak.get();
    }
}
