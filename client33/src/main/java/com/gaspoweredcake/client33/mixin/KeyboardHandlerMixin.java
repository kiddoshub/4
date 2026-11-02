
package com.gaspoweredcake.client33.mixin;

import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.events.client33.CharTypedEvent;
import com.gaspoweredcake.client33.events.client33.KeyInputEvent;
import com.gaspoweredcake.client33.gui.GuiKeyEvents;
import com.gaspoweredcake.client33.gui.WidgetScreen;
import com.gaspoweredcake.client33.utils.Utils;
import com.gaspoweredcake.client33.utils.misc.input.Input;
import com.gaspoweredcake.client33.utils.misc.input.KeyAction;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    public void onKey(long handle, int action, KeyEvent event, CallbackInfo ci) {
        int modifiers = event.modifiers();
        if (event.key() != InputConstants.UNKNOWN.getValue()) {
            // SDL supplies the current left/right modifier state with the event.
            if (minecraft.gui.screen() instanceof WidgetScreen widgetScreen && action == InputConstants.REPEAT) {
                widgetScreen.keyRepeated(new KeyEvent(event.key(), event.keycode(), modifiers));
            }

            // Keep releases accurate while widgets suppress shortcut dispatch.
            Input.setKeyState(event.key(), action != InputConstants.RELEASE);
            if (GuiKeyEvents.canUseKeys) {
                if (Client33.EVENT_BUS.post(KeyInputEvent.get(new KeyEvent(event.key(), event.keycode(), modifiers), KeyAction.get(action))).isCancelled())
                    ci.cancel();
            }
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void onChar(long handle, CharacterEvent event, CallbackInfo ci) {
        if (Utils.canUpdate() && !minecraft.isPaused() && (minecraft.gui.screen() == null || minecraft.gui.screen() instanceof WidgetScreen)) {
            if (Client33.EVENT_BUS.post(CharTypedEvent.get((char) event.codepoint())).isCancelled()) ci.cancel();
        }
    }
}
