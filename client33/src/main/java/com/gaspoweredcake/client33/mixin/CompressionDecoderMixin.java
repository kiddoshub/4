
package com.gaspoweredcake.client33.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.misc.AntiPacketKick;
import net.minecraft.network.CompressionDecoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CompressionDecoder.class)
public abstract class CompressionDecoderMixin {
    @ModifyExpressionValue(method = "decode", at = @At(value = "CONSTANT", args = "intValue=8388608"))
    private int client33$maximizeUncompressedPacketLimit(int original) {
        return Modules.get().isActive(AntiPacketKick.class) ? Integer.MAX_VALUE : original;
    }
}
