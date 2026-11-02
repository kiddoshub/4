
package com.gaspoweredcake.client33.systems.modules.movement;

import com.gaspoweredcake.client33.events.world.TickEvent;
import com.gaspoweredcake.client33.mixininterface.IVec3;
import com.gaspoweredcake.client33.settings.DoubleSetting;
import com.gaspoweredcake.client33.settings.EnumSetting;
import com.gaspoweredcake.client33.settings.Setting;
import com.gaspoweredcake.client33.settings.SettingGroup;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class AutoJump extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The method of jumping.")
        .defaultValue(Mode.Jump)
        .build()
    );

    private final Setting<JumpWhen> jumpIf = sgGeneral.add(new EnumSetting.Builder<JumpWhen>()
        .name("jump-if")
        .description("Jump if.")
        .defaultValue(JumpWhen.Always)
        .build()
    );

    private final Setting<Double> velocityHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("velocity-height")
        .description("The distance that velocity mode moves you.")
        .defaultValue(0.25)
        .min(0)
        .sliderMax(2)
        .build()
    );

    public AutoJump() {
        super(Categories.Movement, "auto-jump", "Automatically jumps.");
    }

    private boolean jump() {
        return switch (jumpIf.get()) {
            case Sprinting -> mc.player.isSprinting() && (mc.player.zza != 0 || mc.player.xxa != 0);
            case Walking -> mc.player.zza != 0 || mc.player.xxa != 0;
            case Always -> true;
        };
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!mc.player.onGround() || mc.player.isShiftKeyDown() || !jump()) return;

        if (mode.get() == Mode.Jump) mc.player.jumpFromGround();
        else ((IVec3) mc.player.getDeltaMovement()).client33$setY(velocityHeight.get());
    }

    public enum JumpWhen {
        Sprinting,
        Walking,
        Always
    }

    public enum Mode {
        Jump,
        LowHop
    }
}
