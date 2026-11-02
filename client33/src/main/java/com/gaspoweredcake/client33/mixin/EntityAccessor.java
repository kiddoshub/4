
package com.gaspoweredcake.client33.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("wasTouchingWater")
    void client33$setInWater(boolean touchingWater);

    @Accessor("id")
    int client33$getId();

    @Accessor("id")
    void client33$setId(int id);
}
