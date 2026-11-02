package com.gaspoweredcake.client33.mixin;

import net.minecraft.network.PacketDecoder;
import net.minecraft.network.ProtocolInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PacketDecoder.class)
public interface PacketDecoderAccessor {
    @Accessor("protocolInfo") ProtocolInfo<?> client33$getProtocolInfo();
}
