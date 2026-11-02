
package com.gaspoweredcake.client33.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.gaspoweredcake.client33.systems.modules.Modules;
import com.gaspoweredcake.client33.systems.modules.movement.EntityControl;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Mob.class)
public abstract class MobMixin {
    @ModifyReturnValue(method = "isSaddled", at = @At("RETURN"))
    private boolean isSaddled(boolean original) {
        return Modules.get().get(EntityControl.class).spoofSaddle() || original;
    }
}
