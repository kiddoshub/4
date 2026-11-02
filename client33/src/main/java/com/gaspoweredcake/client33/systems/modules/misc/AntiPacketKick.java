
package com.gaspoweredcake.client33.systems.modules.misc;

import com.gaspoweredcake.client33.settings.BoolSetting;
import com.gaspoweredcake.client33.settings.Setting;
import com.gaspoweredcake.client33.settings.SettingGroup;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;

public class AntiPacketKick extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> catchExceptions = sgGeneral.add(new BoolSetting.Builder()
        .name("catch-exceptions")
        .description("Drops corrupted packets.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> logExceptions = sgGeneral.add(new BoolSetting.Builder()
        .name("log-exceptions")
        .description("Logs caught exceptions.")
        .defaultValue(true)
        .visible(catchExceptions::get)
        .build()
    );

    public AntiPacketKick() {
        super(Categories.Misc, "anti-packet-kick", "Attempts to prevent you from being disconnected by large packets.");
    }

    public boolean catchExceptions() {
        return isActive() && catchExceptions.get();
    }
}
