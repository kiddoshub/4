
package com.gaspoweredcake.client33.systems.modules.movement;

import com.gaspoweredcake.client33.events.client33.KeyInputEvent;
import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.settings.BoolSetting;
import com.gaspoweredcake.client33.settings.Setting;
import com.gaspoweredcake.client33.settings.SettingGroup;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.render.Freecam;
import com.gaspoweredcake.client33.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;

public class AirJump extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> maintainLevel = sgGeneral.add(new BoolSetting.Builder()
        .name("maintain-level")
        .description("Maintains your current Y level when holding the jump key.")
        .defaultValue(false)
        .build()
    );

    private int level;

    public AirJump() {
        super(Categories.Movement, "air-jump", "Lets you jump in the air.");
    }

    @Override
    public void onActivate() {
        level = mc.player.blockPosition().getY();
    }

    @EventHandler
    private void onKey(KeyInputEvent event) {
        if (Modules.get().isActive(Freecam.class) || mc.gui.screen() != null || mc.player.onGround()) return;

        if (event.action != KeyAction.Press) return;

        if (mc.options.keyJump.matches(event.input)) {
            level = mc.player.blockPosition().getY();
            mc.player.jumpFromGround();
        } else if (mc.options.keyShift.matches(event.input)) {
            level--;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (Modules.get().isActive(Freecam.class) || mc.player.onGround()) return;

        if (maintainLevel.get() && mc.player.blockPosition().getY() == level && mc.options.keyJump.isDown()) {
            mc.player.jumpFromGround();
        }
    }
}
