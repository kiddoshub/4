
package com.gaspoweredcake.client33.mixin;

import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.CrossbowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CrossbowItem.class)
public interface CrossbowItemAccessor {
    @Invoker("getShootingPower")
    static float client33$getSpeed(ChargedProjectiles itemStack) {
        return 0;
    }
}
