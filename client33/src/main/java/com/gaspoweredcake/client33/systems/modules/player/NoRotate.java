
package com.gaspoweredcake.client33.systems.modules.player;

import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.gaspoweredcake.client33.mixin.ClientPacketListenerMixin;
import com.gaspoweredcake.client33.systems.modules.Categories;
import com.gaspoweredcake.client33.systems.modules.Module;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @see ClientPacketListenerMixin#onHandleMovePlayerHead(ClientboundPlayerPositionPacket, CallbackInfo, LocalFloatRef, LocalFloatRef)
 */
public class NoRotate extends Module {
    public NoRotate() {
        super(Categories.Player, "no-rotate", "Attempts to block rotations sent from server to client.");
    }
}
