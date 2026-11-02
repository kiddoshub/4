
package com.gaspoweredcake.client33.mixin.lithium;

import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.world.Collisions;
import net.caffeinemc.mods.lithium.common.entity.LithiumEntityCollisions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.border.WorldBorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LithiumEntityCollisions.class)
public abstract class LithiumEntityCollisionsMixin {
    @Inject(method = "isWithinWorldBorder", at = @At("HEAD"), cancellable = true)
    private static void onIsWithinWorldBorder(WorldBorder border, AABB box, CallbackInfoReturnable<Boolean> cir) {
        if (Modules.get().get(Collisions.class).ignoreBorder()) {
            cir.setReturnValue(true);
        }
    }
}
