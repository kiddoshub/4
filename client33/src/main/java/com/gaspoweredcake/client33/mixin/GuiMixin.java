
package com.gaspoweredcake.client33.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.gaspoweredcake.client33.Client33;
import com.gaspoweredcake.client33.events.game.OpenScreenEvent;
import com.gaspoweredcake.client33.gui.WidgetScreen;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.movement.GUIMove;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void client33$onSetScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof WidgetScreen) {
            screen.mouseMoved(
                minecraft.mouseHandler.getScaledXPos(minecraft.getWindow()),
                minecraft.mouseHandler.getScaledYPos(minecraft.getWindow())
            );
        }

        OpenScreenEvent event = OpenScreenEvent.get(screen);
        Client33.EVENT_BUS.post(event);

        if (event.isCancelled()) ci.cancel();
    }

    @WrapOperation(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;releaseAll()V"))
    private void client33$onSetScreenKeyBindingUnpressAll(Operation<Void> op) {
        Modules modules = Modules.get();
        if (modules == null) {
            op.call();
            return;
        }

        GUIMove guiMove = modules.get(GUIMove.class);
        if (guiMove == null || !guiMove.isActive() || guiMove.skip()) {
            op.call();
            return;
        }

        Options options = minecraft.options;
        for (KeyMapping kb : KeyMappingAccessor.getKeysById().values()) {
            if (kb == options.keyUp) continue;
            if (kb == options.keyLeft) continue;
            if (kb == options.keyRight) continue;
            if (kb == options.keyDown) continue;
            if (guiMove.sneak.get() && kb == options.keyShift) continue;
            if (guiMove.sprint.get() && kb == options.keySprint) continue;
            if (guiMove.jump.get() && kb == options.keyJump) continue;
            ((KeyMappingAccessor) kb).client33$invokeRelease();
        }
    }
}
