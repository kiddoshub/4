
package com.gaspoweredcake.client33.utils.misc.text;

import com.gaspoweredcake.client33.mixin.ScreenMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This class does nothing except ensure that {@link ClickEvent}'s containing 33 commands can only be executed if they come from the client.
 *
 * @see ScreenMixin#onDefaultHandleClickEvent(ClickEvent, Minecraft, Screen, CallbackInfo)
 */
public class Client33ClickEvent implements ClickEvent {
    public final String value;

    public Client33ClickEvent(String value) {
        this.value = value;
    }

    @Override
    public @NonNull Action action() {
        return Action.RUN_COMMAND;
    }
}
