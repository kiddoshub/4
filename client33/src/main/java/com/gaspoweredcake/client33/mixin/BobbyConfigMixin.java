package com.gaspoweredcake.client33.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.johni0702.minecraft.bobby.BobbyConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Bobby's Sodium overlay uses this effective limit instead of the vanilla option range. */
@Mixin(value = BobbyConfig.class, remap = false)
public abstract class BobbyConfigMixin {
    @ModifyReturnValue(method = "getMaxRenderDistance", at = @At("RETURN"))
    private int client33$extendedSodiumRange(int configured) {
        return Math.max(configured, 300);
    }
}
