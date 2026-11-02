
package com.gaspoweredcake.client33.systems.modules.movement;

import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.settings.EnumSetting;
import com.gaspoweredcake.client33.settings.Setting;
import com.gaspoweredcake.client33.settings.SettingGroup;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;

public class AntiVoid extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The method to prevent you from falling into the void.")
        .defaultValue(Mode.Jump)
        .onChanged(value -> { if (value != Mode.Flight) releaseFlight(); })
        .build()
    );

    private boolean ownsFlight;

    public AntiVoid() {
        super(Categories.Movement, "anti-void", "Attempts to prevent you from falling into the void.");
    }

    @Override
    public void onDeactivate() {
        releaseFlight();
    }

    private void releaseFlight() {
        if (!ownsFlight) return;
        ownsFlight = false;
        Modules.get().get(Flight.class).disable();
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        int minY = mc.level.getMinY();
        if (mc.player.getY() > minY || mc.player.getY() < minY - 15) {
            releaseFlight();
            return;
        }

        switch (mode.get()) {
            case Flight -> {
                Flight flight = Modules.get().get(Flight.class);
                if (!flight.isActive()) {
                    flight.enable();
                    ownsFlight = flight.isActive();
                }
            }
            case Jump -> mc.player.jumpFromGround();
        }
    }

    public enum Mode {
        Flight,
        Jump
    }
}
