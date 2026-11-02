
package com.gaspoweredcake.client33.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker("jumpInLiquid")
    void client33$swimUpwards(TagKey<Fluid> fluid);

    @Accessor("jumping")
    boolean client33$isJumping();

    @Accessor("noJumpDelay")
    int client33$getJumpCooldown();

    @Accessor("noJumpDelay")
    void client33$setJumpCooldown(int cooldown);
}
