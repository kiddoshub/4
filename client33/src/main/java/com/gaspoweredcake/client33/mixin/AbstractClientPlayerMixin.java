
package com.gaspoweredcake.client33.mixin;

import com.gaspoweredcake.client33.utils.misc.FakeClientPlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.gaspoweredcake.client33.Client33.mc;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    // Player model rendering in main menu

    @Inject(method = "getPlayerInfo", at = @At("HEAD"), cancellable = true)
    private void onGetPlayerListEntry(CallbackInfoReturnable<PlayerInfo> cir) {
        if (mc.getConnection() == null) cir.setReturnValue(FakeClientPlayer.getPlayerListEntry());
    }
}
