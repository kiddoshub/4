
package com.gaspoweredcake.client33.systems.modules.render;

import com.gaspoweredcake.client33.events.game.ChangePerspectiveEvent;
import com.gaspoweredcake.client33.events.client33.MouseScrollEvent;
import com.gaspoweredcake.client33.settings.*;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import com.gaspoweredcake.client33.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.CameraType;
import com.mojang.blaze3d.platform.InputConstants;

public class CameraTweaks extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScrolling = settings.createGroup("Scrolling");

    // General

    private final Setting<Boolean> clip = sgGeneral.add(new BoolSetting.Builder()
        .name("clip")
        .description("Allows the camera to clip through blocks.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> cameraDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("camera-distance")
        .description("The distance the third person camera is from the player.")
        .defaultValue(4)
        .min(0)
        .onChanged(value -> distance = value)
        .build()
    );

    private final Setting<Boolean> noFront = sgGeneral.add(new BoolSetting.Builder()
        .name("no-front")
        .description("Skip the front-facing camera when changing perspective.")
        .defaultValue(false)
        .build()
    );

    // Scrolling

    private final Setting<Boolean> scrollingEnabled = sgScrolling.add(new BoolSetting.Builder()
        .name("scrolling")
        .description("Allows you to scroll to change camera distance.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Keybind> scrollKeybind = sgScrolling.add(new KeybindSetting.Builder()
        .name("bind")
        .description("Binds camera distance scrolling to a key.")
        .visible(scrollingEnabled::get)
        .defaultValue(Keybind.fromKey(InputConstants.KEY_LALT))
        .build()
    );

    private final Setting<Double> scrollSensitivity = sgScrolling.add(new DoubleSetting.Builder()
        .name("sensitivity")
        .description("Sensitivity of the scroll wheel when changing the cameras distance.")
        .visible(scrollingEnabled::get)
        .defaultValue(1)
        .min(0.01)
        .build()
    );

    public double distance;

    public CameraTweaks() {
        super(Categories.Render, "camera-tweaks", "Allows modification of the third person camera.");
    }

    @Override
    public void onActivate() {
        distance = cameraDistance.get();
        if (noFront.get() && mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT)
            mc.options.setCameraType(CameraType.FIRST_PERSON);
    }

    @EventHandler
    private void onPerspectiveChanged(ChangePerspectiveEvent event) {
        distance = cameraDistance.get();
        if (noFront.get() && event.perspective == CameraType.THIRD_PERSON_FRONT) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            event.cancel();
        }
    }

    @EventHandler
    private void onMouseScroll(MouseScrollEvent event) {
        if (mc.options.getCameraType() == CameraType.FIRST_PERSON || mc.gui.screen() != null || !scrollingEnabled.get() || (scrollKeybind.get().isSet() && !scrollKeybind.get().isPressed()))
            return;

        if (scrollSensitivity.get() > 0) {
            distance = Math.max(0, distance - event.value * 0.25 * (scrollSensitivity.get() * distance));

            event.cancel();
        }
    }

    public boolean clip() {
        return isActive() && clip.get();
    }
}
