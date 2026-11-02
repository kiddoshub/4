
package com.gaspoweredcake.client33.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.misc.AntiPacketKick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * @see net.minecraft.network.PacketBundlePacker#decode(io.netty.channel.ChannelHandlerContext, net.minecraft.network.protocol.Packet, java.util.List)
 */
@Mixin(targets = "net/minecraft/network/protocol/BundlerInfo$1$1")
public abstract class PacketBundlingMixin {
    @ModifyExpressionValue(method = "addPacket", at = @At(value = "CONSTANT", args = "intValue=4096"))
    private int add(int value) {
        if (Modules.get().get(AntiPacketKick.class).isActive()) return Integer.MAX_VALUE;
        return value;
    }
}
