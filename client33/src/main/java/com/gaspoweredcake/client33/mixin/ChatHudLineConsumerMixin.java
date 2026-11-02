
package com.gaspoweredcake.client33.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.misc.BetterChat;
import com.gaspoweredcake.client33.systems.modules.render.NoRender;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$1", remap = false)
public abstract class ChatHudLineConsumerMixin {
    // Player Heads, also draw immediately when line is set
    @Inject(method = "accept", at = @At("HEAD"))
    private void setLine(GuiMessage.Line line, int lineIndex, float alpha, CallbackInfo ci) {
        Modules.get().get(BetterChat.class).line = line;
    }

    // No Message Signature Indicator
    @ModifyExpressionValue(method = "accept", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/chat/GuiMessage$Line;tag()Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;"))
    private GuiMessageTag onRender_modifyIndicator(GuiMessageTag indicator) {
        return Modules.get().get(NoRender.class).noMessageSignatureIndicator() ? null : indicator;
    }
}
